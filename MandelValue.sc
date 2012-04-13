/*
	MandelValue
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	A value instance for MandelSpace
	
*/

MandelValue : AbstractFunction {
	var <key, <>bdl, <decorator, quant;
	var space;
	var <bus, busDependant;
	var <>sourcePullInterval = 0.05;
	var sourceRoutine;
	var <>doHeal = true;
	var lastUpdateBeats;
	
	*new {|space, key|
		^super.new.init(space, key);
	}
	
	init {|aspace, akey|
		space = aspace;
		key = akey.asSymbol;		
	}
	
	value {
		^this.getValue(); // why?
	}
	
	valueArray {|array|
		^this.getValue(); // why?
	}
	
	asStream {
		^Routine({
			while {true} {
				this.getValue().yield;
			};
		});
	}
	
	asBus {
		^bus.isNil.if({this.prCreateBus}, {bus});
	}
	
	asBusPlug {
		^BusPlug.for(this.asBus)	;
	}
	
	prCreateBus {
		this.freeBus;
		space.hub.server.serverRunning.if ({
			bus = Bus.control(space.hub.server, 1);
			bus.set(this.getValue());
		
			busDependant = {|changed, what, value| bus.set(value)};
			this.addDependant(busDependant);
		}, {
			"Server is not running! You have to re-evaluate.".warn;
			// dummy Bus to fail silently
			^Bus.control(space.hub.server, 1);
		});
		
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
	
	ir {
		^this.getValue();	
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
		who = who ? space.hub.name;
				
		schedBeats.isNil.if {
			this.quant.isNil.if ({
				schedBeats = 0.0;	
			}, {
				schedBeats = this.quant.nextTimeOnGrid(space.hub.clock);
			});
		};
		doSend.if {space.sendValue([key, value], schedBeats, strategy)};
		^this.prSetBDL(value, schedBeats, who);	
	}
	
	tryHealValue {|value, schedBeats, who|
		((bdl.setAtBeat < schedBeats) && doHeal).if {
			("MandelSpace Value " ++ key.asString ++ " got healed!").postln;
			this.setValue(value,  schedBeats, who, doSend: false);
		}
	}
	
	// maybe remove this, move to setValue
	prSetBDL {|value, schedBeats, who|		
		bdl.isNil.if ({
			bdl = MandelBDL(value, who, schedBeats);
			bdl.onChangeFunc = {this.update;};
			^value;	
		}, {
			^bdl.schedule(value, schedBeats, who);
		});		
	}
	
	decorator_ {|func|
		decorator = func;
		try {this.update;};
	}
	
	update {|theChanger, what ... moreArgs|
		if(lastUpdateBeats != space.hub.clock.beats) {
			space.prScheduleUpdate(key, {this.changed(\value, this.getValue())});
			lastUpdateBeats = space.hub.clock.beats;
		};
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
		^(doHeal && (bdl.setBy == space.hub.name) && ((bdl.setAtBeat) + 4 < space.hub.clock.beats) && (bdl.setAtBeat > 0));
	}
	
	freeRessources {
		this.freeBus;	
	}
}