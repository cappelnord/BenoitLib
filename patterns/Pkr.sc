/*
	Pkr
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Retrieve the value of a kr NodeProxy via 
	Shared Memory Interface if available.
	
	If not it will pull the value and always
	yield the last received.
	
*/

Plkr : Pfunc {
	*new {|proxy|
		"Plkr is deprecated. Use Pkr instead!".warn;
		^Pkr(proxy);	
	}	
}

Pkr : Pfunc {
	*new {|proxy|
		var check;
		
		// audio?
		proxy.bus.isSettable.not.if {
			"Not a kr NodeProxy. This will only yield 0".warn;
			^Pfunc({0});	
		};
		
		check = {proxy.bus.server.hasShmInterface}.try;
		
		check.if ({
			^Pfunc({proxy.bus.getSynchronous()});
		}, {
			var last = 0.0;
			"No shared memory interface detected. Use localhost server on SC 3.5 or higher to get better performance".warn;	
			proxy.bus.get({|v| last = v;});
			^Pfunc({proxy.bus.get({|v| last = v;}); last;});
		});
	}	
}


// allows the usage of kr NodeProxies in Patterns directly
+ NodeProxy {
	asStream {
		^Pkr(this).asStream;
	}
}

