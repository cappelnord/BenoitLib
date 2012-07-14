/*
	MandelClock
	(c) 2010-11 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Adds a redirect to MandelHub for tempo changes
	
*/


MandelClock : TempoClock {
	var <>hub;
	
	// this calls into the hub, doing the tempo change
	tempo_ {|newTempo|
		hub.changeTempo(newTempo);
	}
	
	// this is called by MandelHub and is the actual tempo method
	commitTempo {|newTempo|
		this.setTempoAtBeat(newTempo, this.beats);
		this.changed(\tempo);  // this line is added
	}
}