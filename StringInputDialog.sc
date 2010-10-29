/*
	StringInputDialog
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de
*/

StringInputDialog {

	var window, txt, function;
	
	classvar <>defaultPos = \center;
	
	*new {|title, msg, function, width=400, pos|
		^super.new.init(title, msg, function,width, pos);	
	}
	
	init {|title, msg, a_function, width, pos|
		
		var x;
		var y;
		
		// used to make the text input a little bit larger when using swing
		var swingBigger = 0;
		(GUI.id == \swing).if {
			swingBigger = 7;
		};
		
		function = a_function;
		
		defaultPos.isKindOf(Point).if {
			pos = pos ? defaultPos;	
		};
		
		pos.isNil.if ({
			
			var xPos = 0.5;
			
			(defaultPos == \left).if  { xPos = 1/3};
			(defaultPos == \right).if { xPos = 2/3};
			
			x = Window.screenBounds.width * xPos - (width / 2);
			y = Window.screenBounds.height / 2 - 10;
			
		}, {
			x = pos.x;
			y = pos.y;
		});
		
		window = Window.new(title, Rect( x,
									y,
									width,
									35 + swingBigger), false);
		
		txt = TextField(window, Rect(7,7,width-80,20 + swingBigger)).canFocus_(false);
		
		// the keyDownAction is a little hacky, SCTextField doesn't seem to register Escape, which i neeeed :-(
		// this could be improved some time later ...
		
		Button(window, Rect(width - 67,7,57,20 + swingBigger))
			.states_([[msg,Color.black,Color.clear]])
			.action_({|button| this.doAction;});
			
		window.view.keyDownAction_({ |b, char, modifiers, unicode, keycode|
				
			(char.isPrint).if {
				txt.string_(txt.string ++ char);	
			};
						
			// backspace
			(unicode == 127).if {
				
				(txt.string.size > 1).if({
					txt.string_(txt.string[0..(txt.string.size-2)]);
				},{
					txt.string_("");
				});

			};
			
			// return
			(unicode == 13).if {
				this.doAction;
				this.close;
			};
			
			// escape
			(unicode == 27).if {
				this.close;
			};
				 
		});

				
		window.front;
	}
	
	doAction {
		function.value(txt.string);
		window.close;	
	}
	
	close {
		window.close;	
	}
	
}