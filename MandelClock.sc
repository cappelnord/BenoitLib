/*
	MandelClock
	(c) 2010-11 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Synchronization System used by Benoit and the Mandelbrots
	
	This class is way to big. I'll refactor it later into a more
	modular approach.
	
*/


MandelClock {
	
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
	
	var <>helloAfterPorts = false;
	
	var <>tickFreq = 0.02; // in s
	var <>quant = 16; // may be nil, in beats
		
	var <>allowTempoRequests = true;
	
	var <>maxTempo = 4.0;
	var <>minTempo = 0.2;
	
	// TODO: really use this!
	var beatsPerBar = 4;
			
	var <guiInstance;
			
	var <>postPrefix = "MandelClock: ";
		
	var metro;
			
	var tempoBusInstance, tempoBusDependant;
	
	// Modules
	var modules;

	var <net;
	var <space;
	var <tools;
	var <platform;
	var <time;
	
	var <>server;
	
	// Startup procedure is a mess. ToFix!
	
	*startGeneral {|timeClass, server|
		NetAddr.broadcastFlag_(true);
		
		timeClass = timeClass ? MandelTimeDriver;
		server = server ? Server.default;
		
		instance.notNil.if {
			"THERE IS ALREADY A MANDELCLOCK INSTANCE".postln;
			^nil;	
		};
	}
			
	*startLeader {|name, startTempo = 2.0, timeClass, server|
		var general = MandelClock.startGeneral(timeClass, server);
		general.isNil.if {"COULD NOT START A LEADER MANDELOCK".postln; ^nil;}
		timeClass = general[0];
		server = general[1];
		
		(NetAddr.langPort != 57120).if {
			"*** Warning! ***".postln;
			("Your sclang port is not 57120, it's " ++ NetAddr.langPort ++ "!").postln;
			"This isn't a problem, but followers must use this port as".postln;
			"second argument in the startFollower call.".postln;
			"".postln; 	
		};
		
		instance = MandelClock.new(name, 0, startTempo, name, [NetAddr.langPort], leading:true, timeClass:timeClass, server:server);
		^instance;
	}
	
	*startFollower {|name, port=57120, action, timeClass, server|
		
		var addr;
		var general = MandelClock.startGeneral(timeClass, server);
		general.isNil.if {"COULD NOT START A FOLLOWER MANDELOCK".postln; ^nil;}
		timeClass = general[0];
		server = general[1];
		
		addr = NetAddr("255.255.255.255", port);
		addr.sendMsg(MandelNetwork.oscPrefix ++ "/requestPort", name, NetAddr.langPort);
		
		"Waiting for a signal from the Leader ...".postln;
		
		// ATTENTION: THIS NEEDS MANUAL UPDATE IF NETWORK CODE CHANGES
		bStrapResponder = OSCresponder(nil, oscPrefix ++ "/clock", {|ti, tR, message, addr|
			bStrapResponder.remove;
			
			instance = MandelClock.new(name, message[3], message[4], message[1].asString,[port], false, timeClass:timeClass, server:server);
			instance.helloAfterPorts = true;
			instance.publishPorts;
			
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
		clock = TempoClock.new(startTempo, startBeat, queueSize:4096);
		clock.permanent_(true);
		
		TempoClock.default = clock;		
		externalTempo = startTempo;
		tempo = startTempo;
		
		// init Modules
		this.pr_initModules(ports);
		
		time = timeClass.new(this);
		modules.add(time);
		
		// build responders
		this.pr_generalResponders;
				
		// start your career
		leading.if ({
			this.pr_becomeLeader;
		},{
			"Follower".postln;
			this.pr_becomeFollower;	
		});
		
		this.pr_doCmdPeriod;
	}
	
	pr_initModules {|ports|
		modules = List.new;
		
		net = MandelNetwork(this, ports);
		modules.add(net);
		space = MandelSpace(this);
		modules.add(space);
		tools = MandelTools(this);
		modules.add(tools);
		
		// platform specific
		Platform.case(
			\osx, {
				platform = MandelPlatformOSX(this);
			},
			\linux, {
				platform = MandelPlatformLinux(this);
			}
		);
		modules.add(platform);
		
		modules.do {|module| module.onStartup(this) };	
	}
	
	sendHello {
		this.net.sendMsgCmd("/hello");	
	}
	
	sendRequestHello {
		this.net.sendMsgCmd("/requestHello");	
	}
	
	sendPongPort {
		this.net.sendMsgCmd("/pongPort", NetAddr.langPort);
	}
	
	takeLead {
		this.net.sendMsgCmd("/takeLead");
		
		leading.not.if {
			this.pr_becomeLeader;
		};
	}
	
	chat {|message|
		this.net.sendMsgCmd("/chat", message);
	}
	
	shout {|message|
		this.net.sendMsgCmd("/shout", message);
	}
	
	publishPorts {
		this.net.sendMsgCmd("/publishPorts");	
	}
	
	// of course it could be the same method for a leader and a follower
	// but I think normally only a leader should change the tempo, so making
	// this cut enforces this somehow.
	requestTempo {|newTempo, dur=0|
		leading.not.if {
			newTempo = this.pr_safeTempo(newTempo);
			this.net.sendMsgCmd("/requestTempo", newTempo.asFloat, dur.asFloat);
		};
	}
	
	changeTempo {|newTempo, dur=0|
		
		var delta, stopTest;
		
		newTempo = this.pr_safeTempo(newTempo);
		
		leading.if {
			tempoChangeSJ.stop;
			
			((dur <= 0) || (newTempo == tempo)).if ({
				this.pr_setClockTempo(newTempo);
				time.tick;
			},{
				delta = (newTempo - tempo) * 0.1 / dur;
				
				(delta < 0.0).if ({
					stopTest = {((tempo + delta) <= newTempo).if({this.pr_setClockTempo(newTempo);true;},{false;});};
				},{
					stopTest = {((tempo + delta) >= newTempo).if({this.pr_setClockTempo(newTempo);true;},{false;});};				});
				
				tempoChangeSJ = SkipJack({
					// TO IMPROVE: Smoother curve, linear kinda sux
					this.pr_setClockTempo(tempo + delta);
				},0.1, stopTest, name: "TempoChange");
			});
		};	
	}
	
	pr_safeTempo {|newTempo|
		
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
	
	pr_becomeLeader {
		
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
		this.pr_leaderResponders;
		
		modules.do {|module| module.onBecomeLeader(this) };
		
		this.post("You are now the leader!");
	}
	
	pr_becomeFollower {
		
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
				
				this.pr_setClockTempo(externalTempo);
				
			};
		},5, name: "LeaderCheck");
		
		// start follower responders
		this.pr_followerResponders;	
		
		modules.do {|module| module.onBecomeFollower(this) };
	}
	
	
	pr_setClockTempo {|newTempo|
		
		(newTempo < (minTempo / 4)).if {
			newTempo = minTempo / 4;
		};
		
		tempo = newTempo;
		clock.tempo_(newTempo);
		this.changed(\tempo, newTempo);
		leading.if {externalTempo = newTempo;};
	}
	
	// responders only for followers
	pr_followerResponders {
		this..net.addOSCResponder(\follower, "/clock", {|header, payload|
			time.receiveTick(*payload);
		}, \leaderOnly);
	}
	
	// responders only for leaders
	pr_leaderResponders {
		
		this.pr_addResponder(oscLeaderResponders, "/requestTempo", {|ti, tR, message, addr|
			
			this.post(message[1].asString ++ " requested a tempo change to " ++ message[2].asFloat ++ " BPS");
			
			allowTempoRequests.if {
				this.changeTempo(message[2].asFloat, message[3].asFloat);
			};
		});
	}
	
	// responders for leaders and followers
	pr_generalResponders {
		
		// chat and shout responders
		this.net.addOSCResponder(\general, "/chat", {|header, payload|
			 (header.name ++ ":  " ++ payload[0].asString).postln;
		});
		
		// a shout should be more visible than this.
		this.net.addOSCResponder(\general, "/shout", {|header, payload|
			 (header.name ++ " (shout):  " ++ payload[0].asString).postln;
			 this.displayShout(header.name, payload[0]);
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
		
		// port responders
		
		this.pr_addResponder(\general, "/pingPort", {|header, payload|
			this.sendPongPort;
		}, \leaderOnly);
		
		this.pr_addResponder(oscGeneralResponders, "/systemPorts", {|ti, tR, message, addr|
			this.pr_shouldFollow(message).if {
				this.pr_managePorts(message[2..32]); // 32 is just a large enough number.
				
				// this is the last step after connecting to 
				// a MandelClock System, so this is a good point
				// to say hello.
				
				helloAfterPorts.if {
					this.sendHello;
					helloAfterPorts = false;	
				};
			};
		});
		
		this.pr_addResponder(oscGeneralResponders, "/takeLead", {|ti, tR, message, addr|
				this.pr_receivedLeaderAnnouncement(message[1].asString);
		});
	}
	
	clear {
		this.net.clearAllResponders;
		
		lastTickSJ.stop;
		tickSJ.stop;
		tempoChangeSJ.stop;
		
		instance = nil;
	}
	
	*clear {
		instance.notNil.if {
			instance.clear;
		};
		
		// stops looking for a leader
		bStrapResponder.remove;	
	}
	
	pr_receivedLeaderAnnouncement {|newLeader|
		
		(newLeader != name).if {
			
			(postPrefix + newLeader + " is our new leader!").postln;
			leaderName = newLeader;
			
			leading.if {
				this.pr_becomeFollower;
				this.post("You are not the leader anymore :-(");
			};
		};	
	} 
	
	pr_shouldFollow {|message|
		^((message[1].asString == leaderName) && leading.not);	
	}
	
	gui {|pos|
		guiInstance.notNil.if {
			this.closeGUI;
		};
		
		guiInstance = MandelClockGUI(this, pos);
		^guiInstance;
	}
	
	tap {
		tapInstance.isNil.if {
			^tapInstance = MandelClockTap.new(this);
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
		^MandelClock.filenameSymbol.asString.dirname ++ "/" ++ filename;
	}
	
	displayShout {|name, message|
		// stupid sanity replacement, should be replaced by better escaping (or stuff)
		name = name.tr($', $ );
		message = message.tr($', $ );
			
		platform.displayNotification(name, message);
	}
	
	pr_sendWindow {|title, func|
		StringInputDialog.new(title, "Send", {|string| 
			func.value(string);
			platform.focusCurrentDocument;
		});		
	}
	
	chatWindow {
		this.pr_sendWindow("MandelClock Chat",  {|string| this.chat(string);});
	}
	
	shoutWindow {
		this.pr_sendWindow("MandelClock Shout", {|string| this.shout(string);});
	}
	
	metro {|pan=0.0, quant=4|
		this.stopMetro;
		
		Server.default.waitForBoot({
			
			SynthDef(\mcTestClick, {|out=0, freq=440, pan=0, amp=0.4|
				var sig = SinOsc.ar(freq, phase:0.5pi);
				sig = sig * EnvGen.ar(Env.perc(0.000001,0.1), doneAction:2);
				
				OffsetOut.ar(out, Pan2.ar(sig, pan) * amp);
			}).add;
		
			metro = Pbind(\instrument, \mcTestClick, \dur, 1, \octave, 6, \pan, pan, \degree, Pseq([7,Pn(0,quant-1)],inf)).play(clock, quant:quant);
		});
	}
	
	impulseMetro {
		this.stopMetro;
		
		Server.default.waitForBoot({
			SynthDef(\mcTestImpulse, {|out=0, amp=1|
				var sig = Impulse.ar(0).dup;
				var remove = Line.kr(0,1,0.1, doneAction:2);
				
				OffsetOut.ar(out, sig);
			}).add;
			
			metro = Pbind(\instrument, \mcTestImpulse, \dur, 1, \amp, 1).play(clock);
		});	
	}
	
	stopMetro {
		metro.stop;
	}
	
	pr_doCmdPeriod {		
		modules.do {|mod| mod.registerCmdPeriod(this);};
		CmdPeriod.doOnce({this.pr_doCmdPeriod});	
	}
	
	tempoBus {
		tempoBusInstance.isNil.if {
			tempoBusInstance = Bus.control(server, 1);
			tempoBusInstance.set(tempo);
			
			tempoBusDependant = {|changed, what, value| (what == \tempo).if {tempoBusInstance.set(value)};};
			this.addDependant(tempoBusDependant);
		};
		^tempoBusInstance;
	}	
}