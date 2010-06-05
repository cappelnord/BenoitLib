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
	
	*new {|title, msg, function, width=400|
		^super.new.init(title, msg, function,width);	
	}
	
	init {|title, msg, a_function, width|
		function = a_function;
		
		window = Window.new(title, Rect(	Window.screenBounds.width/2 - (width/2),
									Window.screenBounds.height/2 - 10,
									width,
									35), false);
		
		txt = TextField(window, Rect(7,7,width-80,20));
		
		// the keyDownAction is a little hacky, SCTextField doesn't seem to register Escape, which i neeeed :-(
		// this could be improved some time later ...
		
		Button(window, Rect(width - 67,7,57,20))
			.states_([[msg,Color.black,Color.clear]])
			.action_({|button| this.doAction;}).keyDownAction_({ |b, char, modifiers, unicode, keycode|
				
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
				 
	}).focus(true);
		
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