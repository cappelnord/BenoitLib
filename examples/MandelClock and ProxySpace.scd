// Several aspects of MandelClock can be mapped to ProxySpace. 
// So first, establish a working ProxySpace, push it and create a MandelClock

s.boot;
p = ProxySpace.push(s)

m = MandelClock.startLeader("MCTest");
x = m.space;

// *** TEMPO PROXY ***
// You can map the current tempo in BPS to a kr NodeProxy

m.tools.makeTempoProxy

~imp = {Impulse.ar(~tempo).dup * 0.5}
~imp.play

m.changeTempo(1)
m.changeTempo(3)

~imp.stop

// *** FREQUENCY TOOLS ***
// HarmonyMaster (see the other example) can be used in ProxySpace as well.
// This maps HarmonyMaster to ~rootFreq, ~mtransposeFreq and ~ctransposeFreq

m.tools.mapFreqsToProxySpace

~son = {SinOsc.ar(~rootFreq).dup * 0.1}
~son.play

x[\root] = 6

~son2 = {SinOsc.ar(~ctransposeFreq * 0.5).dup * 0.1}
~son2.play

x[\ctranspose] = 4


// you can also lag the frequencies if you like

m.tools.mapFreqsToProxySpace(1)

x[\root] = 3

~son.stop
~son2.stop

// *** MAP MANDELSPACE VALUE TO PROXY SPACE ***
// You can map any MandelSpace Value to ProxySpace, even synthesized ones.

x[\test] = 100
x.getObject(\test).mapToProxySpace(0.5); // 0.5 seconds lag

~testSound = {SinOsc.ar(~test).dup * 0.1}
~testSound.play

x[\test] = 200

// you could also apply a decorator
x.getObject(\test).decorator = {|v| v * 4}

x[\test] = 100

x.getObject(\test).decorator = nil
