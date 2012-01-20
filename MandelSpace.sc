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
	
	var <>allowRemoteCode = false;
	
	var envirInstance;
	
	const serializationPrefix = "#SER#"; // the following two letters further describe the type.
	
	classvar defaultDictInstance;
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	*getValueOrDefault {|key|
		MandelClock.instance.notNil.if {
			^MandelClock.instance.space.at(key);
		}Ê{
			^MandelSpace.defaultDict.at(key);
		}
	}
	
	*defaultDict {
		defaultDictInstance.isNil.if {
			defaultDictInstance = (
				scale: \minor,
				tuning: \et12,
				mtranspose: 0,
				ctranspose: 0,
				root: 0
			);
		};
		^defaultDictInstance;	
	}
	
	init {|maclock|
		mc = maclock;	
					
		objects = Dictionary.new;
		
		this.pr_buildEvents();
		this.pr_setDefaults();
	}
	
	createValue {|key, value|
		var obj = this.getObject(key);
		obj.setValue(value, 0);
		^value;
	}
	
	pr_setDefaults {
		MandelSpace.defaultDict.keys.do {|key|
			this.createValue(key, MandelSpace.defaultDict.at(key));
		}
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
	
	getObject {|key|
		var obj = objects.at(key.asSymbol);
		
		obj.isNil.if {
			obj = MandelValue(this, key);
			objects.put(key.asSymbol, obj);
		}
		^obj;
	}
	
	getValue {|key, useDecorator=true|
		var obj = this.getObject(key);
		^obj.getValue();
	}
	
	setValue {|key, value, schedBeats=0.0|
		var obj = this.getObject(key);
		this.sendValue(key, value, schedBeats);
		^obj.setValue(value, schedBeats);
	}
	
	sendValue {|key, value, schedBeats=0.0|
		mc.sendMsgCmd("/value", key.asString, this.serialize(value), schedBeats.asFloat);
	}
	
	// dict interface
	at {|key|
		^this.getValue(key);	
	}
	
	put {|key, value|
		^this.setValue(key, value);	
	}
	
	removeAt {|key|
		// to implement	
	}
	
	// code to string if not a native osc type
	serialize {|value|
		value.isInteger.if {^value};
		value.isFloat.if {^value};
		value.isString.if {^value};
		value.isKindOf(Symbol).if {^(serializationPrefix ++ "SM" ++ value.asString)};
		
		value.isFunction.if {
			value.isClosed.if({
				^(serializationPrefix ++ "CS" ++ value.asCompileString);
			},{
				Error("Only closed functions can be sent through MandelSpace").throw;	
			});
		};
		
		"There is no explicit rule to send the object through MandelSpace - trying asCompileString".warn;
		^(serializationPrefix ++ "CS" ++ value.asCompileString);
	}
	
	// build object if object was serialized
	deserialize {|value|
		var pfl, serType, payload;
		value.isNumber.if {^value};
		value.isKindOf(Symbol).if {value = value.asString};
		
		// if it's a string we need to know if we have to deserialize an object
		value.isString.if {
			value.containsStringAt(0, serializationPrefix).if({
				pfl = serializationPrefix.size;
				serType = value[pfl..pfl+2];
				payload = value[pfl+2..];
				
				(serType == "CS").if {
					allowRemoteCode.if({
						^value.interpret;
					}, {
						"MandelSpace received remote code but wasn't allowed to execute.\nSet allowRemoteCode to true if you know what you're doing!".warn;
					});	
				};
				
				(serType == "SM").if {
					^value.asSymbol;	
				};
					
			}, {
				^value; // normal string.
			});
		};
		
		// if everything else fails ...
		^value;
	}
	
	addDependency {|father, son|
		var obj = this.getObject(father);
		obj.pr_receiveDependency(son);
	}
	
	clearDependenciesFor {|son|
		objects.do {|obj|
			obj.pr_removeDependenciesFor(son);	
		}
	}
	
	pr_callSon {|key|
		var obj = this.getObject(key);
		obj.pr_valueHasChanged;
	}
	
	onBecomeLeader {|mc|
		mc.addResponder(\leader, "/requestValueSync", {|ti, tR, message, addr|
			objects.keys.do {|key|
				var value = objects.at(key).bdl;
				this.sendValue(key, value.value(), 0.0);
				value.list.do {|item|
					this.sendValue(key, item[1], item[0]);
				};
			};
		});	
	}
	
	onStartup {|mc|
		mc.addResponder(\general, "/value", {|ti, tR, message, addr|
			(message[1].asString != mc.name).if {
				this.getObject(message[2].asSymbol).setValue(this.deserialize(message[3]), message[4].asFloat);
			};
		});
	}
	
	envir {
		envirInstance.isNil.if {
			envirInstance = MandelEnvironment(this);
		};
		^envirInstance;	
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
	
	// function interface
	value {
		^this.getValue();
	}
	
	asStream {
		^Routine({
			inf.do {
				this.getValue().yield;
			};
		});
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
	
	// maybe remove this, move to setValue
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