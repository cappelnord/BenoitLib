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
	
	*new {|value, clock|
		^super.new.init(value, clock);
	}
	
	init {|startValue, startClock|
		value = startValue;
		clock = startClock ? TempoClock.default;
		list = SortedList.new(8, {|x,y| x[0] < y[0]});	
	}
	
	schedule {|newValue, beats|
		var val = this.value();
		(beats <= clock.beats).if({
			value = newValue;
		},{
			list.add([beats, newValue]);
		});
		^val;
	}
	
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
}