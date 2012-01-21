// MandelSpace Examples

// First build a MandelClock instance
m = MandelClock.startLeader("MSpaceTest");

// We use MandelSpace quite often, so we assign x to it
x = m.space

// MandelSpace can be used like a Dictionary, so we can set values quite easily.
// All keys need to be symbols. All values are shared with all users.

x[\test] = 10;



// *** SCHEDULED CHANGES ***
// A MandelValue can now it's future. You can schedule changes.

// schedules the change one bar ahead
x.getObject(\test).setValue(100, m.clock.beats + 4)



// *** DECORATORS ***

// We can use a Decorator function to locally change the yielded value
x.getObject(\test).decorator = {|v|Êv * 10};

x[\test]; the value now seems to appear to be 100 but its actually only locally. 

x[\test] = 500;

x[\test];

// we can allways acess the raw value:
x.getObject(\test).getValue(false); // overrides decorator

x.getObject(\test).decorator = nil; // now our decorator is cleared

x[\test]; // and we get the raw value



// *** DEPENDENCIES ***
// (MandelValues can be used as a Model and uses the standard SC Dependency interface)

(
x.getObject(\test).addDependant({|theChanger, what, value|
	theChanger.dump; // MandelValue object
	what.dump; // key
	value.dump; // the (decorated) value
});
)

// the function is triggered, when the value is changed
x[\test] = 20

// scheduled changes also work
x.getObject(\test).setValue(500, m.clock.beats + 4)



// *** SYNTHESIZED VALUES WITH DECORATORS ***
// We can locally create new MandelSpace Values, synthesized by others trough decorators

x[\a] = 10;
x[\b] = 20;
x.getObject(\c).decorator = {|v, space| space[\a] * space[\b]}

// \c is now a local MandelSpace value, composed by the global values \a and \b.
// if \a or \b are changed then \c also changes
x[\c]

x.getObject(\c).addDependant({|theChanger, what, value|
	("Yeah: c is now " ++ value).postln;
});

x[\a] = 50; // the Dependy function isn't called. but we can fix that!



// *** RELATIONS ***

// we can explicitly establish a relation between values.
// in our example \c relies on \a and \b

x.getObject(\c).addRelation(\a);
x.getObject(\c).addRelation(\b);

// if now either a or b is changed, the change function for c is called. 
x[\a] = 100

// this can be used to construct quite interessting stuff



// *** MAP VALUES TO PROXY SPACE ***
// values can be mapped to Proxy Space
// First we need to push (or specify) the PS.
s.boot;
p = ProxySpace.push(s)

x[\freq] = 440
x.getObject(\freq).mapToProxySpace

// The MandelSpace value \freq is now mapped to \freq.kr
~son = {SinOsc.ar(~freq).dup * 0.1}
~son.play

x[\freq] = 220

// we can also add a lag to the values
x.getObject(\freq).mapToProxySpace(1)

x[\freq] = 880

~son.stop;

// let's synthesize the octave and map to ProxySpace
x.getObject(\freq_octave).decorator = {|v, space| space[\freq] * 2}
x.getObject(\freq_octave).addRelation(\freq)
x.getObject(\freq_octave).mapToProxySpace(1)

~son = {SinOsc.ar(~freq_octave).dup}
~son.play

// we can now change ~freq_octave through freq
x[\freq] = 100

// or we could change the decorator
x.getObject(\freq_octave).decorator = {|v, space| space[\freq] * 4}

// *** QUANT TO MANDELCLOCK ***
// Each MandelValue object can have it's own quant. it's usefull if you want
// to have value changes only on a new bar (or something). 
// The setter is responsible to quant.

x[\quant] = 10
x.getObject(\quant).addDependant( {|changer, key, value| ("POST: " + value).postln; });

x[\quant] = 100; // POST is posted immediatly

x.getObject(\quant).quant = 4

x[\quant] = 20; // POST is on next bar