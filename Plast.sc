/*
	Plast
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Remembers the last value of a key.
	
*/

Plast : Pattern {

	var <>key;
	var last;
	
	*new {|key|
		^super.new.key_(key);
	}
	
	embedInStream {|event|
		var ret;
		
		while {true} {
			ret = last ?? event.use({event.at(key).value});
			last = event.use({event.at(key).value});
			last.yield;
		};
		
		^event;
	}
}