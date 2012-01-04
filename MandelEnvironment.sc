/*
	MandelEnvironment
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Acts as an Environment for MandelSpace
	
*/

MandelEnvironment : EnvironmentRedirect {

    var space;
	
	*new {|space|
		^super.new.init(space);
	}
	
	init {|a_space|
		space = a_space;
	}
	
	at {|key|
		^space.at(key);	
	}
	
	put {|key, obj|
		space.put(key, obj);	
	}
	
	localPut {|key, obj|
		space.put(key, obj); // override network?
	}
	
	removeAt {|key|
		space.removeAt(key);	
	}	
}