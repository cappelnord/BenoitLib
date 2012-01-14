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
	var <window, bpmText, mesText, beatText, bpsText, beatArr, clock,mc;
	var sj;
	var stillOpen = true;
	
	classvar <>defaultPos;
	
	*new {|mc, pos|
		^super.new.init(mc,pos);	
	}
	
	init {|a_mc, pos|
		
		mc = a_mc;		
		clock = mc.clock;
		
		// well, 400@400 is quite random :-)
		pos = pos ? defaultPos ? (400@400);
		
		window = Window.new("MandelClockGUI", Rect(pos.x,pos.y,288,65), false);
		window.addFlowLayout(10@10,5@5);
		
		bpmText = StaticText(window, 50@20);
		StaticText(window,45@20).string_("BPM");
		mesText = StaticText(window, 45@20);
		beatText = StaticText(window, 45@20);
		
		CompositeView.new(window,Point(10,20)); // silly spacing

		Button(window,45@20)
			.states_([["Chat", Color.black, Color.clear]])
			.action_({mc.chatWindow;});
		
		window.view.decorator.nextLine;
		
		bpsText = StaticText(window, 50@20);
		StaticText(window,45@20).string_("BPS");
		
		beatArr = nil.dup(4);
		
		4.do {|i|
			beatArr[i] = StaticText(window,20@20);
		};
		
		CompositeView.new(window,Point(10,20)); // silly spacing
		
		Button(window,45@20)
			.states_([["Shout", Color.black, Color.clear]])
			.action_({mc.shoutWindow;});
		
		this.pr_clearBeats;
		
		sj = SkipJack({
			
			var tempo = mc.externalTempo;
			var color = Color.black;
			
			MandelClock.debug.if {
				tempo = mc.tempo;
			};
			
			(mc.tempo > mc.externalTempo).if {
				color = Color(0,0.4,0);
			};
			
			(mc.tempo < mc.externalTempo).if {
				color = Color(0.4,0,0);
			};
			
			bpmText.stringColor_(color);
			bpsText.stringColor_(color);
			
			bpmText.string_((tempo * 60).asString[0..5]);
			bpsText.string_(tempo.asString[0..5]);
			
			beatText.string_((clock.beats % 4 + 1).asString[0..4]);
			
		},0.1);
		
		window.onClose_({this.pr_prepClose;});
		window.front;
		
		this.pr_addCmdPeriod ;
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
	
	pr_schedNextBeat {
		var nextBeat = clock.beats.ceil;
		var color = Color.green;
		
		((nextBeat % 4) == 0).if {color = Color.red;};
				
		clock.schedAbs(nextBeat, {
			stillOpen.if {
				{
					this.pr_clearBeats;
					beatArr[nextBeat%4].background = color;
					mesText.string_((clock.beats / 4).floor);
					this.pr_schedNextBeat;
				}.defer;
			};
		});	
	}
	
	pr_addCmdPeriod {
		stillOpen.if {
			CmdPeriod.doOnce({
				this.pr_addCmdPeriod;
				this.pr_schedNextBeat;
			});
		};	
	}	
}