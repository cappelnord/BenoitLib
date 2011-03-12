MandelClockTap {
	
	var instance, clock, myClock;
	var window;
	var tempo, bpm, beat;
	
	var button, bpmText;
	
	var newTempo;
	var newBeat;
	
	var tapNum = 0;
	var lastTaps = #[0];
	
	var ser = 0;
	
	var status = 0;
	
	var <>open = true;
		
	*new {|instance|
		^super.new.init(instance);
	}
	
	init {|a_instance|
		
		var dec;
		instance = a_instance ? MandelClock.instance;
		clock = instance.clock;
		myClock = TempoClock.new(clock.tempo);
		
		window = Window.new("MandelClockTap", Rect(400,400,288,45), false);
		dec = window.addFlowLayout(10@10, 5@5);
		
		button = Button(window, 180@20).states_(
			[["Feeding", Color.black, Color.green],
			 ["Waiting ...", Color.black, Color.red],
			 ["Syncing ...", Color.black, Color.yellow]]
		).action_({
			this.tap;
		});
		
		Button(window, 20@20).states_([["I", Color.black, Color.clear]]).action_({
			StringInputDialog.new("BPM", "Please enter desired BPM", {|txt|
				this.setBPM(txt.asFloat);
			});
		});
		
		bpmText = StaticText(window, 50@20);
		dec.nextLine;

		
		this.setBPM(instance.clock.tempo * 60);
		this.setBeat(instance.clock.beats);
		
		window.front;
		
		window.onClose_({
			instance.tapInstance = nil;
			open = false;
		});
		
		this.progress;	
	}
	
	setBPM {|a_bpm|
		bpm = a_bpm;
		tempo = bpm / 60.0;
		
		myClock.tempo_(tempo);
		
		{open.if{bpmText.string_(bpm.asString)};}.defer;
	}
	
	setBeat {|a_beat|
		beat = a_beat;
	}
	
	progress {
		
		var b = beat + (1/8);
		
		open.if {
			
			this.setBeat(b);
			ser = ser + 1;
			
			((clock.seconds - lastTaps.last) > 5).if {
				status = 0;
				{button.value_(0)}.defer;			
			};
		
			myClock.sched(1/8, {
				instance.pr_receiveTick(ser, b, tempo, true);
				this.progress;
			});
		};
	}
	
	deltaBeat {|delta|
		beat = beat + delta;	
	}
	
	tap {
		var newStatus = 0;
		var delta;
		(status == 0).if({
			newStatus = 1;
			
			/*
			delta = beat % 4 + 1;
			if(delta > 2,{
				newBeat = clock.beats - (4-delta);
			},{
				newBeat = clock.beats + delta;
			});
			*/
			
			newBeat = clock.beats.round;
				
			button.value_(1);
			tapNum = 1;
			lastTaps = [clock.seconds];
		}, {
			newStatus = status;
			
			lastTaps = lastTaps.add(clock.seconds);
			newTempo = 1 / (lastTaps.differentiate[1..lastTaps.size-1].sum / (lastTaps.size-1));
			
			
			(tapNum == 3).if {
				this.setBeat(newBeat + 2);
				newStatus = 2;
			};
			
			(tapNum >= 3).if {
				this.setBPM(newTempo * 60);
			};
			
			tapNum = tapNum + 1;
		});
		
		status = newStatus;
		button.value_(status);		
	}
}
