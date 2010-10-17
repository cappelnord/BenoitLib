// a test to check if a MandelClock Leader can be forced to a sync signal

(
	MandelClock.startLeader("Test");
	MandelClock.debug=true;
	m = MandelClock.instance;
	m.listenToTicks = true;
	m.gui;
	
	fork({
		var tempo = 1.5;
		var beat = 0;
		var num = 12;
		inf.do {|i|
		
		m.pr_receiveTick(0,beat,tempo,true);
		
		beat = beat + (1/num*tempo);
		if(tempo > 1.5)
		{
			// tempo = tempo - 0.001;
		};
		(1/num).wait;
			
		}}, clock: SystemClock);
)

AppClock

m.metro

fork
AppClock

Clock