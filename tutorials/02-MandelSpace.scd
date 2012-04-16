/*
	BenoitLib Tutorial 02: Sharing Data with MandelSpace
*/

// *** What is MandelSpace? ***

// MandelSpace is in essence a Dictionary-alike structure that is shared
// between all members of the MandelHub. Any member can set any value at any
// time. Values can't only be set at instant but also can be scheduled. Values
// are wrapped into MandelValues which know about the present and future of
// the MandelValue and has othe features.

// Keys in the MandelSpace are restricted to symbols.



// *** Using MandelSpace ***

// MandelSpace can be accessed via m.space, but it's more comfortable to
// assign the space to its own interpreter variable, by convention x.

x = m.space;



// *** Setting and Retreiving Values ***

// Setting a value is exactly the same as setting a value in a Dictionary.
// If a value is set by one user the value is transmitted to all users of
// the MandelHub.

x[\tutorial] = 10;

// Retreiving a value is a bit different, as accessing MandelSpace via 
// the Dictionary interface yields MandelValues.

x[\tutorial]; // MandelValue: 10

// You can retreive the value either with the getValue or the value method.
// The value method is especially interessting, as MandelValues act like functions.

x[\tutorial].value; // 10



// *** MandelValues for function composition ***

// MandelValue is a subclass of AbstractFunction, so you can compose functions
// with MandelValues

f = {10} * x[\tutorial]; // a BinaryOpFunction
f.value(); // 100
x[\tutorial] = 20;
f.value(); // 200

// This also works with Patterns and Streams!

f = Pseq([1, 2, 3], inf) * x[\tutorial];
g = f.asStream;

g.next; // 20
x[\tutorial] = 5;
g.next; // 10

x[\offset] = 6;
g = g + x[\offset];
g.next; // 21

// This makes MandelValues a good friend of Patterns. You can also use
// the Pman Pattern object along with the Symbol to access MandelSpace.

f = Pman(\offset) + Pman(\tutorial)
g = f.asStream;
g.next; // 11
x[\offset] = 11;
g.next; // 16



// *** Scheduling Values ***

// We can schedule value changes in the future. This is not only handy
// but defies the problem of latency. The scheduling takes place on every
// user and the message is sent immediately. If there is enough time
// the message is sent multiple times to assure arrival. As all users share
// a synchronized clock we can be sure, that a value change takes place e.g.
// at the beginning of the next bar. This is vital for harmonic progressions.

// There are different methods for that.
// The most basic option is the setValue method.

x[\sched].setValue(10);
x[\sched].value; // OK, it's 10 now.
x[\sched].setValue(100, m.clock.beats + 8); // the value changes 8 beats from now
x[\sched].value; // If you're fast enough it's still 10. If you waited 8 beats it's 100.

// If you want value changes only to occur at the beginning of a bar you
// can set a quant. Look in the Quant help file for more info.

// Be aware, that quants are only set locally.

x[\sched].quant = 4;
x[\sched] = 200;
x[\sched].value; // observe fast enough and it might be still 100.
			   // will change to 200 at the start of the next bar.
			   
// You can clear the quant by assigning nil.

x[\sched].quant = nil;



// *** Decorator Functions ***

// Each MandelValue can have a local decorator function. The decorator
// acts as a filter for the value.

x[\dec] = 5;
x[\dec].decorator = {|in| in * 100};
x[\dec].value; // 500
x[\dec] = 2;
x[\dec].value; // 200

// Keep in mind: Decorator functions are local only.
// You can override the Decorator function:

x[\dec].getValue(false);



// *** MandeValues as Models ***

// MandelValues can act as models: You can add dependant objects
// or functions to a MandelValue. The dependant is called on each
// value change.

(
x[\dep].addDependant({|obj, what, value|
	"ACTION!".postln;
	obj.postln;
	what.postln;
	value.postln;	
});
)

x[\dep] = 100

// Let's try it with a scheduled value change:

x[\dep].setValue(200, m.clock.beats + 8); // wait for it!

// Add add a decorator (just for fun!)
// (Observe, setting a new decorator also messages the dependants)

x[\dep].decorator = {|in| in / 5};

x[\dep].setValue(5000, m.clock.beats + 8); // wait for it



// *** Synthesized Values ***

// Combining dependencies and a decorator function you can create
// synthesized MandelValues. The decorator function hereby computes
// a value by processing other MandelValues.

x[\one] = 10;
x[\two] = 10;
x[\synth].decorator = x[\one] * x[\two];

x[\synth].value; // 100

// OK, now we add a dependant for x[\synth]

x[\synth].addDependant({|obj, what, value| ("Yeah: " ++ value).postln;});

x[\one] = 50; // changing x[\one] doesn't call the depenandant yet.
			  // we have to add x[\synth] as a dependant to one and two.

x[\one].addDependant(x[\synth]);
x[\two].addDependant(x[\synth]);

x[\one] = 50; // now our function gets called when one of its components
			  // is changed.



// *** Setting Values through Patterns ***

// MandelSpace defines a new Event-Type, \mandelspace.
// Each key/value pair (except for \type, \dur, \removeFromCleanup
// and \deltaSched) is set in MandelSpace.

(
Pdef(\test).quant = 4;

Pdef(\test, Pbind(
	\type, \mandelspace,
	\test, Pwhite(0,100,inf),
	\dur, 1
));
)

Pdef(\test).play;

// x[\test] is now set to a random number from 0 to 100 on each beat.

x[\test].value; // execute multiple times!

Pdef(\test).stop;

// *** Schedule Values through Patterns ***

// You can only guarantee, that values are transmitted in time if
// they're scheduled for the future, as all users have received
// the transmission then.

// In Events/Patterns you can add a deltaSched key that isn't transmitted
// as a value but schedules the value for the <jeweiligen> time in beats
// to the future. Together with a quant and a negative offset in quant, 
// matching deltaSched, you can assure, that the everyone received the
// value on the next bar.

(
Pdef(\test2).quant = [4, -1];

Pdef(\test2, Pbind(
	\type, \mandelspace,
	\deltaSched, 1,
	\myDeg, Pseq([0,1,2,3,4,4],inf),
	\dur, 4
));

Pdef(\test2Play).quant = 4;

Pdef(\test2Play, Pbind(
	\degree, Pman(\myDeg),
	\dur, 1
));
)

Pdef(\test2).play; // might take a bar to take place

Pdef(\test2Play).play; // some sound to test with

Pdef(\test2).stop; Pdef(\test2Play).stop;

// MandelSpace also works (kind of) on the server side. 
// Check out the next tutorial to find out more!
