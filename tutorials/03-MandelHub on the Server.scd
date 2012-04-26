/*
	BenoitLib Tutorial 03: MandelHub on the Server
	
	Ways of using MandelHub with scsynth and ProxySpace
	
	Execute each code of line seperately and observe the results.
	If code is enclosed by parenthesis all code is meat to be
	executed at the same time.
*/

// MandelHub can also control stuff on the server side.
// So let's boot it up first.
s.boot;

// Sometimes it's interessting to have the current tempo on the server. This can
// be done with m.tempoBus. Calling m.tempoBus creates a control rate bus on the
// server that get's updated when the tempo changes. This is very usefull for
// triggers or times for rythmic delays.

(
SynthDef(\tempoBusTest, {
    Out.ar(0, RLPF.ar(Impulse.ar(m.tempoBus.kr).dup, 8000, 0.1));
}).add;
)

a = Synth(\tempoBusTest);
m.changeTempo(1, 10);
a.free;

// This can also be used to trigger an envelope. If you quant a synth to a beat
// you can use it along with patterns.

(
SynthDef(\tempoBusTest2, {
    var sig = SinOsc.ar(400);
    var env = EnvGen.kr(Env.perc(0.001, 0.25), Impulse.kr(m.tempoBus.kr));
    Out.ar(0, sig.dup * env);
}).add;
)

a = Synth(\tempoBusTest2);
m.changeTempo(3, 10);
a.free;



// *** Using MandelValues on the Server ***

// Keep in mind, that in this tutorial the only one how can change the values
// is you, but that the key point of MandelSpace is collaboration. I hope you
// still have x assigned to m.space, if not:
x = m.space;

// Let's initialize a value:
x[\busDemo] = 440

// You can map a MandelValue to a control rate bus.
// This is also done automatically everytime this kind of ressource is required,
// so it's not necessary to call this:

x[\busDemo].asBus

// The MandelValue has different methods to access the bus.
// .kr creates an OutputProxy.

a = {SinOsc.ar(x[\busDemo].kr).dup * 0.1}.play
x[\busDemo] = 660; // the frequency of the sine wave changed!
// of course it can be lagged:
a.free; a = {SinOsc.ar(x[\busDemo].kr.lag2(0.5)).dup * 0.1}.play;
x[\busDemo] = 330;
// and a decorator can be applied
x[\busDemo].decorator = {|in| in * 4};
// I hope you realize, that synthesized values can be mapped to a kr bus too.
a.free;



// *** Triggering on the Synth with MandelValues ***

// Sometimes it's interessting to trigger events on the synth at each value
// change. For this you can use the .tr method.

(
a.free; a = {
    var sig = SinOsc.ar(x[\busDemo].kr * 2).dup;
    var env = EnvGen.ar(Env.perc(0.001, 0.5), x[\busDemo].tr);
    sig * env * 0.1;
}.play;
)

x[\busDemo] = 48.midicps;
x[\busDemo] = 50.midicps;
x[\busDemo] = 52.midicps;
x[\busDemo] = 53.midicps;
x[\busDemo] = 55.midicps;
x[\busDemo] = 55.midicps;


// Now you could even use a \mandelspace Pattern to play this sound, even
// if it might not be the best idea. But it works ;-)
(
Pdef(\busTest, Pbind(
    \type, \mandelspace,
    \busDemo, Pwhite(200, 2000),
    \dur, 1/4
)).play;
)

Pdef(\busTest).stop;



// *** Mapping MandelValues to Node Arguments ***

// As MandelValues have a kr bus representation on the server this is possible:

(
a.free; a = {|freq=440|
    SinOsc.ar(freq).dup * 0.1;
}.play;
)

x[\busDemo] = 220;
a.map(\freq, x[\busDemo])
x[\busDemo] = 660;



// *** Using ProxySpace along with MandelSpace ***

// Beno”t and the Mandelbrots regularily uses a pushed ProxySpace.

p = ProxySpace.push(s.boot);

// Most ressources can either be used in a NodeProxy definition or can be used
// as NodeProxy sources.

~tempo = m.tempoBus;
~busDemo = x[\busDemo];

~snd = {SinOsc.ar(~busDemo).dup * 0.1 * Decay.kr(Impulse.kr(~tempo))};
~snd.play;

x[\busDemo] = 330;
m.changeTempo(0.75, 10);

~snd.free;



// *** Experimental Features ***

// Sample and publish a kr NodeProxy to a MandelValue
~lfo = {SinOsc.kr(0.1)};
~lfo <>> x[\lfo];

x[\lfo].value; // execute multiple times to observe

// <>> operator also kind of works in the other direction.

~snd = {|freq=440| SinOsc.ar(freq).dup * 0.1};
~snd.play;
x[\proxyTest] = 220;
x[\proxyTest] <>>.freq ~snd;

x[\proxyTest] = 440;

