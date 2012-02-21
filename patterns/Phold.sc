/*
	Phold, Pholds, Pholda, Pholdas
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	A time based sample-and-hold pattern.
	
*/

// based on Pstutter
Pholds : FilterPattern {
	var <>dur;
	var <>nextPoll;
	
	*new {|dur, pattern|
		^super.new(pattern).dur_(dur);
	}
	
	storeArgs { 
		^[dur,pattern];
	}
	
	embedInStream {|event|
		
		var stream = pattern.asStream;
		var durStream = dur.asStream;
		var thisEvent, thisDur;
		
		var doPoll = true;		
		
		while {true} {
			
			// first values
			doPoll.if {
				thisEvent = stream.next(event);
				thisDur = durStream.next(event);
				
				(thisEvent.isNil || thisDur.isNil).if {
					^event;
				};
				nextPoll = this.prNewTimeInstance + thisDur.abs;
				doPoll = false;
			};			
			
			// allow undersampling in a versions
			(this.prCurTimeInstance >= nextPoll).not.if {
				event = thisEvent.copy.yield;
			};
						
			(this.prCurTimeInstance >= nextPoll).if {
				doPoll = true;	
			};
		};		
	}
	
	prNewTimeInstance {
		^this.prCurTimeInstance;	
	}
	
	prCurTimeInstance {
		^thisThread.seconds;
	}
}

Pholdas : Pholds {
	
	prNewTimeInstance {
		^this.nextPoll ? this.prCurTimeInstance;
	}
}

Phold : Pholds {
	
	var <>clock;
	
	*new {|dur, pattern, clock|
		^super.new(dur,pattern).clock_(clock ? TempoClock.default);
	}
	
	storeArgs { 
		// I think clock is not relevant
		^[dur,pattern];
	}

	prCurTimeInstance {
		^clock.beats;
	}
}

Pholda : Phold {

	prNewTimeInstance {
		^this.nextPoll ? this.prCurTimeInstance;	
	}
}