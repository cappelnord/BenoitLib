/*
	messy and a hack, well ...
*/

TempoClockDisplay
{
	var window, bpmText, mesText, beatText, bpsText, beatArr, clock;
	var sj;
	var stillOpen = true;
	
	*new {|clock|
		^super.new.init(clock);	
	}
	
	init {|a_clock|
				
		clock = a_clock;
		
		window = Window.new("Clock Display", Rect(400,400,220,60));
		window.addFlowLayout(10@10,5@2);
		
		bpmText = StaticText(window, 40@20);
		StaticText(window,45@20).string_("BPM");
		mesText = StaticText(window, 25@20);
		beatText = StaticText(window, 70@20);
		
		window.view.decorator.nextLine;
		
		bpsText = StaticText(window, 40@20);
		StaticText(window,45@20).string_("BPS");
		
		beatArr = nil.dup(4);
		
		4.do {|i|
			beatArr[i] = StaticText(window,20@20);
		};
		
		this.pr_clearBeats;
		
		sj = SkipJack({
			bpmText.string_((clock.tempo * 60));
			bpsText.string_(clock.tempo);
			beatText.string_(clock.beats % 4 + 1);
		},0.1);
		
		window.onClose_({sj.stop;stillOpen=false;});
		window.front;
		
		this.pr_schedNextBeat;
		
	}
	
	close {
		sj.stop;
		window.close;	
	}
	
	pr_clearBeats {
		beatArr.do {|item|
			item.background = Color.grey;
		};
	}
	
	pr_schedNextBeat {
		var nextBeat = clock.beats.ceil;
		var color = Color.green;
		
		((nextBeat % 4) == 0).if {color = Color.red;};
		
		clock.schedAbs(nextBeat, {
			stillOpen.if {
				{
					this.pr_clearBeats;
					beatArr[nextBeat%4].background = color;
					mesText.string_((clock.beats / 4).ceil);
					this.pr_schedNextBeat;
				}.defer;
			};
		});	
	}
	
}