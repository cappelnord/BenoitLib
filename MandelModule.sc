/*
	MandelModule
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	MandelModule defines an interface and provides
	empty implementations for MandelClock Plugins.
	
*/

MandelModule {
	
	var mc;
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	init {|maclock|
		mc = maclock;
	}	
	
	
	onStartup {|mc|
		
	}
	
	onBecomeLeader {|mc|
		
	}
	
	onBecomeFollower {|mc|
		
	}
}