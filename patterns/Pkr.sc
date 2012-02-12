/*
	Pkr
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Retrieve the value of a kr Bus/NodeProxy via 
	Shared Memory Interface if available.
	
	If not it will pull the value and always
	yield the last received.
	
*/

Plkr : Pfunc {
	*new {|bus|
		"Plkr is deprecated. Use Pkr instead!".warn;
		^Pkr(bus);	
	}	
}

Pkr : Pfunc {
	*new {|bus|
		var check;
		var last = 0.0;
		
		bus = bus.asBus;

		// audio?
		bus.isSettable.not.if {
			"Not a kr Bus or NodeProxy. This will only yield 0".warn;
			^Pfunc({0});	
		};
		
		check = {bus.server.hasShmInterface}.try;
		
		check.if ({
			^Pfunc({bus.getSynchronous()});
		}, {
			"No shared memory interface detected. Use localhost server on SC 3.5 or higher to get better performance".warn;	
			bus.get({|v| last = v;});
			^Pfunc({bus.get({|v| last = v;}); last;});
		});
	}	
}
