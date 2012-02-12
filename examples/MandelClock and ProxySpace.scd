// Several aspects of MandelClock can be mapped to ProxySpace. 
// So first, establish a working ProxySpace, push it and create a MandelClock

s.boot;
p = ProxySpace.push(s)

m = MandelClock.startLeader("MCTest");
x = m.space;

// Use the kr Busses of MandelValues
// You directly can map MandelValues as a kr Bus

x[\bbfreq] = 400;

~bb = {SinOsc.ar(x[\bbfreq].kr)}
~bb.play;

x[\bbfreq] = 800;

// It's also good to lag the values

~bb = {SinOsc.ar(x[\bbfreq].kr.lag2(0.5))}

x[\bbfreq] = 1200;


// *** TEMPO PROXY ***
// You can map the current tempo in BPS to a kr NodeProxy

~tempo = m.tempoBus

~imp = {Impulse.ar(~tempo).dup * 0.5}
~imp.play

m.changeTempo(1)
m.changeTempo(3)

~imp.stop

// *** FREQUENCY TOOLS ***
// HarmonyMaster (see the other example) can be used in ProxySpace as well.
// Frequencies of all harmony related values are mapped to rootFreq, ctransposeFreq and mtransposeFreq


~rootFreq = x[\rootFreq];
~son = {SinOsc.ar(~rootFreq).dup * 0.1}
~son.play

x[\root] = 6

// also possible to directly use the bus
~son = {SinOsc.ar(x[\rootFreq].kr).dup * 0.1}

~ctransposeFreq = x[\ctransposeFreq]

~son2 = {SinOsc.ar(~ctransposeFreq * 0.5).dup * 0.1}
~son2.play

x[\ctranspose] = 4


~son.stop
~son2.stop

// *** MAP MANDELSPACE VALUE TO PROXY SPACE ***
// You can use any MandelSpace Value in ProxySpace, even synthesized ones.

x[\test] = 100
~test = x[\test]

~testSound = {SinOsc.ar(~test).dup * 0.1}
~testSound.play

x[\test] = 200

// you could also apply a decorator
x.getObject(\test).decorator = {|v| v * 4}

x[\test] = 100

x.getObject(\test).decorator = nil

// lagging values should be done in the synth

~testSound = {SinOsc.ar(~test.lag2(0.5)).dup * 0.1}

// *** PUBLISH KR PROXIES TO MANDELSPACE ***
// You can publish a .kr Bus or NodeProxy via MandelSpace

~kr = {SinOsc.kr(0.1)}

~kr.publish(x[\kr]);

x[\kr].value