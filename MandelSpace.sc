/*
	MandelSpace
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Shared Variable Space used by MandelHub
	
*/

MandelSpace : MandelModule {
	
	var <objects;
	var <hub;
	
	var <>allowRemoteCode = false;
	
	var envirInstance;
		
	var <quant;
	
	classvar defaultDictInstance;
	
	var healSJ;
	
	var updateSink = nil;
	
	*new {|hub|
		^super.new.init(hub);	
	}
	
	*getValueOrDefault {|key|
		MandelHub.instance.notNil.if {
			^MandelHub.instance.space.getValue(key);
		} {
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
	
	init {|a_hub|
		hub = a_hub;	
					
		objects = Dictionary.new;
		
		this.prBuildEvents();
		this.prSetDefaults();
		this.prCreateFreqValues();
		
		this.prStartHealSJ;

	}
	
	prStartHealSJ {
		// all keys that can be healed
		var keyStreamFunc = {
			var keys = objects.values.select({|item|item.canHeal}).collect({|item|item.key});
			(keys.size > 0).if({
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
	
	prSetDefaults {
		MandelSpace.defaultDict.keys.do {|key|
			this.getObject(key).setValue(MandelSpace.defaultDict.at(key), -1, doSend:false);
		}
	}
	
	prBuildEvents {
		var degreeDict = (
			\i: 0,
			\ii: 2,
			\iii: 4,
			\iv: 5,
			\v: 7,
			\vi: 9,
			\vii: 11	
		);
		var decodeHarmony = {|x|
			var xs = x.asString;
			var firstChar = xs[0];
			var scale = \minor;
			var ct = 0;
			firstChar.isUpper.if({scale = \major;});
	
			((firstChar == $m) || (firstChar == $M)).if({
				ct = xs[1..].asInteger;
			}, {
				var acc = 0;
				var lastChar = xs[xs.size - 1];
				(lastChar == $s).if {acc = 1;};
				(lastChar == $b).if {acc = -1;};
				(acc != 0).if {xs = xs[0..xs.size-2];};
				ct = degreeDict[xs.toLower.asSymbol] + acc;
			});
			[ct, scale];
		};
		
		Event.addEventType(\mandelspace, {
			var schedBeats;
			var strategy = \time;
			var list = List.new;
			~deltaSched = ~deltaSched ? 0.0;
			schedBeats = hub.clock.beats + ~deltaSched;
			
			(schedBeats <= hub.clock.beats).if {
				schedBeats = 0.0; // no scheduling
			};
			
			// stream values if duration is short
			((~dur <= 0.1) && (schedBeats == 0.0)).if {
				strategy = \stream;	
			};

			if(currentEnvironment[\harmony] != nil) {
				var rs = decodeHarmony.value(currentEnvironment[\harmony]);
				((rs[0] != nil) && (rs[1] != nil)).if ({
					currentEnvironment[\ctranspose] = rs[0];
					currentEnvironment[\scale] = rs[1];
				}, {
					("Could not decode harmony: " ++ currentEnvironment[\harmony]).error;
				});
			};
			
			currentEnvironment.keys.do {|key|
				#[\type, \dur, \removeFromCleanup, \deltaSched, \harmony].includes(key).not.if {
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
		
		key.isNil.if {
			("Invalid key: " ++ key).error;
			^nil;	
		};
		
		key = key.asSymbol;
		
		obj = this.getObject(key);
		obj.setValue(value, schedBeats, strategy:strategy);
	}
	
	setValueList {|keyValueList, schedBeats, strategy=\time|
		var cleanList = List.new;
		
		this.prStartUpdateSink;
		(0,2..(keyValueList.size-1)).do {|i|
			var key = keyValueList[i];
			var value = keyValueList[i+1];
				
			key.isNil.if ({/* NOOP */}, {
				key = key.asSymbol;
				this.getObject(key).setValue(value, schedBeats, doSend: false);
				cleanList.add(key);
				cleanList.add(value);
			});
		};		
		this.prFinishUpdateSink;
		
		this.sendValue(cleanList, schedBeats, strategy);
	}
	
	sendValue {|keyValueList, schedBeats=0.0, strategy=\time|
		var delta = schedBeats - hub.clock.beats;
		var burstNum = 2;
		var origList = keyValueList;
		var oi = 0;
		var serSlots;
		keyValueList = Array.newClear(origList.size / 2 * 3);
		
		// encode data
		(0,3..(keyValueList.size-1)).do {|i|
			keyValueList[i] = origList[oi].asString;
			serSlots = this.serialize(origList[oi+1]);
			keyValueList[i+1] = serSlots[0];
			keyValueList[i+2] = serSlots[1];
			oi = oi + 2;
		};
		
		schedBeats = schedBeats.asFloat;
		
		(delta < 0.25).if ({
			(strategy == \stream).if {
				hub.net.sendMsgCmd("/value", schedBeats, *keyValueList);
			};
			#[\time, \critital, \timeCritical, \important, \relaxed].includes(strategy).if {
				hub.net.sendMsgBurst("/value", strategy, schedBeats, *keyValueList);
			};
		}, {
			// burst, if there is time.
			(delta > 8).if {delta = 8};
			delta = delta * 0.75;
			burstNum = burstNum + delta.round;
			hub.net.sendMsgBurst("/value", [burstNum, delta], schedBeats, *keyValueList);
		});
	}
	
	sendHealValue {|key|
		var obj = this.getObject(key);
		var serSlots;
		obj.canHeal.if {
			serSlots = this.serialize(obj.value);
			hub.net.sendMsgBurst("/healValue", \relaxed, obj.bdl.setAtBeat.asFloat, key.asString, serSlots[0], serSlots[1]);
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
		value.isInteger.if {^[0, value]};
		value.isFloat.if {^[0, value]};
		value.isString.if {^[0, value]};
		value.isKindOf(Symbol).if {^["SM", value.asString]};
		value.isNil.if {^["NL", ""]};
		
		value.isFunction.if {
			value.isClosed.if({
				^["CS", value.asCompileString];
			},{
				Error("Only closed functions can be sent through MandelSpace").throw;	
			});
		};
		
		"There is no explicit rule to send the object through MandelSpace - trying asCompileString".warn;
		("Compile String: " ++ value.asCompileString).postln;
		// ("Key: " ++ key).postln;
		^["CS", value.asCompileString];
	}
	
	// build object if object was serialized
	deserialize {|serType, value|
		var payload;
		value.isNumber.if {^value};
		(serType == \NL).if {^nil;};
		
		value.isKindOf(Symbol).if {
			((serType == 0) || (serType == '')).if {^value.asString;};
			(serType == \SM).if {^value.asSymbol;};
			(serType == \CS).if {
				allowRemoteCode.if({
					^value.asString.interpret;
				}, {
					"MandelSpace received remote code but wasn't allowed to execute.\nSet allowRemoteCode to true if you know what you're doing!".warn;
				});	
			};	
		};
		
		// if everything else fails ...
		^value.asString;
	}
	
	onSyncRequest {|hub|
		{
			0.1.wait;
			objects.keys.do {|key|
				var value = objects.at(key).bdl;
				value.notNil.if {
					(value.setAtBeat > 0).if {
						this.sendValue([key, value.value()], 0.0);
					};
					0.01.wait;
					value.list.do {|item|
						this.sendValue([key, item[1]], item[0]);
						0.01.wait;
					};
				};
			};
		}.fork; // delay a little bit and add wait time
	}
	
	onStartup {|hub|
		hub.net.addOSCResponder(\general, "/value", {|header, payload|
			var name = header.name;
			var schedBeats = payload[0].asFloat;
			
			this.prStartUpdateSink;
			(1,4..(payload.size-1)).do {|i|
				var key = payload[i].asSymbol;
				var value = this.deserialize(payload[i+1], payload[i+2]);
				// ("Key: " ++ key).postln;
				// ("Value: " ++ value).postln; 
				this.getObject(key).setValue(value, schedBeats, header.name, doSend:false);
			};
			this.prFinishUpdateSink;
			
		}, \dropOwn);
		
		hub.net.addOSCResponder(\general, "/healValue", {|header, payload|
			var schedBeats = payload[0].asFloat;
			var key = payload[1].asSymbol;
			var value = this.deserialize(payload[2], payload[3]);
			this.getObject(key).tryHealValue(value, schedBeats, header.name);
		}, \dropOwn);
	}
	
	envir {
		envirInstance.isNil.if {
			envirInstance = MandelEnvironment(this);
		};
		^envirInstance;	
	}
	
	prCreateFreqValues {|lag=0.0|
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
			var obj = this.getObject(item);
			obj.addDependant(rootFreq);
		};
		
		rootFreq.addDependant(mtransposeFreq);
		rootFreq.addDependant(ctransposeFreq);
		
		this.getObject(\mtranspose).addDependant(mtransposeFreq);
		this.getObject(\ctranspose).addDependant(ctransposeFreq);
		
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
	
	freeAllRessources {
		objects.do {|item|
			item.freeRessources;
		};
	}
	
	onClear {|hub|
		this.freeAllRessources;
		healSJ.stop;
	}
	
	keys {
		^objects.keys;	
	}
	
	prStartUpdateSink {
		updateSink = IdentityDictionary();	
	}
	
	prScheduleUpdate {|key, func|
		updateSink.isNil.if({
			func.value();
		}, {
			updateSink[key].isNil.if {
				updateSink[key] = func;	
			};
		});
	}
	
	prFinishUpdateSink {
		var oldSink = updateSink;
		updateSink = IdentityDictionary.new;
		
		oldSink.keys.do {|key|
			updateSink[key] = 0;	
		};
		
		oldSink.values.do {|item|
			item.value();	
		};
		
		(updateSink.size > oldSink.size).if {
			this.prFinishUpdateSink;	
		};
				
		updateSink = nil;	
	}
}