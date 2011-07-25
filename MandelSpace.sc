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
	var <mc;
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	

	
	init {|maclock|
		mc = maclock;	
					
		objects = Dictionary.new;
		
		this.pr_buildEvents();
		this.pr_setDefaults();
	}
	
	at {|key|
		^this.pr_getObject(key);	
	}
	
	createValue {|key, value|
		var obj = this.pr_getObject(key);
		obj.pr_setBDL(value, 0);
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
	
	pr_getObject {|key|
		var obj = objects.at(key.asSymbol);
		
		obj.isNil.if {
			obj = MandelValue(this, key);
			objects.put(key.asSymbol, obj);
		}
		^obj;
	}
	
	getValue {|key, useDecorator=true|
		var obj = this.pr_getObject(key);
		^obj.getValue();
	}
	
	setValue {|key, value, schedBeats=0.0|
		var obj = this.pr_getObject(key);
		mc.sendMsgCmd("/value", key.asString, value, schedBeats.asFloat);
		^obj.setValue(value, schedBeats);
	}
	
	addDependency {|father, son|
		var obj = this.pr_getObject(father);
		obj.pr_receiveDependency(son);
	}
	
	clearDependenciesFor {|son|
		objects.do {|obj|
			obj.pr_removeDependenciesFor(son);	
		}
	}
	
	pr_callSon {|key|
		var obj = this.pr_getObject(key);
		obj.pr_valueHasChanged;
	}
	
	onBecomeLeader {|mc|
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
	
	onStartup {|mc|
		mc.addResponder(\general, "/value", {|ti, tR, message, addr|
			(message[1].asString != mc.name).if {
				this.pr_setBDL(message[2].asSymbol, message[3], message[4].asFloat);
			};
		});
	}
}

MandelValue  {
	var <key, <>bdl, <decorator, <listeners, <dependencies, <>nodeProxy;
	var space;
	
	*new {|space, key|
		^super.new.init(space, key);
	}
	
	init {|aspace, akey|
		space = aspace;
		key = akey.asSymbol;
		
		listeners = List();
		dependencies = IdentitySet();
	}
	
	getValue {|useDecorator=true|
		(useDecorator && decorator.notNil).if ({
			^decorator.value(bdl.value, space, key);
		}, {
			^bdl.value;
		});	
	}
	
	setValue {|value, schedBeats=0.0|
		^this.pr_setBDL(value, schedBeats);	
	}
	
	
	pr_setBDL {|value, schedBeats|		
		bdl.isNil.if ({
			bdl = BeatDependentValue(value);
			^value;	
		}, {
			^bdl.schedule(value, schedBeats);
		});		
	}
	
	decorator_ {|func|
		decorator = func;	
	}
	
	addListener {|func|
		listeners.add(func);
		this.pr_activateChangeFunc;
	}
	
	clearListeners {
		listeners.clear;
		this.pr_deactivateChangeFunc;
	}
	
	addDependency {|father|
		space.addDependency(father, this.key);
	}
	
	pr_receiveDependency {|son|
		dependencies.add(son);
		this.pr_activateChangeFunc;
	}
	
	clearDependencies {
		space.clearDependenciesFor(this.key);	
	}
	
	pr_removeDependenciesFor {|son|
		dependencies.remove(son);
		this.pr_deactivateChangeFunc;
	}
	
	pr_valueHasChanged {		
		listeners.do {|func|
			func.value(this.getValue(), space, key);
		};
		
		dependencies.do {|son|
			space.pr_callSon(son);	
		};
	}
	
	mapToProxySpace {|lag=0.0|
		var ps, node;
				
		space.mc.setProxySpace;
		
		ps = space.mc.proxySpace;
		
		node = ps.envir[key];
		node.isNil.if {
			node = NodeProxy.control(ps.server, 1);
			ps.envir.put(key, node);
		};
		
		node.put(0, {|value=0, lag=0| Lag2.kr(value, lag)}, 0, [\value, this.getValue().asFloat, \lag, lag]);
		this.addListener({|v| node.set(\value, v.asFloat) });
		nodeProxy = node;
		
		^node;
	}
	
	pr_activateChangeFunc {
		bdl.notNil.if {
			bdl.onChangeFunc = {this.pr_valueHasChanged};		};
	}
	
	pr_deactivateChangeFunc {		
		((listeners.size == 0) && (dependencies.size == 0)).if {
			bdl.notNil.if {
				bdl.onChangeFunc = nil;
			};
		};
	}
}