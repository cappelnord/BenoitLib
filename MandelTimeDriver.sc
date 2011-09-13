/*
	MandelTimeDriver
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	MandelTimeDriver is the standard timing agent
	for MandelClock. Followers use this module to
	interpret Ticks and change the clock. Leaders
	use this to sync their followers.
	
*/

MandelTimeDriver : MandelModule {

	var <mc;
	
	var clockSerial = 0;

	var <>hardness = 0.5;
	var <>deviationMul = 0.3;
	var deviationGate = true;
	var <>latencyCompensation = 0.005; // in s
	var <>deviationThreshold = 0.02; // in beats
	
	var badTicks = 0;

	var <lastTickTime = 0;
	
	var <>listenToTicks = true;

	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	init {|maclock|
		mc = maclock;					
	}
	
	tick {
		mc.sendMsgCmd("/clock", clockSerial, mc.clock.beats, mc.tempo.asFloat);
		clockSerial = clockSerial + 1;
	}
	
	receiveTick {|ser, bea, tem, force=false|
				
		var deviation;
		var tempoHasChanged = false;
		var thisDeviationTreshold = deviationThreshold;
		var quant = mc.quant;
		
		// (ser + "\n" + bea + "\n" + tem + "\n").postln;
		
		
		force.if {
			mc.externalTempo = tem;	
		};
		
	
		// only interpret a tick if it's a new one.
		((ser > clockSerial) || force).if {
			MandelClock.debug.if {
				(((clockSerial + 1) != ser) && (force.not)).if {
					this.post("A tick was lost or too late!");
				};
			};
			
			// update internal state
			clockSerial = ser;
			lastTickTime = thisThread.seconds;
					
			listenToTicks.if {
				
				if(mc.externalTempo != tem) {
					mc.externalTempo = tem;
					tempoHasChanged = true;
				};
				

				// compensate network latency (stupid)
				bea = bea + (latencyCompensation * tem);
				
				// calculate the beat we want to snap on.
				deviation = mc.clock.beats - bea;
				
				// snap to next quant if necessary
				quant.notNil.if {
					(deviation.abs > (quant / 2)).if {
						// this may not work. brain damage!
						deviation = deviation - ((deviation / quant).floor * quant);
					};
				};
								
				// if the deviationGate is open it should be more difficult to close it again
				deviationGate.if {
					thisDeviationTreshold = thisDeviationTreshold / 4;
				};
								
				((deviation.abs > thisDeviationTreshold) || tempoHasChanged) .if ({
					
					MandelClock.debug.if {
						mc.post("Deviation: " ++ deviation);
					};
										
					// warning, crappy case syntax!
					case
					{ tempoHasChanged == true } {
						mc.pr_setClockTempo(mc.externalTempo);
					}
					// if five ticks were bad OR timing is really off
					{(badTicks > 5) || (deviation.abs > (deviationThreshold * 5))} {
						mc.pr_setClockTempo((mc.tempo * (1.0 - hardness)) + ( mc.externalTempo + (deviation * deviationMul * -1) * hardness));
					};
					
					deviationGate = true;
					badTicks = badTicks + 1;
					
				},{ // if our timing is good at the moment
					(mc.externalTempo != mc.tempo).if {
						mc.pr_setClockTempo(mc.externalTempo);
					};
					
					badTicks = 0;
					deviationGate = false;
				});
			};
		};
	}
	
	onStartup {|mc|
		
	}
	
	onBecomeLeader {|mc|
		lastTickTime = 0;
	}
	
	onBecomeFollower {|mc|
		badTicks = 0;
		lastTickTime = thisThread.seconds;
	}
}