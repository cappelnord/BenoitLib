/*
	MandelGUI
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
*/

MandelGUI
{
	var <window, bpmText, mesText, beatText, bpsText, beatArr, clock, hub;
	var sj;
	var stillOpen = true;
	
	classvar <>defaultPos;
	
	*new {|hub, pos|
		^super.new.init(hub ,pos);	
	}
	
	init {|a_hub, pos|
		
		hub = a_hub;		
		clock = hub.clock;
		
		// well, 400@400 is quite random :-)
		pos = pos ? defaultPos ? (400@400);
		
		window = Window.new("MandelGUI", Rect(pos.x,pos.y,288,65), false);
		window.addFlowLayout(10@10,5@5);
		
		bpmText = StaticText(window, 50@20);
		StaticText(window,45@20).string_("BPM");
		mesText = StaticText(window, 45@20);
		beatText = StaticText(window, 45@20);
		
		CompositeView.new(window,Point(10,20)); // silly spacing

		Button(window,45@20)
			.states_([["Chat", Color.black, Color.clear]])
			.action_({hub.chatWindow;});
		
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
			.action_({hub.shoutWindow;});
		
		this.prClearBeats;
		
		sj = SkipJack({
			
			var tempo = hub.externalTempo;
			var color = Color.black;
			
			MandelHub.debug.if {
				tempo = hub.tempo;
			};
			
			(hub.tempo > hub.externalTempo).if {
				color = Color(0,0.4,0);
			};
			
			(hub.tempo < hub.externalTempo).if {
				color = Color(0.4,0,0);
			};
			
			bpmText.stringColor_(color);
			bpsText.stringColor_(color);
			
			bpmText.string_((tempo * 60).asString[0..5]);
			bpsText.string_(tempo.asString[0..5]);
			
			beatText.string_((clock.beats % 4 + 1).asString[0..4]);
			
		},0.1);
		
		window.onClose_({this.prPrepClose;});
		window.front;
		
		this.prAddCmdPeriod ;
		this.prSchedNextBeat;
		
	}
	
	
	close {
		this.prPrepClose;
		window.close;	
	}
	
	prPrepClose {
		sj.stop;
		stillOpen = false;
	}
	
	prClearBeats {
		beatArr.do {|item|
			item.background = Color.grey;
		};
	}
	
	prSchedNextBeat {
		var nextBeat = clock.beats.ceil;
		var color = Color.green;
		
		((nextBeat % 4) == 0).if {color = Color.red;};
				
		clock.schedAbs(nextBeat, {
			stillOpen.if {
				{
					this.prClearBeats;
					beatArr[nextBeat%4].background = color;
					mesText.string_((clock.beats / 4).floor);
					this.prSchedNextBeat;
				}.defer;
			};
		});	
	}
	
	prAddCmdPeriod {
		stillOpen.if {
			CmdPeriod.doOnce({
				this.prAddCmdPeriod;
				this.prSchedNextBeat;
			});
		};	
	}	
}