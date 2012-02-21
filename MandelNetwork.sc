/*
	MandelNetwork
	(c) 2012 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	MandelNetwork manages incoming and outgoing
	OSC traffic and ports.
	
*/

MandelNetwork : MandelModule {
	
	classvar <>oscPrefix = "/mc";
	
	var <>dumpTXOSC = false;
	var <>dumpRXOSC = false;
	
	// OSCresponders
	var oscGeneralResponders;
	var oscLeaderResponders;
	var oscFollowerResponders;
	
	var <addrDict;
	
	var currentMessageID;
	
	var mc;
	
	var oscQueue;
	var oscQueueRoutine;
	var oscQueueWaitTime = 0.01;
	
	var burstGuardDict;

	*new {|maclock, ports|
		^super.new.init(maclock);	
	}
	
	init {|maclock, ports|
		mc = maclock;
		
		// start networking
		ports = ports ? [57120];
		
		// this is a workaround, check if ports are publishhed correctly
		ports.includes(57120).not.if {
			ports = ports ++ 57120;
		};
		
		addrDict = IdentityDictionary.new;
		this.prManagePorts(ports);
		
		// bookkeeping for responders
		oscGeneralResponders = Dictionary.new;
		oscLeaderResponders = Dictionary.new;
		oscFollowerResponders = Dictionary.new;
		
		currentMessageID = 5000.rand;
		
		oscQueue = LinkedList.new;
		oscQueueRoutine = Routine({}); // dummy
		
		burstGuardDict = IdentityDictionary.new;
	}
	
	onStartup {|mc|		
		this.addOSCResponder(\general, "/pingPort", {|header, payload|
			this.sendPongPort;
		}, \leaderOnly);
		
		this.addOSCResponder(\general, "/systemPorts", {|header, payload|
			this.prManagePorts(payload);
		}, \leaderOnly);
	}
	
	onBecomeLeader {|mc|
		this.addOSCResponder(\leader, "/requestPort", {|header, payload|
			this.addPort(payload[0].asInteger);
			mc.post(header.name ++ " requested port " ++ payload[0]);
			this.sendPublishPorts;
		});
		
		this.addOSCResponder(\leader, "/publishPorts", {|header, payload|
			this.sendSystemPorts;
		});	
	}
	
	/*
	onBecomeFollower {|mc|
		
	}
	
	registerCmdPeriod {|mc|
		
	}
	*/
	
	sendPongPort {
		this.sendMsgCmd("/pongPort", NetAddr.langPort);
	}
	
	sendSystemPorts {
		var intKeys = (addrDict.keys.collect{|i| i.asInteger}).asArray;
		this.sendMsgBurst("/systemPorts", \critical, *intKeys);
	}
	
	sendPublishPorts {
		this.sendMsgBurst("/publishPorts", \critical);
	}
	
	prRespondersForKey {|key|
		^key.switch (
			\general,  {oscGeneralResponders},
			\leader,   {oscLeaderResponders},
			\follower, {oscFollowerResponders}
		);
	}
	
	prManagePorts {|ports|
		var addList = List.new;
				
		ports.do {|item|
			item = item.asInteger;
			
			addrDict.includesKey(item).not.if {
				addList = addList.add((item -> NetAddr("255.255.255.255", item)));
			};	
		};
		
		addList.do {|item|
			addrDict.add(item);	
		};
	}
	
	addPort {|port|
		port = port.asInteger;
		
		addrDict.includesKey(port).not.if {
			addrDict = addrDict.add(port -> NetAddr("255.255.255.255", port));
		};
	}
	
	clearAllResponders {
		[\leader, \follower, \general].do {|key|
			this.clearResponders(key);
		};
	}
	
	clearResponders {|key|
		var list = this.prRespondersForKey(key);
		list.do {|item| item.remove;};
		list.clear;	
	}
	
	// this is all slightly off  ...
	
	// send a message without defering
	sendMsgDirect {|... args|
		var cmd = args[0];
		var messageID = 0; // no burst
		args = args[1..];
		
		this.sendMsg(oscPrefix ++ cmd, mc.name, messageID, *args);
	}
	
	// send a message without a burst ID
	sendMsgCmd {|... args|
		var cmd = args[0];
		var messageID = 0; // no burst
		args = args[1..];
		
		this.dispatchMsg(oscPrefix ++ cmd, mc.name, messageID, *args);
	}
	
	sendMsgBurst {|... args|
		var cmd = args[0];
		var burstNum = 1; // in case no burst strategy is found
		var burstSpan = 1;
		var burst = args[1];
		var messageID = this.nextMessageID;
		var burstWait;
		args = args[2..];
		
		burst.isKindOf(Symbol).if {
			burst = (
				time: #[2, 0.25],
				critical: #[8, 4],
				important: #[4, 2],
				timeCritical: #[8, 0.5],
				relaxed: #[4,8]
			).at(burst);
		};
		
		burst.isKindOf(Collection).if {
			burstNum = burst[0];
			burstSpan = burst[1];	
		};
		
		
		burstWait = burstSpan / burstNum;
		
		{
			burstNum.do {
				this.dispatchMsg(oscPrefix ++ cmd, mc.name, messageID, *args);
				burstWait.wait;	
			};
		}.fork;
	}
	
	dispatchMsg {|... args|
		oscQueue.add(args);
		
		// start a new Routine - could be solved better probably
		oscQueueRoutine.isPlaying.not.if {
			oscQueueRoutine = {
				while({oscQueue.last.isNil.not}, {
					this.sendMsg(*oscQueue.pop);
					oscQueueWaitTime.wait;
				});
			}.fork;
		};
	}
	
	// sendMessage delivers to NetAddr
	sendMsg {|... args|
		dumpTXOSC.if {
			("OSC TX: " ++ args.asCompileString)	.postln;
		};
		
		addrDict.do {|addr|
				addr.sendMsg(*args);
		};
	}
	
	addOSCResponder {|dictKey, cmdName, action, strategy=\no|
		var dict = this.prRespondersForKey(dictKey);
		var responder = OSCresponder(nil, oscPrefix ++ cmdName, {|ti, tR, message, addr|
			
			var doDispatch = true;
			
			// Seperate Header from Payload
			var header = ();
			var payload = message[3..];
			
			dumpRXOSC.if {
				("OSC RX: " ++ message.asCompileString).postln;
			};
			
			header[\cmdName] = message[0].asString;
			header[\name] = message[1].asString;
			header[\messageID] = message[2];
			
			// Register Message and/or discard
			doDispatch = doDispatch && this.prFilterBurstMessages(message[1].asSymbol, message[2]);
			
			// Discard by Strategy
			doDispatch.if {
				doDispatch = strategy.switch(
					\leaderOnly, {(header.name == mc.leaderName.asString) && mc.leading.not},
					\dropOwn, {(header.name != mc.name)},
					{true}
				);
			};
			
			// Dispatch
			doDispatch.if ({
				action.value(header, payload);
			}, {
				dumpRXOSC.if {"OSC RX: Message discarded.".postln;};
			});	
		}).add;
		
		dict.add(cmdName -> responder);
	}
	
	prFilterBurstMessages {|name, messageID|
		var queue = burstGuardDict.at(name);
		var curBeat = mc.clock.beats;
		var checkList = true;
		var last;
		
		// early out
		(messageID == 0).if {^true;};
		
		// build queue if necessary
		queue.isNil.if {
			queue = LinkedList.new;
			burstGuardDict.put(name, queue);	
		};
		
		// drop old messageIDs
		while({checkList}, {
			last = queue.last;
			last.isNil.if({
				checkList = false
			},{
				(last[0] <= curBeat).if ({
					queue.pop;
				}, {
					checkList = false;
				});
			});
		});
		
		// linear search, exit if found
		queue.do {|item|
			(item[1] == messageID).if {^false};	
		};
		
		// if not found add to list, exit with true
		queue.add([curBeat+32, messageID]);
		^true;
	}
	
	nextMessageID {
		var ret = currentMessageID;
		currentMessageID = currentMessageID + 1;
		^ret;	
	}
}