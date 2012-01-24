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
	classvar <>oscPrefix = "/mc";
	classvar bStrapResponder;
	
	classvar <>debug = false;
	classvar <>dumpOSC = false;
	
	var <>tapInstance;
	
	var <clock;
	
	var <>externalTempo; // the tempo set by external clock
	var <>tempo; // the internal tempo, may differ because it does a correction
	
	var lastTickSJ;
	
	var tickSJ;
	var tempoChangeSJ;
			
	// OSCresponders
	var oscGeneralResponders;
	var oscLeaderResponders;
	var oscFollowerResponders;
		
	var <leading = false;
	var <leaderName;
	
	var <addrDict;
	
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
	
	var <proxySpace;
		
	var <>postPrefix = "MandelClock: ";
		
	var metro;
	
	var dropSchedDict;
	
		
	var <>dropFunc = nil; 
	
	// Modules
	var modules;

	var <space;
	var <tools;
	var <platform;
	var <time;
			
	*startLeader {|name, startTempo = 2.0, timeClass|
		
		timeClass = timeClass ? MandelTimeDriver;
		
		instance.notNil.if {
			"THERE IS ALREADY A MANDELCLOCK INSTANCE".postln;
			^instance;	
		};
		
		(NetAddr.langPort != 57120).if {
			"*** Warning! ***".postln;
			("Your sclang port is not 57120, it's " ++ NetAddr.langPort ++ "!").postln;
			"This isn't a problem, but followers must use this port as".postln;
			"second argument in the startFollower call.".postln;
			"".postln; 	
		};
		
		instance = MandelClock.new(name, 0, startTempo, name, [NetAddr.langPort], leading:true, timeClass:timeClass);
		^instance;
	}
	
	*startFollower {|name, port=57120, action, timeClass|
		
		var addr;
		
		timeClass = timeClass ? MandelTimeDriver;
		
		instance.notNil.if {
			"THERE IS ALREADY A MANDELCLOCK INSTANCE".postln;
			^instance;	
		};
		
		NetAddr.broadcastFlag_(true);
		addr = NetAddr("255.255.255.255", port);
		addr.sendMsg(oscPrefix ++ "/requestPort", name, NetAddr.langPort);
		
		"Waiting for a signal from the Leader ...".postln;
		
		bStrapResponder = OSCresponder(nil, oscPrefix ++ "/clock", {|ti, tR, message, addr|
			
			bStrapResponder.remove;
			
			instance = MandelClock.new(name, message[3], message[4], message[1].asString,[port], false, timeClass:timeClass);
			instance.helloAfterPorts = true;
			instance.publishPorts;
			
			("... you are now following " ++ message[1].asString ++ "!").postln;
			
			action.value(instance);
			instance.sendMsgCmd("/requestValueSync");
			
		}).add;
		
	}
	
	*new {|name, startBeat, startTempo, leaderName, ports, leading=false, timeClass|
		
		name.isNil.if {
			name = "RandomUser" ++ 100000.rand;	
		};
		
		^super.new.init(name, startBeat, startTempo, leaderName, ports, leading, timeClass);
	}
	
	init {|a_name, startBeat, startTempo, a_leaderName, ports, a_leading, timeClass|
		
		name = a_name;
		leaderName = a_leaderName;
		leading = a_leading;
		
		NetAddr.broadcastFlag_(true);
		
		// start networking
		ports = ports ? [57120];
		
		// this is a workaround, check if ports are publishhed correctly
		ports.includes(57120).not.if {
			ports = ports ++ 57120;
		};
		
		addrDict = IdentityDictionary.new;
		this.pr_managePorts(ports);
		
		// bookkeeping for responders
		oscGeneralResponders = Dictionary.new;
		oscLeaderResponders = Dictionary.new;
		oscFollowerResponders = Dictionary.new;
		
		// bookkeeping for scheduling
		dropSchedDict = Dictionary.new;
		
		
		// start the clock
		clock = TempoClock.new(startTempo, startBeat, queueSize:4096);
		clock.permanent_(true);
		
		TempoClock.default = clock;		
		externalTempo = startTempo;
		tempo = startTempo;
		
		// init Modules
		this.pr_initModules;
		
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
	
	pr_initModules {
		modules = List.new;

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
	
	takeLead {
		this.sendMsgCmd("/takeLead");
		
		leading.not.if {
			this.pr_becomeLeader;
		};
	}
	
	hello {
		this.sendMsgCmd("/hello");	
	}
	
	requestHello {
		this.sendMsgCmd("/requestHello");	
	}
	
	chat {|message|
		this.sendMsgCmd("/chat", message);
	}
	
	shout {|message|
		this.sendMsgCmd("/shout", message);
	}
	
	systemPorts {
		var intKeys = (addrDict.keys.collect{|i| i.asInteger}).asArray;
		this.sendMsgCmd("/systemPorts", *intKeys);	
	}
	
	publishPorts {
		this.sendMsgCmd("/publishPorts");	
	}
	
	// of course it could be the same method for a leader and a follower
	// but I think normally only a leader should change the tempo, so making
	// this cut enforces this somehow.
	requestTempo {|newTempo, dur=0|
		leading.not.if {
			newTempo = this.pr_safeTempo(newTempo);
			this.sendMsgCmd("/requestTempo", newTempo.asFloat, dur.asFloat);
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
	
	// sendMessageCmd adds name and oscPrefix
	sendMsgCmd {|... args|
		
		var message = args[0];
		var argNum = args.size;
		
		// args.postcs;
		
		args = args[(1..(args.size-1))]; // remove first item, i think this is dumb
		
		(argNum > 1).if ({
			this.sendMsg(oscPrefix ++ message, name, *args);
		},{
			this.sendMsg(oscPrefix ++ message, name);
		});
	}
	
	// sendMessage delivers to NetAddr
	sendMsg {|... args|
		
		dumpOSC.if {args.postcs;};
		
		addrDict.do {|addr|
				addr.sendMsg(*args);
		};
	}
	
	pr_becomeLeader {
		
		leaderName = name;
		leading = true;
		
		// clear follower responders
		this.pr_clearResponders(oscFollowerResponders);
		
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
		this.pr_clearResponders(oscLeaderResponders);

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
		this.tools.pr_setTempoProxy(newTempo); // refactor to tempo change listener soon
		leading.if {externalTempo = newTempo;};
	}
	
	addResponder {|key, cmd, function |
	
		var d = (\general: oscGeneralResponders,
				\leader: oscLeaderResponders,
				\follower: oscFollowerResponders);
				
		var dict = d[key];
		
		key.notNil.if ({
			this.pr_addResponder(dict, cmd, function);
		}, {
			("Key " ++ key.asString ++ " not found!").postln;
		});
	}
	
	// this function should go sometimes
	pr_addResponder {|dict, cmd, function|
		dict.add(cmd -> OSCresponder(nil, oscPrefix ++ cmd, function).add);
	}
	
	// responders only for followers
	pr_followerResponders {
		this.pr_addResponder(oscFollowerResponders, "/clock", {|ti, tR, message, addr|
			this.pr_shouldFollow(message).if {
				time.receiveTick(message[2], message[3], message[4]);
			};
		});
	}
	
	// responders only for leaders
	pr_leaderResponders {
		
		this.pr_addResponder(oscLeaderResponders, "/requestPort", {|ti, tR, message, addr|
			this.pr_addPort(message[2].asInteger);
			this.post(message[1].asString ++ " requested port " ++ message[2]);
		});
		
		this.pr_addResponder(oscLeaderResponders, "/publishPorts", {|ti, tR, message, addr|
			leading.if {
				this.systemPorts;
			};
		});
		
		this.pr_addResponder(oscLeaderResponders, "/requestTempo", {|ti, tR, message, addr|
			
			this.post(message[1].asString ++ " requested a tempo change to " ++ message[2].asFloat ++ " BPS");
			
			allowTempoRequests.if {
				this.changeTempo(message[2].asFloat, message[3].asFloat);
			};
		});
	}
	
	// responders for leaders and followers
	pr_generalResponders {
		
		// drop responder
		
		this.pr_addResponder(oscGeneralResponders, "/drop", {|ti, tR, message, addr|
			(message[1].asString != name).if {
				this.pr_receiveDrop(message[2].asInteger, message[3].asFloat);
			};
		});
		
		// chat and shout responders
				
		this.pr_addResponder(oscGeneralResponders, "/chat", {|ti, tR, message, addr|
			 (message[1].asString ++ ":  " ++ message[2].asString).postln;
		});
		
		// a shout should be more visible than this.
		this.pr_addResponder(oscGeneralResponders, "/shout", {|ti, tR, message, addr|
			 (message[1].asString ++ " (shout):  " ++ message[2].asString).postln;
			 this.displayShout(message[1].asString, message[2].asString);
		});
		
		this.pr_addResponder(oscGeneralResponders, "/hello", {|ti, tR, message, addr|
			
			var name = message[1].asString;
			
			(leaderName == name).if({
				(name ++ " is the leader.").postln;	
			},{
				(name ++ " is following the leader.").postln;
			});
		});
		
		this.pr_addResponder(oscGeneralResponders, "/requestHello", {|ti, tR, message, addr|
			this.hello;
		});
		
		// port responders
		
		this.pr_addResponder(oscGeneralResponders, "/pingPort", {|ti, tR, message, addr|
			this.pr_shouldFollow(message).if {
				this.sendMsgCmd("pongPort", NetAddr.langPort);
			};
		});
		
		this.pr_addResponder(oscGeneralResponders, "/systemPorts", {|ti, tR, message, addr|
			this.pr_shouldFollow(message).if {
				this.pr_managePorts(message[2..32]); // 32 is just a large enough number.
				
				// this is the last step after connecting to 
				// a MandelClock System, so this is a good point
				// to say hello.
				
				helloAfterPorts.if {
					this.hello;
					helloAfterPorts = false;	
				};
			};
		});
		
		this.pr_addResponder(oscGeneralResponders, "/takeLead", {|ti, tR, message, addr|
				this.pr_receivedLeaderAnnouncement(message[1].asString);
		});
	}
	
	
	clearAllResponders {
		this.pr_clearResponders(oscGeneralResponders);
		this.pr_clearResponders(oscLeaderResponders);
		this.pr_clearResponders(oscFollowerResponders);
	}
	
	pr_clearResponders {|list|
		list.do {|item| item.remove;};
		list.clear;
	}
	
	clear {
		this.clearAllResponders;
		
		lastTickSJ.stop;
		tickSJ.stop;
		tempoChangeSJ.stop;
		this.clearTempoProxy;
		
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
	
	pr_managePorts {|ports|
		
		var addList = List.new;
		var remList = List.new;
				
		ports.do {|item|
			
			item = item.asInteger;
			
			addrDict.includesKey(item).not.if {
				addList = addList.add((item -> NetAddr("255.255.255.255", item)));
			};	
		};
		
		addrDict.keys.do {|key|
			ports.includes(key).not.if {
				remList.add(key);	
			};	
		};
		
		addList.do {|ass|
			addrDict.add(ass);	
		};
		
		remList.do {|key|
			addrDict.removeAt(key);	
		};
	}
	
	pr_addPort {|port|
		
		port = port.asInteger;
		
		addrDict.includesKey(port).not.if {
			addrDict = addrDict.add(port -> NetAddr("255.255.255.255", port));
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
	
	// it's not very nice to check for a class (anti OO, a class COULD act as a ProxySpace)
	// not good: can't set another ps.
	setProxySpace {|ps|
		proxySpace.isKindOf(ProxySpace).not.if {
			((ps == nil) && (currentEnvironment.isKindOf(ProxySpace))).if {
				ps = currentEnvironment;	
			};
			
			(ps.isKindOf(ProxySpace)).if({
				proxySpace = ps;
			},{
				"You need to specify a ProxySpace!".throw;
			});
		}
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
	
	drop {|bar, function|
	
		var schedBeat = bar * beatsPerBar;
		
		(schedBeat > clock.beats).if ({
			dropSchedDict.at(bar).isNil.if {
				clock.schedAbs(schedBeat - 0.0000001 ,{
					dropSchedDict.at(bar).value;
					dropSchedDict.removeAt(bar);
					nil;
				});
			};
						
			dropSchedDict.removeAt(bar); // does nothing on first time
			dropSchedDict.put(bar, function);
			
		}, {
			("Bar " ++ bar ++ " is in the past! Didn't do anything.").warn;
		});
	}
	
	startDrop {|quant=0|
		this.sendMsgCmd("/drop", quant.asInteger, clock.beats.asFloat);
		this.pr_receiveDrop(quant.asInteger, clock.beats.asFloat);
	}
	
	pr_receiveDrop {|quant, referenceBeat|
		var schedBeat;
		
		(quant == 0).if {
			this.runDropFunc;
		};
				
		(dropFunc != nil).if {
			schedBeat = ceil(referenceBeat / quant) * quant;
			
			(schedBeat < clock.beats).if ({
				this.runDropFunc;
			}, {
				clock.schedAbs(schedBeat - 0.0000001 ,{
					this.runDropFunc;
					nil;
				});
			});
		};
	}
	
	runDropFunc {
		dropFunc.isNil.not.if ({
			dropFunc.value;
			this.displayShout("", "Hot Drop!");
			"Hot Drop!".postln;
		}, {
			this.displayShout("", "Cold Drop!");
			"Cold Drop!".postln;
		});
		dropFunc = nil;
	}
	
	pr_doCmdPeriod {		
		modules.do {|mod| mod.registerCmdPeriod(this);};
		CmdPeriod.doOnce({this.pr_doCmdPeriod});	
	}
	
	// deprecated

	getValue {|key, useDecorator=true|
		"getValue is going to be removed from MandelClock instance. Use m.space.getValue".postln;
		^space.getValue(key, useDecorator);
	}
	
	setValue {|key, value, schedBeats=0.0|
		"setValue is going to be removed from MandelClock instance. Use m.space.setValue".postln;
		^space.setValue(key, value, schedBeats);
	}
	
	makeTempoProxy {
		"makeTempoProxy is going to be removed from MandelClock instance. Use m.tools.makeTempoProxy".postln;
		^tools.makeTempoProxy;
	}

	clearTempoProxy {
		"clearTempoProxy is going to be removed from MandelClock instance. Use m.tools.makeTempoProxy".postln;
		^tools.clearTempoProxy;
	}	
}