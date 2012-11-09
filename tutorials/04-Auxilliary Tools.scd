/*
	BenoitLib Tutorial 04: Auxilliary MandelHub Tools and Goodies

	Execute each code of line seperately and observe the results.
	If code is enclosed by parenthesis all code is meat to be
	executed at the same time.
*/

// *** Dumping MandelSpace Values ***

// (I hope you still have x assigned to m.space.)

x.dumpValues;

// You now see all values you set in this tutorial yet but also others, 
// quite familliar to Pattern users.

//  \scale, \octaveRatio, \ctranspose, \tuning,
//  \root, \mtranspose, \stepsPerOctave

// All these values are set to Events default. But what are they for?



// *** The Harmony Master ***

// The initial reason for developing MandelSpace was to find a way
// for the band to play with a common harmonic progression. When we decide to
// do this, one of the band takes the roll of the Harmony Master and
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

// You see, that he now sets values like \root and \scale, which are used
// in events and patterns to calculate the frequency of a note. If all players
// use these values in their patterns they are synced harmonically.

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

// now change some values in the harmonyMaster Pdef
// (e.g. \major instead of \minor) and you can hear the harmonic changes.

// To be even more musical there is a magic \harmony key in \mandelspace
// events. It takes two kindes of notation and sets \root and \scale
// accordingly.

// Notation 1 takes Symbols from roman number analysis (based on c-major).
// You can add a s or b at the end to add an accidential.
// * \V    --> root: 7, scale: \major
// * \iii  --> root: 4, scale: \minor
// * \IIIb --> root: 3, scale: \major

// Notation 2 just mixes the root number with a uppercase M for major
// and a downcase m for minor:
// * \M7   --> root: 7, scale: \major
// * \m4   --> root: 4, scale: \minor
// * \M3   --> root: 3, scale: \major

( // is \harmonyMaster still running?
Pdef(\harmonyMaster, Pbind(
    \type, \mandelspace,
    \deltaSched, 1,
    \harmony, Pseq([\i, \iv, \V, \i], inf),
    \dur, 4
));
)

Pdef(\harmonyMaster).stop; Pdef(\hmTest).stop;



// *** rootFreq, mtransposeFreq, ctransposeFreq ***

// It's easy to use Harmony Master with Patterns.
// But what about plain NodeProxies?
// How can all the Event magic for calculating a frequency be used with them?

// MandelSpace has 3 synthesized MandelValues by default which depend on the
// Harmony Master values. With the .kr function they can be used easily within
// a Synth or NodeProxy.

a = {SinOsc.ar(x[\rootFreq].kr).dup * 0.1}.play

x[\root] = 5; // the pitch should be higher now

x[\root] = 0; // back to the start

a.free;

// The 3 values depend on different values. rootFreq just takes the root
// note in account, while mtransposeFreq and ctransposeFreq take the root and
// m/ctranspose values into account too.

// So with these synthesized values it's easy to use a common root note even
// with plain NodeProxies.



// *** Metronome ***

// It's sometimes handy to check the sync with a metronome.
// MandelHub has one built in:

m.tools.metro;

// Of course you can stop it too:

m.tools.stopMetro;



// *** ID3v1 genres ***

// Because it's the most important feature for any laptop band we integrated
// the ID3v1 genre table.

m.tools.genres;

// If you don't know what style you want to play in your rehearsal let MandelHub
// decide for you:

m.tools.genres.choose;