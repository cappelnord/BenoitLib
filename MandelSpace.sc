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
				root: 0,
				stepsPerOctave: 12,
				octaveRatio: 2
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
	
	setValue {|key, value, schedBeats|
		var obj = this.getObject(key);
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
						^payload.interpret;
					}, {
						"MandelSpace received remote code but wasn't allowed to execute.\nSet allowRemoteCode to true if you know what you're doing!".warn;
					});	
				};
				
				(serType == "SM").if {
					^payload.asSymbol;	
				};
					
			}, {
				^value; // normal string.
			});
		};
		
		// if everything else fails ...
		^value;
	}
	
	addRelation {|father, son|
		var obj = this.getObject(father);
		obj.pr_receiveRelation(son);
	}
	
	clearRelationsFor {|son|
		objects.do {|obj|
			obj.pr_removeRelationsFor(son);	
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
	var <key, <>bdl, <decorator, <relations, <>nodeProxy, <quant;
	var space;
	
	*new {|space, key|
		^super.new.init(space, key);
	}
	
	init {|aspace, akey|
		space = aspace;
		key = akey.asSymbol;
		
		relations = IdentitySet();
	}
	
	// function interface
	value {
		^this.getValue();
	}
	
	asStream {
		^Routine({
			while {true} {
				this.getValue().yield;
			};
		});
	}
	
	quant_ {|val|
		quant = val.asQuant;	
	}
	
	getValue {|useDecorator=true|
		(useDecorator && decorator.notNil).if ({
			^decorator.value(bdl.value, space, key);
		}, {
			^bdl.value;
		});	
	}
	
	setValue {|value, schedBeats|
		schedBeats.isNil.if {
			quant.isNil.if ({
				schedBeats = 0.0;	
			}, {
				schedBeats = quant.nextTimeOnGrid(space.mc.clock);
			});
		};
		space.sendValue(key, value, schedBeats);
		^this.pr_setBDL(value, schedBeats);	
	}
	
	// maybe remove this, move to setValue
	pr_setBDL {|value, schedBeats|		
		bdl.isNil.if ({
			bdl = BeatDependentValue(value);
			bdl.onChangeFunc = {this.pr_valueHasChanged;};
			^value;	
		}, {
			^bdl.schedule(value, schedBeats);
		});		
	}
	
	decorator_ {|func|
		decorator = func;
		this.pr_valueHasChanged();	
	}
	
	addRelation {|father|
		space.addRelation(father, this.key);
	}
	
	pr_receiveRelation {|son|
		relations.add(son);
	}
	
	clearRelations {
		space.clearRelationsFor(this.key);	
	}
	
	pr_removeRelationsFor {|son|
		relations.remove(son);
	}
	
	pr_valueHasChanged {	
		relations.do {|son|
			space.pr_callSon(son);	
		};
		
		this.changed(key, this.getValue());
	}
	
	mapToProxySpace {|lag=0.0, useLatency=true|
		var ps, node;
				
		space.mc.setProxySpace;
		
		ps = space.mc.proxySpace;
		
		node = ps.envir[key];
		node.isNil.if {
			node = NodeProxy.control(ps.server, 1);
			ps.envir.put(key, node);
		};
		
		node.put(0, {|value=0, lag=0| Lag2.kr(value, lag)}, 0, [\value, this.getValue().asFloat, \lag, lag]);
		this.addDependant({|changer, what, value| node.setGroup([\value, value.asFloat], useLatency) });
		nodeProxy = node;
		
		^node;
	}
}