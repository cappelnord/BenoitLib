// MIDI Sync Test

(
	var beat;
	var lastTick;
	var tempo;
	
	s.latency = nil;
	
	// init MIDI
	MIDIClient.initialized.not.if({
			MIDIClient.init;
			MIDIClient.sources.do({ arg src, i;
				MIDIIn.connect(i, src);		// connect it
			});
		});
		
	
	MIDIIn.sysrt = {|src, index, data|
	
		(index == 10).if {
			MandelHub.start("MIDISlave");
			m = MandelHub.instance;	
			beat = 0;
			lastTick = Main.elapsedTime;
			
		};
		
		(index == 12).if {
			m.clear;	
		};
		
		(index == 8).if {
			beat = beat + (1/24);
			tempo = 1/24/(Main.elapsedTime - lastTick);
			lastTick = Main.elapsedTime;
			m.pr_receiveTick(0,beat,tempo,true);
		};
		
	};

)

m.gui
m.metro

