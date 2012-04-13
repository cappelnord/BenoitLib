
// MandelClock Tests
MandelHub.debug = true

// #1: Tempo Tests 
a = MandelHub("Leader", 0, 2, "test", leading:true, timeClass: MandelTimeDriver);
a.gui;
a.metro(-1); // left

m = MandelHub.join("Follower", action: {m = MandelHub.instance;});
m.gui;
m.metro(1); // right

( // This simulates out-of-sync clocks, this should never happen to this extent!
{
	a.changeTempo(2);
	0.5.wait;
	m.time.listenToTicks = false;
	a.changeTempo(4);
	0.2.wait;
	a.changeTempo(2);
	m.time.listenToTicks = true; // now m should resync
	
}.fork;
)

// accelerando
a.changeTempo(2);
