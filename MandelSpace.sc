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
	
	setValue {|key, value, schedBeats, strategy=\stream|
		var obj;
		key = key.asSymbol;
		
		key.isNil.if {
			("Invalid key: " ++ key).error;
			^nil;	
		};
		
		obj = this.getObject(key);
		obj.setValue(value, schedBeats, strategy:strategy);
	}
	
	sendValue {|key, value, schedBeats=0.0, strategy=\stream|
		var delta = schedBeats - mc.clock.beats;
		var burstNum = 2;
		
		(delta < 0.25).if ({
			(strategy == \stream).if {
				mc.net.sendMsgCmd("/value", key.asString, this.serialize(value), schedBeats.asFloat);
			};
			[\critital, \timeCritical, \important].includes(strategy).if {
				mc.net.sendMsgBurst("/value", strategy, key.asString, this.serialize(value), schedBeats.asFloat);
			};
		}, {
			// burst, if there is time.
			(delta > 8).if {delta = 8};
			delta = delta * 0.75;
			burstNum = burstNum + delta.round;
			mc.net.sendMsgBurst("/value", [burstNum, delta], key.asString, this.serialize(value), schedBeats.asFloat);
		});
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
		mc.net.addOSCResponder(\general, "/value", {|header, payload|
			this.getObject(payload[0].asSymbol).setValue(this.deserialize(payload[1]), payload[2].asFloat, header.name, doSend:false);
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
}

MandelValue {
	var <key, <>bdl, <decorator, <relations, quant;
	var space;
	var <bus, busDependant;
	var <>sourcePullInterval = 0.05;
	var sourceRoutine;
	
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
	
	setValue {|value, schedBeats, who, doSend=true, strategy=\stream|
		who = who ? space.mc.name;
				
		schedBeats.isNil.if {
			this.quant.isNil.if ({
				schedBeats = 0.0;	
			}, {
				schedBeats = this.quant.nextTimeOnGrid(space.mc.clock);
			});
		};
		doSend.if {space.sendValue(key, value, schedBeats, strategy)};
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
		
		this.unmap(\source);
		
		source.isNil.if ({
			^this; // early exit - just disconnect
		});
		
		bus.isNil.if ({
			"Could not set source - not compatible to Bus!".warn;
		}, {
			var stream = Pkr(bus).asStream;
			sourceRoutine = {
				var lastVal = nil;
				while({true}, {
					var val = stream.next;
					if(val != lastVal) {
						this.setValue(val);
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
}