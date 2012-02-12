/*
	MandelTools
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Contains different convenience tools.
	
*/

MandelTools : MandelModule {
	
	var mc;
	var space;
		
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	init {|maclock|
		mc = maclock;	
		space = mc.space;
	}
	
	createFreqValues {|lag=0.0|
		var scale = PmanScale().asStream;
		var relations = [\scale, \tuning, \root, \stepsPerOctave, \octaveRatio];
		var stdValues = [\root, \stepsPerOctave, \octaveRatio];
		
		var rootFreq = space.getObject(\rootFreq);
		var mtransposeFreq = space.getObject(\mtransposeFreq);
		var ctransposeFreq = space.getObject(\ctransposeFreq);
		
		var stdEvent = {
			var ev = Event.partialEvents[\pitchEvent].copy;
			stdValues.do {|item|
				ev[item] = space.getValue(item);
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
			ev[\mtranspose] = 	space[\mtranspose];
			ev.use {~freq.value()};
		};
		
		ctransposeFreq.decorator = {
			var ev = stdEvent.value();
			ev[\ctranspose] = 	space[\ctranspose];
			ev.use {~freq.value()};
		};
	}
}