/*
	BenoitLib Tutorial 04: Auxilliary MandelHub Tools and Goodies
	
*/

// *** Dumping MandelSpace Values ***

// (I hope you still got x set to m.space.)

x.dumpValues;

// You now might see all values you set in this tutorial but also others, 
// quite familliar to Pattern users.

//	\scale, \octaveRatio, \ctranspose, \tuning,
//	\root, \mtranspose, \stepsPerOctave

// All these values are set to Events default. But what are they for?



// *** The Harmony Master ***

// The initial reason for developing MandelSpace was to find a way
// for the band to play with harmonic progressions. When we decide to
// do this, one of the band takes the roll of the harmony master and
// uses a Pdef like this to control the harmonic progression.


(
Pdef(\harmonyMaster).quant = [4, -1];

Pdef(\harmonyMaster, Pbind(
	\type, \mandelspace,
	\deltaSched, 1,
	\mtranspose, 0,
	\scale, \minor,
	\tuning, \et12,
	\root, 0,
	\dur, 4
));

Pdef(\harmonyMaster).play;
)

// You see, that he now sets values like \root and \scale, which aare used
// in events and patterns to calculate the frequency of a note. If all players
// use these values in their patterns they are harmonically synced together.

(
Pdef(\hmTest).quant = 4;

Pdef(\hmTest, Pbind(
	\dur, 1,
	\root, Pman(\root),
	\scale,  PmanScale(),
	\degree, Pseq([0,2,4,7],inf)
));

Pdef(\hmTest).play;
)

// now change some values in the harmonyMaster Pdef (for example: \major instead of \minor)
// and you can hear the harmonic changes.

Pdef(\harmonyMaster).stop; Pdef(\hmTest).stop;



// 