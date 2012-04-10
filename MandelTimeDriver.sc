/*
	MandelTimeDriver
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	MandelTimeDriver is the default timing agent
	for MandelHub. Followers use this module to
	interpret Ticks and change the clock. Leaders
	use this to sync their followers.
	
*/

MandelTimeDriver : MandelModule {

	var <hub;
	
	var clockSerial = 0;

	var <>hardness = 0.5;
	var <>deviationMul = 0.3;
	var deviationGate = true;
	var <>latencyCompensation = 0.005; // in s
	var <>deviationThreshold = 0.02; // in beats
	
	var badTicks = 0;

	var <lastTickTime = 0;
	
	var <>listenToTicks = true;

	
	*new {|hub|
		^super.new.init(hub);	
	}
	
	init {|a_hub|
		hub = a_hub;					
	}
	
	tick {
		hub.net.sendMsgDirect("/clock", clockSerial, hub.clock.beats, hub.tempo.asFloat);
		clockSerial = clockSerial + 1;
	}
	
	receiveTick {|ser, bea, tem, force=false|
				
		var deviation;
		var tempoHasChanged = false;
		var thisDeviationTreshold = deviationThreshold;
		var quant = hub.quant;
		
		// (ser + "\n" + bea + "\n" + tem + "\n").postln;
		
		
		force.if {
			hub.externalTempo = tem;	
		};
		
	
		// only interpret a tick if it's a new one.
		((ser > clockSerial) || force).if {
			MandelHub.debug.if {
				(((clockSerial + 1) != ser) && (force.not)).if {
					this.post("A tick was lost or too late!");
				};
			};
			
			// update internal state
			clockSerial = ser;
			lastTickTime = thisThread.seconds;
					
			listenToTicks.if {
				
				if(hub.externalTempo != tem) {
					hub.externalTempo = tem;
					tempoHasChanged = true;
				};
				

				// compensate network latency (stupid)
				bea = bea + (latencyCompensation * tem);
				
				// calculate the beat we want to snap on.
				deviation = hub.clock.beats - bea;
				
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
					
					MandelHub.debug.if {
						hub.post("Deviation: " ++ deviation);
					};
										
					// warning, crappy case syntax!
					case
					{ tempoHasChanged == true } {
						hub.prSetClockTempo(hub.externalTempo);
					}
					// if five ticks were bad OR timing is really off
					{(badTicks > 5) || (deviation.abs > (deviationThreshold * 5))} {
						hub.prSetClockTempo((hub.tempo * (1.0 - hardness)) + ( hub.externalTempo + (deviation * deviationMul * -1) * hardness));
					};
					
					deviationGate = true;
					badTicks = badTicks + 1;
					
				},{ // if our timing is good at the moment
					(hub.externalTempo != hub.tempo).if {
						hub.prSetClockTempo(hub.externalTempo);
					};
					
					badTicks = 0;
					deviationGate = false;
				});
			};
		};
	}
	
	onStartup {|hub|
		
	}
	
	onBecomeLeader {|hub|
		lastTickTime = 0;
	}
	
	onBecomeFollower {|hub|
		badTicks = 0;
		lastTickTime = thisThread.seconds;
	}
}