/*
	Plkr
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Use the last retreived value from a kr NodeProxy
	as a value in a Pattern.
	
	It actually retrieves the current value if a
	Shared Memory Interface is detected.
	
*/

Plkr : Pfunc {
	*new {|proxy, init=0|
		var last = init;
		if(proxy.bus.isSettable.not, {
			proxy.bus.get({|v| last = v;});
			^Pfunc({proxy.bus.get({|v| last = v;}); last;});
		}, {
			^Pfunc({proxy.bus.getSynchronous()});
		});
	}	
}


// allows the usage of kr NodeProxies in Patterns directly
+ NodeProxy {
	asStream {
		^Plkr(this).asStream;
	}
}

