/*
	MandelHub
	(c) 2010-11 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Performance system used by Benoit and the Mandelbrots
	
*/


MandelHub {
	
	/*
		Each group needs a leader. At the moment nobody claims this
		position for itself, but a System without a leader throws a
		warning.
	*/
	
	classvar <instance;
	classvar bStrapResponder;
	
	classvar <>debug = false;
	
	var <>tapInstance;
	
	var <clock;
	
	var <>externalTempo; // the tempo set by external clock
	var <>tempo; // the internal tempo, may differ because it does a correction
	
	var lastTickSJ;
	
	var tickSJ;
	var tempoChangeSJ;
					
	var <leading = false;
	var <leaderName;
		
	// an String ID
	var <name;
		
	var <>tickFreq = 0.05; // in beats
	var <>quant = 16; // may be nil, in beats
		
	var <>allowTempoRequests = true;
	
	var <>maxTempo = 4.0;
	var <>minTempo = 0.2;
	
	// TODO: really use this!
	var beatsPerBar = 4;
			
	var <guiInstance;
			
	var <>postPrefix = "MandelHub: ";
					
	var tempoBusInstance, tempoBusDependant;
	
	// Modules
	var modules;

	var <net;
	var <space;
	var <tools;
	var <platform;
	var <time;
	var <timer;
	
	var <>server;
	
	// Startup procedure is a mess. ToFix!
	
	*startGeneral {|timeClass, server|
		NetAddr.broadcastFlag_(true);
		
		timeClass = timeClass ? MandelTimeDriver;
		server = server ? Server.default;
		
		instance.notNil.if {
			"THERE IS ALREADY A MANDELHUB INSTANCE".postln;
			^instance;	
		};
		^[timeClass, server];
	}
			
	*start {|name, startTempo = 2.0, timeClass, server|
		var general = MandelHub.startGeneral(timeClass, server);
		(general === instance).if {^instance};
		general.isNil.if {"COULD NOT START A LEADER MANDELHUB".postln; ^nil;};
		timeClass = general[0];
		server = general[1];
		
		(NetAddr.langPort != 57120).if {
			"*** Warning! ***".postln;
			("Your sclang port is not 57120, it's " ++ NetAddr.langPort ++ "!").postln;
			"This isn't a problem, but followers must use this port as".postln;
			"second argument in the join call.".postln;
			"".postln; 	
		};
		
		instance = MandelHub.new(name, 0, startTempo, name, [NetAddr.langPort], leading:true, timeClass:timeClass, server:server);
		^instance;
	}
	
	*join {|name, port=57120, action, timeClass, server|
		
		var addr;
		var followSkipJack;
		var general = MandelHub.startGeneral(timeClass, server);
		general.isNil.if {"COULD NOT START A FOLLOWER MANDELOCK".postln; ^nil;};
		timeClass = general[0];
		server = general[1];
		
		addr = NetAddr("255.255.255.255", port);
		
		followSkipJack = SkipJack({
			addr.sendMsg(MandelNetwork.oscPrefix ++ "/requestPort", name, -1, NetAddr.langPort);
		}, 1);
		
		"Waiting for a signal from the Leader ...".postln;
		
		// ATTENTION: THIS NEEDS MANUAL UPDATE IF NETWORK CODE CHANGES
		bStrapResponder = OSCresponder(nil, MandelNetwork.oscPrefix ++ "/clock", {|ti, tR, message, addr|
			bStrapResponder.remove;
			followSkipJack.stop;
			
			instance = MandelHub.new(name, message[4], message[5], message[1].asString,[port], false, timeClass:timeClass, server:server);
			("... you are now following " ++ message[1].asString ++ "!").postln;
			
			action.value(instance);			
		}).add;	
	}
	
	*new {|name, startBeat, startTempo, leaderName, ports, leading=false, timeClass, server|
		
		name.isNil.if {
			name = "RandomUser" ++ 100000.rand;	
		};
		
		^super.new.init(name, startBeat, startTempo, leaderName, ports, leading, timeClass, server);
	}
	
	init {|a_name, startBeat, startTempo, a_leaderName, ports, a_leading, timeClass, a_server|
		
		name = a_name;
		leaderName = a_leaderName;
		leading = a_leading;
		server = a_server;
		
		// start the clock
		clock = MandelClock.new(startTempo, startBeat).hub_(this);
		clock.permanent_(true);
		
		TempoClock.default = clock;		
		externalTempo = startTempo;
		tempo = startTempo;
		
		// init Modules
		this.prInitModules(ports);
		
		time = timeClass.new(this);
		modules.add(time);
		
		// build responders
		this.prGeneralResponders;
				
		// start your career
		leading.if ({
			this.prBecomeLeader;
		},{
			this.prBecomeFollower;	
			{this.sendRequestSync}.defer(1);
			{this.sendHello}.defer(1.5);
		});
		
		this.prDoCmdPeriod;
	}
	
	prInitModules {|ports|
		modules = List.new;
		
		net = MandelNetwork(this, ports);
		modules.add(net);
		space = MandelSpace(this);
		modules.add(space);
		tools = MandelTools(this);
		modules.add(tools);
		timer = MandelTimer(this);
		modules.add(timer);
		
		// platform specific
		Platform.case(
			\osx, {
				platform = MandelPlatformOSX(this);
			},
			\linux, {
				platform = MandelPlatformLinux(this);
			},
			{ // default
				this.post("Platform specific functions for your system aren't available.");
				platform = MandelPlatform(this);
			}
		);
		modules.add(platform);
		
		modules.do {|module| module.onStartup(this) };	
	}
	
	sendRequestSync {
		this.net.sendMsgBurst("/requestSync", \critical);
	}
	
	sendHello {
		this.net.sendMsgBurst("/hello", \relaxed);	
	}
	
	sendRequestHello {
		this.net.sendMsgBurst("/requestHello", \relaxed);
	}
	
	takeLead {
		this.net.sendMsgBurst("/takeLead", \crititcal);
		
		leading.not.if {
			this.prBecomeLeader;
		};
	}
	
	chat {|message|
		this.net.sendMsgBurst("/chat", \important, message);
	}
	
	shout {|message|
		this.net.sendMsgBurst("/shout", \important, message);
	}
	
	changeTempo {|newTempo, dur=0|
		
		var delta, stopTest;
		
		newTempo = this.prSafeTempo(newTempo);
		
		leading.if ({
			tempoChangeSJ.stop;
			
			((dur <= 0) || (newTempo == tempo)).if ({
				this.prSetClockTempo(newTempo);
				time.tick;
			},{
				delta = (newTempo - tempo) * 0.1 / dur;
				
				(delta < 0.0).if ({
					stopTest = {((tempo + delta) <= newTempo).if({this.prSetClockTempo(newTempo);true;},{false;});};
				},{
					stopTest = {((tempo + delta) >= newTempo).if({this.prSetClockTempo(newTempo);true;},{false;});};				});
				
				tempoChangeSJ = SkipJack({
					// TO IMPROVE: Smoother curve, linear kinda sux
					this.prSetClockTempo(tempo + delta);
				},0.1, stopTest, name: "TempoChange");
			});
		}, {
			this.post("Requested new tempo from the Leader");
			this.net.sendMsgBurst("/requestTempo", \important, newTempo.asFloat, dur.asFloat);
		});
	}
	
	prSafeTempo {|newTempo|
		
		(newTempo < minTempo).if {
			this.post("Tempo out of range. Set tempo to minTempo=" ++ minTempo);
			newTempo = minTempo;	
		};
		
		(newTempo > maxTempo).if {
			this.post("Tempo out of range. Set tempo to maxTempo=" ++ maxTempo);
			newTempo = maxTempo;
		};
		
		^newTempo;
	}
	
	prBecomeLeader {
		
		leaderName = name;
		leading = true;
		
		// clear follower responders
		this.net.clearResponders(\follower);
		
		// clear follower tasks
		lastTickSJ.stop;
		lastTickSJ = nil;
				
		this.post("Starting leader tasks ...");
		
		// start leader tasks
		tickSJ = SkipJack({
			time.tick;
		}, tickFreq, name: "ClockTick");
		
		// start leader responders
		this.prLeaderResponders;
		
		modules.do {|module| module.onBecomeLeader(this) };
		
		this.post("You are now the leader!");
	}
	
	prBecomeFollower {
		
		leading = false;
		
		// clear leader responders
		this.net.clearResponders(\leader);

		// clear leader tasks		
		tickSJ.stop;
		tickSJ = nil;
		tempoChangeSJ.stop;
		tempoChangeSJ = nil;
		
		this.post("Starting follower tasks ...");
		
		// start follower tasks

		lastTickSJ = SkipJack({
			((time.lastTickTime + 10) < thisThread.seconds).if {
				this.post("WARNING, did not receive clock signals from the leader!");
				this.post("Someone else should take the lead ...");
				
				this.prSetClockTempo(externalTempo);
				
			};
		},5, name: "LeaderCheck");
		
		// start follower responders
		this.prFollowerResponders;	
		
		modules.do {|module| module.onBecomeFollower(this) };
	}
	
	
	prSetClockTempo {|newTempo|
		
		(newTempo < (minTempo / 4)).if {
			newTempo = minTempo / 4;
		};
		
		tempo = newTempo;
		clock.commitTempo(newTempo);
		this.changed(\tempo, newTempo);
		leading.if {externalTempo = newTempo;};
	}
	
	// responders only for followers
	prFollowerResponders {
		this.net.addOSCResponder(\follower, "/clock", {|header, payload|
			time.receiveTick(*payload);
		}, \leaderOnly);
	}
	
	// responders only for leaders
	prLeaderResponders {
		this.net.addOSCResponder(\leader, "/requestTempo", {|header, payload|
			this.post(header.name ++ " requested a tempo change to " ++ payload[0].asFloat ++ " BPS");
			
			allowTempoRequests.if ({
				this.changeTempo(payload[0].asFloat, payload[1].asFloat);
				this.post("Tempchange granted!");
			}, {
				this.post("Tempochange denied.");
			});
		});
		
		this.net.addOSCResponder(\leader, "/requestSync", {|header, payload|
			modules.do {|module| module.onSyncRequest(this, header);};
		}, \dropOwn);
	}
	
	// responders for leaders and followers
	prGeneralResponders {
		
		this.net.addOSCResponder(\general, "/chat", {|header, payload|
			 (header.name ++ ":  " ++ payload[0].asString).postln;
		});
		
		this.net.addOSCResponder(\general, "/shout", {|header, payload|
			 (header.name ++ " (shout):  " ++ payload[0].asString).postln;
			 this.displayShout(header.name, payload[0].asString);
		});
		
		this.net.addOSCResponder(\general, "/hello", {|header, payload|
			(leaderName == header.name).if({
				(header.name ++ " is the leader.").postln;
			},{
				(header.name ++ " is following the leader.").postln;
			});
		});
		
		this.net.addOSCResponder(\general, "/requestHello", {|header, payload|
			this.sendHello;
		});
		
		
		this.net.addOSCResponder(\general, "/takeLead", {|header, payload|
				this.prReceivedLeaderAnnouncement(header.name);
		});
	}
	
	clear {
		this.net.clearAllResponders;
		
		lastTickSJ.stop;
		tickSJ.stop;
		tempoChangeSJ.stop;
		
		modules.do {|module| module.onClear(this)};
		
		instance = nil;		
	}
	
	*clear {
		instance.notNil.if {
			instance.clear;
		};
		
		// stops looking for a leader
		bStrapResponder.remove;	
	}
	
	prReceivedLeaderAnnouncement {|newLeader|
		
		(newLeader != name).if {
			
			(postPrefix + newLeader + " is our new leader!").postln;
			leaderName = newLeader;
			
			leading.if {
				this.prBecomeFollower;
				this.post("You are not the leader anymore :-(");
			};
		};	
	} 
	
	prShouldFollow {|message|
		^((message[1].asString == leaderName) && leading.not);	
	}
	
	gui {|pos|
		guiInstance.notNil.if {
			this.closeGUI;
		};
		
		guiInstance = MandelGUI(this, pos);
		^guiInstance;
	}
	
	tap {
		tapInstance.isNil.if {
			^tapInstance = MandelTap.new(this);
		};
	}
	
	closeGUI {
		guiInstance.notNil.if {
			guiInstance.close;
		}	
	}
	
	post {|message|
		(postPrefix ++ message).postln;	
	}
	
	classPath {|filename|
		^MandelHub.filenameSymbol.asString.dirname ++ "/" ++ filename;
	}
	
	displayShout {|name, message|
		// stupid sanity replacement, should be replaced by better escaping (or stuff)
		name = name.tr($', $ );
		message = message.tr($', $ );
			
		platform.displayNotification(name, message);
	}
	
	prSendWindow {|title, func|
		StringInputDialog.new(title, "Send", {|string| 
			func.value(string);
			platform.focusCurrentDocument;
		});		
	}
	
	chatWindow {
		this.prSendWindow("MandelHub Chat",  {|string| this.chat(string);});
	}
	
	shoutWindow {
		this.prSendWindow("MandelHub Shout", {|string| this.shout(string);});
	}
	
	prDoCmdPeriod {		
		modules.do {|mod| mod.registerCmdPeriod(this);};
		CmdPeriod.doOnce({this.prDoCmdPeriod});	
	}
	
	tempoBus {
		tempoBusInstance.isNil.if {
			server.serverRunning.if ({
				tempoBusInstance = Bus.control(server, 1);
				tempoBusInstance.set(tempo);
			
				tempoBusDependant = {|changed, what, value| (what == \tempo).if {tempoBusInstance.set(value)};};
				this.addDependant(tempoBusDependant);
			}, {
				"Server is not running! You have to re-evaluate tempoBus.".warn;
				// dummy Bus to fail silently
				^Bus.control(server, 1);
			});
		};
		^tempoBusInstance;
	}
	
	
	// depr.
	
	metro {|pan=0.0, quant=4|
		"metro is going to be removed from MandelHub instance. Use m.tools.metro instead".postln;
		^tools.metro(pan, quant);
	}
	
		
	requestTempo {|newTempo, dur=0|
		"requestTempo is deprecated. Use changeTempo instead, even if you're not the leader".postln;
		^this.changeTempo(newTempo, dur);
	}	
}