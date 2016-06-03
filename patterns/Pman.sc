/*
	Pman
	(c) 2011 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de

	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de

	A stream of shared MandelSpace values.

*/

Pman : Pattern {

	var <>key;

	*new {|key|
		^super.new.key_(key);
	}

	embedInStream {|event|
		while {true} {
			MandelSpace.getValueOrDefault(key).yield;
		};
		^event;
	}
}

PmanScale : Pattern {

	// this is a bad design decision but more practiable. As MandelHub and MandelSpace
	// can be seen as a Singleton every PmanScale instance should behave the same at a given time.

	// Saving this as a classvar doesn't reset the scale at instantiation.
	classvar lastScaleKey = \minor;

	embedInStream {|event|
		var scaleKey, tuningKey, scale;
		while {true} {
			scaleKey = MandelSpace.getValueOrDefault(\scale);
			tuningKey  = MandelSpace.getValueOrDefault(\tuning).asSymbol;

			Main.versionAtLeast(3,7).if({
				Tuning.names.includes(tuningKey).not;
			}, {
				TuningInfo.tunings.at(tuningKey).isNil;
			}).if {
				("Unknown Tuning " ++ tuningKey.asString).warn;
				"PmanScale: Falling back to default tuning ...".postln;
				tuningKey = nil;
			};

			scaleKey.isKindOf(Scale).if({
				scale = scaleKey;
				tuningKey.notNil.if({
					scale.tuning_(tuningKey);
				});
				lastScaleKey = scaleKey;
			}, {
				scale = Scale.newFromKey(scaleKey.asSymbol, tuningKey);
				scale.isNil.if ({
					scale = Scale.newFromKey(lastScaleKey.asSymbol, tuningKey);
					("PmanScale: Falling back to last valid scale in this stream: " ++ lastScaleKey.asString).postln;
				}, {
					lastScaleKey = scaleKey;
				});
			});

			scale.yield;
		};
		^event;
	}
}