/*
	MandelTools
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Contains different convenience tools.
	
*/

MandelTools : MandelModule {
	
	var mc;
	var space;
		
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	init {|maclock|
		mc = maclock;	
		space = mc.space;
	}
}