/*
	BeatDependentValue
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
	
	Can be used as value-container which can schedule changes.
	
*/

BeatDependentValue {
	
	var <list;
	var value;
	var clock;
	var <>onChangeFunc;
	
	*new {|value, clock|
		^super.new.init(value, clock);
	}
	
	init {|startValue, startClock|
		value = startValue;
		clock = startClock ? TempoClock.default;
		list = SortedList.new(8, {|x,y| x[0] < y[0]});	
	}
	
	schedule {|newValue, beats|
		(beats <= clock.beats).if({
			value = newValue;
			this.callOnChange();
		},{
			list.add([beats, newValue]);
			
			// i don't want to schedule this if it isn't necessary.
			// even if this is a little bit problematic ...
			onChangeFunc.notNil.if { clock.schedAbs(beats, {this.callOnChange}); };
		});
		^this.value;
	}
	
	// actually a value reset also could be done through scheduling, but i think, that
	// this is a safer thing and i don't think, that callOnChange functions get
	// very common.
	
	value {
		(list.size() > 0).if {
			(list[0][0] <= clock.beats).if {
				value = list[0][1];
				list.removeAt(0);
				^this.value();	
			};
		};
		^value;
	}
	
	callOnChange {
		onChangeFunc.notNil.if {
			onChangeFunc.value(this.value, this);	
		};	
	}
}