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
	
	var metro;
		
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	init {|maclock|
		mc = maclock;	
		space = mc.space;
	}
	
	metro {|pan=0.0, quant=4|
		this.stopMetro;
		
		mc.server.waitForBoot({
			
			SynthDef(\mcTestClick, {|out=0, freq=440, pan=0, amp=0.4|
				var sig = SinOsc.ar(freq, phase:0.5pi);
				sig = sig * EnvGen.ar(Env.perc(0.000001,0.1), doneAction:2);
				
				OffsetOut.ar(out, Pan2.ar(sig, pan) * amp);
			}).add;
		
			metro = Pbind(\instrument, \mcTestClick, \dur, 1, \octave, 6, \pan, pan, \degree, Pseq([7,Pn(0,quant-1)],inf)).play(mc.clock, quant:quant);
		});
	}
	
	impulseMetro {
		this.stopMetro;
		
		mc.server.waitForBoot({
			SynthDef(\mcTestImpulse, {|out=0, amp=1|
				var sig = Impulse.ar(0).dup;
				var remove = Line.kr(0,1,0.1, doneAction:2);
				
				OffsetOut.ar(out, sig);
			}).add;
			
			metro = Pbind(\instrument, \mcTestImpulse, \dur, 1, \amp, 1).play(mc.clock);
		});	
	}
	
	stopMetro {
		metro.stop;
	}
}