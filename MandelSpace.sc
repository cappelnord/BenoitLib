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
	
	var healSJ;
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	*getValueOrDefault {|key|
		MandelClock.instance.notNil.if {
			^MandelClock.instance.space.getValue(key);
		} {
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
		this.pr_createFreqValues();
		
		this.pr_startHealSJ;

	}
	
	pr_startHealSJ {
		// all keys that can be healed
		var keyStreamFunc = {
			var keys = objects.values.select({|item|item.canHeal}).collect({|item|item.key});
			keys.isNil.not.if({
				Pseq(keys, 1).asStream;
			}, {
				nil;
			});
		};
		
		var keyStream = keyStreamFunc.value;
		
		healSJ = SkipJack({
			var nextKey = keyStream.next;
			nextKey.isNil.if({
				keyStream = keyStreamFunc.value;
			}, {
				this.sendHealValue(nextKey);
			});
		},3.5, name: "MandelSpaceHealing");	
	}
	
	pr_setDefaults {
		MandelSpace.defaultDict.keys.do {|key|
			this.getObject(key).setValue(MandelSpace.defaultDict.at(key), 0, doSend:false);
		}
	}
	
	pr_buildEvents {
		Event.addEventType(\mandelspace, {
			var schedBeats;
			var strategy = \time;
			var list = List.new;
			~deltaSched = ~deltaSched ? 0.0;
			schedBeats = mc.clock.beats + ~deltaSched;
			
			(schedBeats <= mc.clock.beats).if {
				schedBeats = 0.0; // no scheduling
			};
			
			// stream values if duration is short
			((~dur <= 0.1) && (schedBeats == 0.0)).if {
				strategy = \stream;	
			};
			
			currentEnvironment.keys.do {|key|
				#[\type, \dur, \removeFromCleanup, \deltaSched].includes(key).not.if {
					list.add(key);
					list.add(currentEnvironment.at(key));
				};
			};
			
			(list.size >= 2).if {
				this.setValueList(list, schedBeats, strategy:strategy);
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
	
	setValue {|key, value, schedBeats, strategy=\time|
		var obj;
		key = key.asSymbol;
		
		key.isNil.if {
			("Invalid key: " ++ key).error;
			^nil;	
		};
		
		obj = this.getObject(key);
		obj.setValue(value, schedBeats, strategy:strategy);
	}
	
	setValueList {|keyValueList, schedBeats, strategy=\time|
		(0,2..(keyValueList.size-1)).do {|i|
			var key = keyValueList[i].asSymbol;
			var value = keyValueList[i+1];
				
			key.isNil.if ({
				("Invalid key: " ++ key).error;
				^nil;
			}, {
				this.getObject(key).setValue(value, schedBeats, doSend: false);
			});
		};
		
		this.sendValue(keyValueList, schedBeats, strategy);
	}
	
	sendValue {|keyValueList, schedBeats=0.0, strategy=\time|
		var delta = schedBeats - mc.clock.beats;
		var burstNum = 2;
		
		// encode data
		(0,2..(keyValueList.size-1)).do {|i|
			keyValueList[i] = keyValueList[i].asString;
			keyValueList[i+1] = this.serialize(keyValueList[i+1]);
		};
		schedBeats = schedBeats.asFloat;
		
		(delta < 0.25).if ({
			(strategy == \stream).if {
				mc.net.sendMsgCmd("/value", schedBeats, *keyValueList);
			};
			#[\time, \critital, \timeCritical, \important, \relaxed].includes(strategy).if {
				mc.net.sendMsgBurst("/value", strategy, schedBeats, *keyValueList);
			};
		}, {
			// burst, if there is time.
			(delta > 8).if {delta = 8};
			delta = delta * 0.75;
			burstNum = burstNum + delta.round;
			mc.net.sendMsgBurst("/value", [burstNum, delta], schedBeats, *keyValueList);
		});
	}
	
	sendHealValue {|key|
		var obj = this.getObject(key);
		obj.canHeal.if {
			mc.net.sendMsgBurst("/healValue", \relaxed, obj.bdl.setAtBeat.asFloat, key.asString, this.serialize(obj.value));
		};
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
		mc.net.addOSCResponder(\leader, "/requestValueSync", {|header, payload|
			{
			0.1.wait;
			objects.keys.do {|key|
				var value = objects.at(key).bdl;
				value.notNil.if {
					this.sendValue([key, value.value()], 0.0);
					0.01.wait;
					value.list.do {|item|
						this.sendValue([key, item[1]], item[0]);
						0.01.wait;
					};
				};
			};
			}.fork; // delay a little bit and add wait times
		}, \dropOwn);	
	}
	
	onStartup {|mc|
		mc.net.addOSCResponder(\general, "/value", {|header, payload|
			var name = header.name;
			var schedBeats = payload[0].asFloat;
			
			(1,3..(payload.size-1)).do {|i|
				var key = payload[i].asSymbol;
				var value = this.deserialize(payload[i+1]);			this.getObject(key).setValue(value, schedBeats, header.name, doSend:false);
			};
		}, \dropOwn);
		
		mc.net.addOSCResponder(\general, "/healValue", {|header, payload|
			var schedBeats = payload[0].asFloat;
			var key = payload[1].asSymbol;
			var value = this.deserialize(payload[2]);
			
			this.getObject(key).tryHealValue(value, schedBeats, header.name);
		}, \dropOwn);
		
		mc.leading.not.if {
			mc.net.sendMsgCmd("/requestValueSync"); // request MandelSpace sync from the leader
		}
	}
	
	envir {
		envirInstance.isNil.if {
			envirInstance = MandelEnvironment(this);
		};
		^envirInstance;	
	}
	
	pr_createFreqValues {|lag=0.0|
		var scale = PmanScale().asStream;
		var relations = [\scale, \tuning, \root, \stepsPerOctave, \octaveRatio];
		var stdValues = [\root, \stepsPerOctave, \octaveRatio];
		
		var rootFreq = this.getObject(\rootFreq);
		var mtransposeFreq = this.getObject(\mtransposeFreq);
		var ctransposeFreq = this.getObject(\ctransposeFreq);
		
		var stdEvent = {
			var ev = Event.partialEvents[\pitchEvent].copy;
			stdValues.do {|item|
				ev[item] = this.getValue(item);
			};
			ev[\scale] = scale.next;
			ev;
		};

		relations.do {|item|
			rootFreq.addRelation(item);
			mtransposeFreq.addRelation(item);
			ctransposeFreq.addRelation(item);
		};
		
		mtransposeFreq.addRelation(\mtranspose);
		ctransposeFreq.addRelation(\ctranspose);
		
		rootFreq.decorator = {
			var ev = stdEvent.value();
			ev.use {~freq.value()};
		};
		
		mtransposeFreq.decorator = {
			var ev = stdEvent.value();
			ev[\mtranspose] = 	this.getValue(\mtranspose);
			ev.use {~freq.value()};
		};
		
		ctransposeFreq.decorator = {
			var ev = stdEvent.value();
			ev[\ctranspose] = 	this.getValue(\ctranspose);
			ev.use {~freq.value()};
		};
	}
	
	freeAllBuses {
		objects.do {|item|
			item.clearBus;
		};
	}
	
	onClear {|mc|
		this.freeAllBuses;
		healSJ.stop;
	}
}

MandelValue {
	var <key, <>bdl, <decorator, <relations, quant;
	var space;
	var <bus, busDependant;
	var <>sourcePullInterval = 0.05;
	var sourceRoutine;
	var <>doHeal = true;
	
	*new {|space, key|
		^super.new.init(space, key);
	}
	
	init {|aspace, akey|
		space = aspace;
		key = akey.asSymbol;
		
		relations = IdentitySet();
	}
	
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
	
	asBus {
		^bus.isNil.if({this.pr_createBus}, {bus});
	}
	
	asBusPlug {
		^BusPlug.for(this.asBus)	;
	}
	
	pr_createBus {
		this.freeBus;
		
		bus = Bus.control(space.mc.server, 1);
		bus.set(this.getValue());
		
		busDependant = {|changed, what, value| bus.set(value)};
		this.addDependant(busDependant);
		
		^bus;
	}
	
	freeBus {
		this.removeDependant(busDependant);
		bus.free;
		bus = nil;			
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
	
	setValue {|value, schedBeats, who, doSend=true, strategy=\time|
		who = who ? space.mc.name;
				
		schedBeats.isNil.if {
			this.quant.isNil.if ({
				schedBeats = 0.0;	
			}, {
				schedBeats = this.quant.nextTimeOnGrid(space.mc.clock);
			});
		};
		doSend.if {space.sendValue([key, value], schedBeats, strategy)};
		^this.pr_setBDL(value, schedBeats, who);	
	}
	
	tryHealValue {|value, schedBeats, who|
		((bdl.setAtBeat < schedBeats) && doHeal).if {
			("MandelSpace Value " ++ key.asString ++ " got healed!").postln;
			this.setValue(value,  schedBeats, who, doSend: false);
		}
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
	
	<>> {|proxy, key=\in|
		proxy.isNil.if {^proxy};
		
		// if the Proxy doesn't allready has a bus we map through assignment
		proxy.bus.isNil.if {
			proxy.source = this;
			^proxy;	
		};
		// otherwise map
		^proxy.map(key, this.asBusPlug);
		
	}
	
	<<> {|source, key=\in|
		var bus = try {source.asBus};
		var stream, val;
		
		this.unmap(\source);
		
		source.isNil.if ({
			^this; // early exit - just disconnect
		});
		
		bus.isNil.if ({
			"Could not set source - not compatible to Bus!".warn;
		}, {
			stream = Pkr(bus).asStream;
			sourceRoutine = {
				var lastVal = nil;
				while({true}, {
					val = stream.next;
					if(val != lastVal) {
						this.setValue(val, strategy:\stream);
						lastVal = val;
					};
					sourcePullInterval.wait;
				});
			}.fork;
		});
		^this;
	}
	
	unmap {|key|
		// (key == \source).if {
		sourceRoutine.stop;
		sourceRoutine = nil;
		// };	
	}
	
	canHeal {
		bdl.isNil.if {^false};
		^(doHeal && (bdl.setBy == space.mc.name));
	}
}