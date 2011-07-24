/*
	MandelSpace
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Shared Variable Space used by MandelClock
	
*/

MandelSpace : MandelModule {
	
	var <objects;
	var mc;
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	init {|maclock|
		mc = maclock;
		
		objects = Dictionary.new;
		
		this.pr_buildEvents();
		this.pr_setDefaults();
	}
	
	createValue {|key, value|
		var obj = this.pr_getObject(key);
		obj.bdl = BeatDependentValue(value);
		^value;
	}
	
	pr_setDefaults {
		this.createValue(\scale, \minor);
		this.createValue(\tuning, \et12);
		this.createValue(\mtranspose, 0);
		this.createValue(\ctranspose, 0);
		this.createValue(\root, 0);	
	}
	
	pr_buildEvents {
		Event.addEventType(\mandelspace, {
			var schedBeats;
			~deltaSched = ~deltaSched ? 0.0;
			schedBeats = MandelClock.instance.clock.beats + ~deltaSched;
			currentEnvironment.keys.do {|key|
				((key != \type) && (key != \dur)).if {
					MandelClock.instance.space.setValue(key, currentEnvironment.at(key), schedBeats);
				};
			};
		});
	}
	
	pr_setBDL {|key, value, schedBeats|
		var obj = this.pr_getObject(key);
		var bdl = obj.at(\bdl);
		
		bdl.isNil.if ({
			this.createValue(key, value);
			^value;	
		}, {
			^bdl.schedule(value, schedBeats);
		});		
	}
	
	pr_getObject {|key|
		var obj = objects.at(key.asSymbol);
		
		obj.isNil.if {
			obj = (
				\key: key,
				\bdl: nil,
				\decorator: nil,
				\listeners: List(),
				\subscribers: List(),
				\nodeProxy: nil
			);
			objects.put(key.asSymbol, obj);
		}
		
		^obj;
	}
	
	getValue {|key, useDecorator=true|
		
		var obj = this.pr_getObject(key);
		
		(useDecorator && obj.at(\decorator).notNil).if ({
			^obj.at(\decorator).value(obj.at(\bdl).value, this, key);
		}, {
			^obj.at(\bdl).value;
		});
	}
	
	setValue {|key, value, schedBeats=0.0|
		mc.sendMsgCmd("/value", key.asString, value, schedBeats.asFloat);
		^this.pr_setBDL(key, value, schedBeats);
	}
	
	setDecorator {|key, func|
		var obj = this.pr_getObject(key);
		obj.decorator = func;	
	}
	
	addListener {|key, func|
		var obj = this.pr_getObject(key);
		obj.at(\listeners).add(func);
		this.pr_activateChangeFunc(obj);
	}
	
	clearListeners {|key|
		var obj = this.pr_getObject(key);
		obj.at(\listeners).clear();
		this.pr_deactivateChangeFunc(obj);
	}
	
	addSubscriber {|key, subscriber|
		var obj = this.pr_getObject(key);
		obj.at(\subscribers).add(subscriber);
		this.pr_activateChangeFunc(obj);
	}
	
	clearSubscribers {|key|
		var obj = this.pr_getObject(key);
		obj.at(\subscribers).clear();
		this.pr_deactivateChangeFunc(obj);
	}
	
	valueHasChanged {|key|
		var obj = this.pr_getObject(key);
		
		obj.at(\listeners).do {|func|
			func.value(this.getValue(key), this, key);
		};
		
		obj.at(\subscribers).do {|subscriber|
			this.valueHasChanged(subscriber);	
		};
	}
	
	mapToProxySpace {|key, lag=0.0|
		var obj = this.pr_getObject(key);
		var ps, node;
		
		key = key.asSymbol;
		
		mc.setProxySpace;
		
		ps = mc.proxySpace;
		
		node = ps.envir[key];
		node.isNil.if {
			node = NodeProxy.control(ps.server, 1);
			ps.envir.put(key, node);
		};
		
		node.put(0, {|value=0| Lag2.kr(value, lag)}, 0, [\value, this.getValue(key).asFloat]);
		this.addListener(key, {|v| node.set(\value, v.asFloat) });
		obj.nodeProxy = node;
		
		^node;
	}
	
	pr_activateChangeFunc {|obj|
		var bdl = obj.at(\bdl);
		bdl.notNil.if {
			bdl.onChangeFunc = {this.valueHasChanged(obj.at(\key))};		};
	}
	
	pr_deactivateChangeFunc {|obj|
		var bdl = obj.at(\bdl);
		
		((obj.at(\listeners).size == 0) && (obj.at(\subscirbers).size == 0)).if {
			bdl.notNil.if {
				bdl.onChangeFunc = nil;
			};
		};
	}
	
	onBecomeLeader {
		mc.addResponder(\leader, "/requestValueSync", {|ti, tR, message, addr|
			objects.keys.do {|key|
				var value = objects.at(key).bdl;
				mc.sendMsgCmd("/value", key.asString, value.value(), 0.0);
				value.list.do {|item|
					mc.sendMsgCmd("/value", key.asString, item[1], item[0]);
				};
			};
		});	
	}
	
	onStartup {
		mc.addResponder(\general, "/value", {|ti, tR, message, addr|
			(message[1].asString != mc.name).if {
				this.pr_setBDL(message[2].asSymbol, message[3], message[4].asFloat);
			};
		});
	}
}