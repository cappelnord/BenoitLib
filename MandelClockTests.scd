
// MandelClock Tests
MandelClock.debug = true

// #1: Tempo Tests 
a = MandelClock.new("Leader", 0, 2, "test", leading:true);
a.gui;
a.metro(-1); // left

m = MandelClock.startFollower("Follower", action: {m = MandelClock.instance;});
m.gui;
m.metro(1); // right

( // This simulates out-of-sync clocks, this should never happen to this extent!
{
	a.changeTempo(2);
	0.5.wait;
	m.listenToTicks = false;
	a.changeTempo(4);
	5.wait;
	a.changeTempo(2);
	m.listenToTicks = true; // now m should resync
	
}.fork;
)

// accelerando
a.changeTempo(3,30);