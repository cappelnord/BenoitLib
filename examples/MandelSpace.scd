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

x[\test].value; // the value now seems to appear to be 1000 but its actually only locally. 

x[\test] = 500;

x[\test].value;

// we can allways acess the raw value:
x.getObject(\test).getValue(false); // overrides decorator

x.getObject(\test).decorator = nil; // now our decorator is cleared

x[\test].value; // and we get the raw value



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
x.getObject(\c).decorator = {|v, space| space.getValue(\a) * space.getValue(\b)}

// \c is now a local MandelSpace value, composed by the global values \a and \b.
// if \a or \b are changed then \c also changes
x[\c].value

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



// *** USE VALUES AS KR BUS ***
// values can be used on the server
// We demonstrate this with ProxySpace

p = ProxySpace.push(s.boot);

x[\freq] = 440


~son = {SinOsc.ar(x[\freq].kr).dup * 0.1}
~son.play

x[\freq] = 220

// we can also add a lag to the values
~son = {SinOsc.ar(x[\freq].kr.lag(1)).dup * 0.1}

x[\freq] = 880

~son.stop;

// let's synthesize the octave and map to ProxySpace
x.getObject(\freq_octave).decorator = {|v, space| space.getValue(\freq) * 2}
x.getObject(\freq_octave).addRelation(\freq)

~son = {SinOsc.ar(x[\freq_octave].kr).dup}
~son.play

// we can now change ~freq_octave through freq
x[\freq] = 100

// or we could change the decorator
x.getObject(\freq_octave).decorator = {|v, space| space.getValue(\freq) * 4}

// *** QUANT TO MANDELCLOCK ***
// Each MandelValue object can have it's own quant. it's usefull if you want
// to have value changes only on a new bar (or something). 
// The setter is responsible to quant.

x[\qtest] = 10
x.getObject(\qtest).addDependant( {|changer, key, value| ("POST: " + value).postln; });

x[\qtest] = 100; // POST is posted immediatly

x.getObject(\qtest).quant = 4

x[\qtest] = 20; // POST is on next bar

// You also can set a global quant which is used if a value hasn't its own quant.
x.quant = 2

x[\testtest] = 10
x.getObject(\testtest).addDependant( {|changer, key, value| ("POST: " + value).postln; });

x[\testtest] = 2