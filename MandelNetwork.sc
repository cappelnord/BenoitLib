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
	classvar <>dumpOSC = true;
	
	// OSCresponders
	var oscGeneralResponders;
	var oscLeaderResponders;
	var oscFollowerResponders;
	
	var <addrDict;
	
	var mc;

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
		this.pr_managePorts(ports);
		
		// bookkeeping for responders
		oscGeneralResponders = Dictionary.new;
		oscLeaderResponders = Dictionary.new;
		oscFollowerResponders = Dictionary.new;
	}
	
	onStartup {|mc|		
		this.addOSCResponder(\general, "/pingPort", {|header, payload|
			this.sendPongPort;
		}, \leaderOnly);
		
		this.addOSCResponder(\general, "/systemPorts", {|header, payload|
			this.pr_managePorts(payload);
		}, \leaderOnly);
	}
	
	onBecomeLeader {|mc|
		this.addOSCResponder(\leader, "/requestPort", {|header, payload|
			this.addPort(payload[0].asInteger);
			mc.post(header.name ++ " requested port " ++ payload[0]);
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
		this.net.sendMsgCmd("/pongPort", NetAddr.langPort);
	}
	
	sendSystemPorts {
		var intKeys = (addrDict.keys.collect{|i| i.asInteger}).asArray;
		this.sendMsgCmd("/systemPorts", *intKeys);	
	}
	
	pr_respondersForKey {|key|
		^key.switch (
			\general,  {oscGeneralResponders},
			\leader,   {oscLeaderResponders},
			\follower, {oscFollowerResponders}
		);
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
	
	clearRespondersÊ{|key|
		var list = this.pr_respondersForKey(key);
		list.do {|item| item.remove;};
		list.clear;	
	}
	
	// rewrite
	sendMsgCmd {|... args|
		var cmd = args[0];
		var argNum = args.size;
		
		// args.postcs;
		
		args = args[1..];
		
		(argNum > 1).if ({
			this.sendMsg(oscPrefix ++ cmd, mc.name, *args);
		},{
			this.sendMsg(oscPrefix ++ cmd, mc.name);
		});
	}
	
	// sendMessage delivers to NetAddr
	sendMsg {|... args|
		dumpOSC.if {
			("OSC TX: " ++ args.asCompileString)	.postln;
		};
		
		addrDict.do {|addr|
				addr.sendMsg(*args);
		};
	}
	
	addOSCResponder {|dictKey, cmdName, action, strategy=\no|
		var dict = this.pr_respondersForKey(dictKey);
		var responder = OSCresponder(nil, oscPrefix ++ cmdName, {|ti, tR, message, addr|
			
			var doDispatch = true;
			
			// Seperate Header from Payload
			var header = ();
			var payload = message[2..];
			
			dumpOSC.if {
				("OSC RX: " ++ message.asCompileString).postln;
			};
			
			header[\cmdName] = message[0].asString;
			header[\name] = message[1].asString;
			
			// Register Message and/or discard
			
			// Discard by Strategy
			
			// Dispatch
			doDispatch.if {
				action.value(header, payload);
			};	
		}).add;
		dict.add(cmdName -> responder);
	}
}