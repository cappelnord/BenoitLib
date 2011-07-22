/*
	MandelSpace
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Shared Variable Space used by MandelClock
	
*/

MandelSpace : MandelPlug {
	
	var <bdlDict;
	var mc;
	
	*new {|maclock|
		^super.new.init(maclock);	
	}
	
	init {|maclock|
		mc = maclock;
		
		bdlDict = Dictionary.new;
		
		this.pr_buildEvents();
		this.pr_setDefaults();
	}
	
	pr_setDefaults {
		bdlDict.put(\scale, BeatDependentValue(\minor));
		bdlDict.put(\tuning, BeatDependentValue(\et12));
		bdlDict.put(\mtranspose, BeatDependentValue(0));
		bdlDict.put(\ctranspose, BeatDependentValue(0));
		bdlDict.put(\root, BeatDependentValue(0));	
	}
	
	pr_buildEvents {
		Event.addEventType(\mandelspace, {
			var schedBeats;
			~deltaSched = ~deltaSched ? 0.0;
			schedBeats = MandelClock.instance.clock.beats + ~deltaSched;
			currentEnvironment.keys.do {|key|
				((key != \type) && (key != \dur)).if {
					MandelClock.instance.setValue(key, currentEnvironment.at(key), schedBeats);
				};
			};
		});
	}
	
	pr_setBDL {|key, value, schedBeats|
		var bdl = bdlDict.at(key.asSymbol);
		
		bdl.isNil.if ({
			bdl = BeatDependentValue(value);
			bdlDict.put(key, bdl);
			^value;	
		}, {
			^bdl.schedule(value, schedBeats);
		});		
	}
	
	getValue {|key|
		^bdlDict.at(key.asSymbol).value();
	}
	
	setValue {|key, value, schedBeats=0.0|
		mc.sendMsgCmd("/value", key.asString, value, schedBeats.asFloat);
		^this.pr_setBDL(key, value, schedBeats);
	}
	
	onBecomeLeader {|mc|
		mc.addResponder(\leader, "/requestValueSync", {|ti, tR, message, addr|
			bdlDict.keys.do {|key|
				var value = bdlDict.at(key);
				mc.sendMsgCmd("/value", key.asString, value.value(), 0.0);
				value.list.do {|item|
					mc.sendMsgCmd("/value", key.asString, item[1], item[0]);
				};
			};
		});	
	}
	
	onStartup {|mc|
		mc.addResponder(\general, "/value", {|ti, tR, message, addr|
			(message[1].asString != mc.name).if {
				this.pr_setBDL(message[2].asSymbol, message[3], message[4].asFloat);
			};
		});
	}
}