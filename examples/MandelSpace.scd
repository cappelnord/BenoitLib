// execute line after line and observe results

m = MandelClock.startLeader("Patrick");

m.space.setValue(\a, 10)
m.space[\a].decorator = {|v| v * 2}
m.space.getValue(\a)
m.space[\a].getValue() // alternative

m.space.at(\x).decorator_({|v, space| space.getValue(\a) - 1}) // alterantive
m.space.getValue(\x)

m.space[\a].addListener({|v| ("A: " ++ v).postln;})
m.space.setValue(\a, 15)

m.space[\x].addListener({|v| ("X: " ++ v).postln;})
m.space[\x].addDependency(\a); // \x now depends on \a

m.space.setValue(\a, 5)

/* decorator and listener functions receive: value, MandelSpace instance, key
   listeners receive decorated values */

/* complete example with all stuff. 
   Rember: \ok is also distributed to all other clients, 
   but they have to add decorators/listeners and ProxySpace
   Mappings for themselves.
*/

p = ProxySpace(s.boot).push;

m.space[\ok].setValue(400)
m.space[\ok].mapToProxySpace(0) // 0 = lag (default)

~yeah[0] = { SinOsc.ar(~ok.kr)};
~yeah.play;

m.space[\ok].setValue(800)

~ok.set(\lag, 0.01)

(
x = Pbind(
	\type, \mandelspace,
	\ok, Pwhite(400, 800)
).play
)

m.space[\ok].decorator = {|v| v * 4}
m.space[\ok].addListener({|v| v.postln;})