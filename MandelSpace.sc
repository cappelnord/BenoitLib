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
	
	var <quant;
	
	
	classvar defaultDictInstance;
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	*getValueOrDefault {|key|
		MandelClock.instance.notNil.if {
			^MandelClock.instance.space.getValue(key);
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
	
	asDict {
		var dict = Dictionary.new;
		
		objects.do {|obj|
			dict.put(obj.key, obj.getValue());	
		};
		
		^dict;
	}
	
	// TODO: prettify
	dumpValues {
		"Dumping all MandelSpace Values: ".postln;
		objects.do {|obj|
			("\\" ++ obj.key ++ ": 	").post;
			obj.getValue.asCompileString.post;
			obj.decorator.isNil.not.if ({
				obj.getValue(false).isNil.if({
					" (Synthesized)".post;
				},{
					(" (Raw: " ++ obj.getValue(false).asCompileString ++ ")").post;
				});
			}, {
				(" (Set by: " ++ obj.setBy.asString ++ ")").post;
			});
			"\n".post;
		};
		"\n".post;
	}
	
	quant_ {|val|
		quant = val.asQuant;	
	}
	
	init {|maclock|
		mc = maclock;	
					
		objects = Dictionary.new;
		
		this.pr_buildEvents();
		this.pr_setDefaults();
	}
	
	pr_setDefaults {
		MandelSpace.defaultDict.keys.do {|key|
			this.getObject(key).setValue(MandelSpace.defaultDict.at(key), 0, doSend:false);
		}
	}
	
	pr_buildEvents {
		Event.addEventType(\mandelspace, {
			var schedBeats;
			~deltaSched = ~deltaSched ? 0.0;
			schedBeats = mc.clock.beats + ~deltaSched;
			
			(schedBeats <= mc.clock.beats).if {
				schedBeats = 0.0; // no scheduling
			};
			
			currentEnvironment.keys.do {|key|
				((key != \type) && (key != \dur) && (key != \removeFromCleanup)).if {
					this.setValue(key, currentEnvironment.at(key), schedBeats);
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
		^obj.getValue(useDecorator);
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
		^this.getObject(key);	
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
		value.isNil.if {^(serializationPrefix ++ "NL")};
		
		value.isFunction.if {
			value.isClosed.if({
				^(serializationPrefix ++ "CS" ++ value.asCompileString);
			},{
				Error("Only closed functions can be sent through MandelSpace").throw;	
			});
		};
		
		"There is no explicit rule to send the object through MandelSpace - trying asCompileString".warn;
		("Compile String: " ++ value.asCompileString).postln;
		// ("Key: " ++ key).postln;
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
				serType = value[pfl..pfl+1];
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
				
				(serType == "NL").if {
					^nil;
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
			{
			0.1.wait;
			objects.keys.do {|key|
				var value = objects.at(key).bdl;
				this.sendValue(key, value.value(), 0.0);
				0.01.wait;
				value.list.do {|item|
					this.sendValue(key, item[1], item[0]);
					0.01.wait;
				};
			};
			}.fork; // delay a little bit and add wait times
		});	
	}
	
	onStartup {|mc|
		mc.addResponder(\general, "/value", {|ti, tR, message, addr|
			(message[1].asString != mc.name).if {
				this.getObject(message[2].asSymbol).setValue(this.deserialize(message[3]), message[4].asFloat, message[1].asString, doSend:false);
			};
		});
		
		mc.leading.not.if {
			mc.sendMsgCmd("/requestValueSync"); // request MandelSpace sync from the leader
		}
	}
	
	envir {
		envirInstance.isNil.if {
			envirInstance = MandelEnvironment(this);
		};
		^envirInstance;	
	}
}

MandelValue  {
	var <key, <>bdl, <decorator, <relations, quant;
	var space;
	
	var <bus, busDependant;
	
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
	
	// TODO: Reset Busses
	asBus {
		^bus.isNil.if({this.pr_createBus}, {bus});
	}
	
	pr_createBusÊ{
		this.removeDependant(busDependant);
		
		bus = Bus.control(space.mc.server, 1);
		bus.set(this.getValue());
		
		busDependant = {|changed, what, value| bus.set(value)};
		this.addDependant(busDependant);
		
		^bus;
	}
	
	prepareForProxySynthDef {|proxy|
		^this.asBus.prepareForProxySynthDef(proxy);
	}
	
	kr {
		^this.asBus.kr;	
	}
	
	ar {
		^{K2A.ar(this.asBus.kr)};
	}
	
	tr {
		^{InTrig.kr(this.asBus)};	
	}
	
	quant {
		quant.isNil.not.if({
			^quant
		}, {
			^space.quant;
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
	
	setBy {
		bdl.value(); // oh no!
		^bdl.setBy;	
	}
	
	setValue {|value, schedBeats, who, doSend=true|
		who = who ? space.mc.name;
				
		schedBeats.isNil.if {
			this.quant.isNil.if ({
				schedBeats = 0.0;	
			}, {
				schedBeats = this.quant.nextTimeOnGrid(space.mc.clock);
			});
		};
		doSend.if {space.sendValue(key, value, schedBeats)};
		^this.pr_setBDL(value, schedBeats, who);	
	}
	
	// maybe remove this, move to setValue
	pr_setBDL {|value, schedBeats, who|		
		bdl.isNil.if ({
			bdl = BeatDependentValue(value, who);
			bdl.onChangeFunc = {this.pr_valueHasChanged;};
			^value;	
		}, {
			^bdl.schedule(value, schedBeats, who);
		});		
	}
	
	decorator_ {|func|
		decorator = func;
		this.pr_valueHasChanged();	
	}
	
	addRelation {|father|
		space.addRelation(father, key);
	}
	
	pr_receiveRelation {|son|
		relations.add(son);
	}
	
	clearRelations {
		space.clearRelationsFor(key);	
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
}