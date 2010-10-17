/*
	Phold, Pholds, Pholda, Pholdas
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Like Pstutter but holds a value for a specified
	duration in beats or seconds.
	
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
				
				(thisEvent.isNil ||ÊthisDur.isNil).if {
					^event;
				};
				nextPoll = this.pr_newTimeInstance + thisDur.abs;
				doPoll = false;
			};			
			
			// allow undersampling in a versions
			(this.pr_curTimeInstance >= nextPoll).not.if {
				event = thisEvent.copy.yield;
			};
						
			(this.pr_curTimeInstance >= nextPoll).if {
				doPoll = true;	
			};
		};		
	}
	
	pr_newTimeInstance {
		^this.pr_curTimeInstance;	
	}
	
	pr_curTimeInstance {
		^thisThread.seconds;
	}
}

Pholdas : Pholds {
	
	pr_newTimeInstance {
		^this.nextPoll ? this.pr_curTimeInstance;
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

	pr_curTimeInstance {
		^clock.beats;
	}
	
}

Pholda : Phold {

	pr_newTimeInstance {
		^this.nextPoll ? this.pr_curTimeInstance;	
	}
	
}