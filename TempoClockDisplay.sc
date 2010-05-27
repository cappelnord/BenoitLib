/*
	messy and a hack, well ...
*/

TempoClockDisplay
{
	var window, bpmText, mesText, beatText, clock;
	var sj;
	
	*new {|clock|
		^super.new.init(clock);	
	}
	
	init {|a_clock|
				
		clock = a_clock;
		
		window = Window.new("TempoClock", Rect(400,400,250,30));
		window.addFlowLayout(10@10,10@10);
		
		bpmText = StaticText(window, 60@20);
		mesText = StaticText(window, 40@20);
		beatText = StaticText(window, 90@20);
		
		sj = SkipJack({
			bpmText.string_((clock.tempo * 60));
			mesText.string_((clock.beats / 4).ceil + 1);
			beatText.string_(clock.beats % 4 + 1);
		},0.1);
		
		window.onClose_({sj.stop;});
		window.front;
		
	}
	
	close {
		sj.stop;
		window.close;	
	}
	
}