/*
	MandelTimer
	(c) 2012 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	MandelTimer provides a resetable timer, visible
	in the GUI.	
*/

MandelTimer : MandelModule {
	
	var startTime;
	var <hub;
	
	*new {|hub|
		^super.new.init(hub);	
	}
	
	init {|a_hub|
		hub = a_hub;
		startTime = Process.elapsedTime;
	}
	
	elapsedTime {
		^(Process.elapsedTime - startTime);
	}
	
	reset {
		hub.leading.if {
			startTime = Process.elapsedTime;
			this.sendElapsedTime;	
		};
	}
	
	onBecomeFollower {|hub|
		hub.net.addOSCResponder(\follower, "/timerElapsedTime", {|header, payload|
			startTime = Process.elapsedTime - payload[0];
		}, \leaderOnly);
	}
	
	onSyncRequest {|hub|
		this.sendElapsedTime;
	}
	
	sendElapsedTime {
		hub.net.sendMsgBurst("/timerElapsedTime", \timeCritical, this.elapsedTime);
	}
}