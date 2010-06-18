// TODO: add audible test

// MandelClock Tests
MandelClock.debug = true

// #1: Tempo Tests 
a = MandelClock.new("Leader", 0, 2, "test", leading:true);
a.gui;

m = MandelClock.startFollower("Follower", action: {m = MandelClock.instance;});
m.gui;

( // This simulates out-of-sync clocks
{
	a.changeTempo(2);
	0.5.wait;
	m.listenToTicks = false;
	a.changeTempo(4);
	1.wait;
	a.changeTempo(2);
	m.listenToTicks = true; // now m should resync
	
}.fork;
)