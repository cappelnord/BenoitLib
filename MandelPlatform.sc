/*
	MandelPlatform
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Contains platform specific functionallity
	
*/

MandelPlatform : MandelModule {
	
	var mc;
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	init {|maclock|
		mc = maclock;	
	}
	
	displayNotification {|title, message|
		// NOOP;
	}
	
	focusCurrentDocument {
		// don't know if this can apply to other platforms
	}
}

MandelPlatformLinux : MandelPlatform {
	displayNotification {|title, message|
		("notify-send '" ++ title ++ "' '"++ message ++ "'").unixCmd(postOutput:false);
	}
}

MandelPlatformOSX : MandelPlatform {
	
	onStartup {
		("osascript '" ++ mc.classPath("mandelRegisterGrowl.scpt") ++ "'").unixCmd(postOutput:false);
	}
	
	displayNotification {|title, message|
		("osascript '" ++ mc.classPath("mandelNotify.scpt") ++ "' '" ++ title ++ "' '" ++ message ++ "'").unixCmd(postOutput:false);
	}
	
	focusCurrentDocument {
		(Platform.ideName == "scapp").if { // might make sense for other ides, but can't try
			Document.current.front;
		};	
	}
}