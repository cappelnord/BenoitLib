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
	
	var hub;
	var space;
	
	var metro;
	
	var genresInstance;
		
	
	*new {|hub|
		^super.new.init(hub);	
	}
	
	init {|a_hub|
		hub = a_hub;	
		space = hub.space;
	}
	
	metro {|pan=0.0, quant=4|
		this.stopMetro;
		
		hub.server.waitForBoot({
			
			SynthDef(\mhTestClick, {|out=0, freq=440, pan=0, amp=0.4|
				var sig = SinOsc.ar(freq, phase:0.5pi);
				sig = sig * EnvGen.ar(Env.perc(0.000001,0.1), doneAction:2);
				
				OffsetOut.ar(out, Pan2.ar(sig, pan) * amp);
			}).add;
		
			metro = Pbind(\instrument, \mhTestClick, \dur, 1, \octave, 6, \pan, pan, \degree, Pseq([7,Pn(0,quant-1)],inf)).play(hub.clock, quant:quant);
		});
	}
	
	impulseMetro {
		this.stopMetro;
		
		hub.server.waitForBoot({
			SynthDef(\mhTestImpulse, {|out=0, amp=1|
				var sig = Impulse.ar(0).dup;
				var remove = Line.kr(0,1,0.1, doneAction:2);
				
				OffsetOut.ar(out, sig);
			}).add;
			
			metro = Pbind(\instrument, \mhTestImpulse, \dur, 1, \amp, 1).play(hub.clock);
		});	
	}
	
	genres {
		var lines;
		genresInstance.isNil.if {
			lines = File(hub.classPath("data/ID3v1Genres.txt"), "r").readAllString.split($\n);
			genresInstance = lines.collect {|line| line.replace(" - ", "|").split($|)[1]};
		};
		^genresInstance;	
	}
	
	stopMetro {
		metro.stop;
	}
}