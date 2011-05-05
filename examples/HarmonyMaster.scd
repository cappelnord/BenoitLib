/*
	Beeing Harmony Master is quite simple but needs some explanation.
	
	You can use the event type \mandelspace after MandelClock is started.
	Each key other than \dur and \deltaSched will be set in the Shared
	Value Space (called MandelSpace).
	
	To assure, that the value is transmitted for the next quant it's good to process
	it 1 Beat in advance. Use the phase argument of quant for this and keep \deltaSched
	in sync with that. Also check out the special cases like Scale.
*/

// you NEED to have a MandelClock instance but you don't have to be the leader.
m = MandelClock.startLeader("Yeah");

(
Pdef(\harmonyMaster).quant = [4, -1];

Pdef(\harmonyMaster, Pbind(
	\type, \mandelspace,
	\deltaSched, 1,
	\dur, 4,
	\mtranspose, Pseq([0,3,1,4],inf),
	\scale, Pseq([\minor, \minor, \minor, \major],inf)
));

Pdef(\harmonyMaster).play;
)

(
Pdef(\test).quant = [4];

Pdef(\test, Pbind(
	\dur, 1/4,
	\degree, Pseq([0,2,4,7], inf),
	\mtranspose, Pman(\mtranspose),
	\scale, PmanScale()
));

Pdef(\test).play;
)

It's also easy to get/set a value in MandelSpace:

MandelClock.instance.getValue(\scale);

MandelClock.instance.setValue(\blub, 5000);
MandelClock.instance.getValue(\blub);