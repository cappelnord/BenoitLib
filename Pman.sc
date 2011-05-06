/*
	Pman
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	A stream of shared MandelClock values.
	
*/

Pman : Pattern {

	var <>key;
	
	*new {|key|
		^super.new.key_(key);
	}
	
	embedInStream {|event|		
		while {true} {
			MandelClock.instance.getValue(key).yield;
		};
		^event;
	}
}

PmanScale : Pattern {
	embedInStream {|event|		
		while {true} {
			Scale.newFromKey(MandelClock.instance.getValue(\scale), MandelClock.instance.getValue(\tuning)).yield;
		};
		^event;
	}	
}