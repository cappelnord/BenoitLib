/*
	MandelClockGUI
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
*/

MandelClockGUI
{
	var window, bpmText, mesText, beatText, bpsText, beatArr, clock,mc;
	var sj;
	var stillOpen = true;
	
	*new {|mc|
		^super.new.init(mc);	
	}
	
	init {|a_mc|
		
		mc = a_mc;		
		clock = mc.clock;
		
		window = Window.new("MandelClockGUI", Rect(400,400,220,60));
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
		
		window.onClose_({this.pr_prepClose;});
		window.front;
		
		this.pr_schedNextBeat;
		
	}
	
	close {
		this.pr_prepClose;
		window.close;	
	}
	
	pr_prepClose {
		sj.stop;
		stillOpen = false;
	}
	
	pr_clearBeats {
		beatArr.do {|item|
			item.background = Color.grey;
		};
	}
	
	// TODO: how can we SkipJack this too but keep total sync with our clock?
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