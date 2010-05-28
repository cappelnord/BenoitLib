/*
	KeyRecorder
	(c) 2010 by Patrick Borgeat <patrick@borgeat.de>
	http://www.cappel-nord.de
	
	Part of BenoitLib
	http://github.com/cappelnord/BenoitLib
	http://www.the-mandelbrots.de

	This is some kind of Recording Client/Server thing, which allows
	to record small audio snippets with the keyboard.
	
	Last time we used it it failed :-(
*/

KeyRecorder
{
	var isRecording = false;
	
	var mc;
	var name;
	
	var curChar;
	
	var krw;
	
	*new {|net|
		^super.new.init(net);
	}
	
	init {|net|
		
		mc = net;
		name = net.name;
		
		krw = KeyRecorderWindow("Recorder " ++ name);
		
		this.setRecording(false);
		
		krw.keyAction_({|view, char, mod|
			
			var button;
			// mod.postln;
			char = char.toLower;
			
			((char.ascii >= 97) && (char.ascii <= 122) && (mod < 1024)).if {
				button = krw.buttonDict.at(char);
				
				isRecording.not.if ({
					
					button.background = Color.red;
					net.sendMsg("/kr/record", name, char.asString);
					curChar = char;
					this.setRecording(true);
				},{
					(char == curChar).if {
						button.background = Color.green;
						net.sendMsg("/kr/stop", name);
						curChar = nil;
						this.setRecording(false);
					};
				});
			};
			
		});
		
		krw.updateAction_({this.requestStatus});
		
		this.requestStatus();
	}
		
	setRecording {|bool|
	
		isRecording = bool;
				
		bool.not.if {
			krw.setStatus("Standing By ...", Color.grey);
		}{
			krw.setStatus("Recording!", Color.red);
		};
	}
	
	requestStatus {
	
		isRecording.not.if {
			krw.setStatus("Waiting for recorded keys ...", Color.blue);
			mc.sendMsg("/kr/status", name);
			
			krw.buttonDict.do {|button|
				button.background = Color.grey;	
			};
			
			OSCresponder(nil, "/kr/recorded", {|time, theResponder, message, addr|
					
				{	
				message.postcs;
				(message[2].asString == name).if {
							
					message.do{|s, i|
				
						(i > 2).if {
							krw.buttonDict.at(s.asString[0]).background = Color.green;
						};
					};
				};
				
				this.setRecording(false);
			}.defer;
				
			}).add.removeWhenDone;
		};
	}
}

KeyRecorderWindow
{
	var window;
	  
	var <buttonDict;
	
	var <status;
	
	var <keyAction;
	var <updateAction;
	
	var updateButton;
	
	*new {|name, keyboardLayout=\germany|
		^super.new.init(name, keyboardLayout);
	}
	
	
	init {|name, keyboardLayout=\germany|

		var keyTable = (germany: #[
		[$q, $w, $e, $r, $t, $z, $u, $i, $o, $p],
		 [$a, $s, $d, $f, $g, $h, $j, $k, $l],
		  [$y, $x, $c, $v, $b, $n, $m]],
		neo2: #[
		[$x, $v, $l, $c, $w, $k, $h, $g, $f, $q],
		 [$u, $i, $a, $e, $o, $s, $n, $r, $t, $d, $y],
		  [$_, $_, $_, $p, $z, $b, $m, $_, $_, $j]]
		).at(keyboardLayout);

		var rowSizes = keyTable.collect({ |item,i| item.size});
		var maxRowSize = rowSizes.copy.sort.last;
		var winWidth = (maxRowSize * 35) + (20 * (rowSizes.indicesOfEqual(maxRowSize).last + 1));

		buttonDict = Dictionary.new;

		window = Window.new("Key " ++ name, Rect(100,100,winWidth,145));
		window.view.decorator = FlowLayout(Rect(0,0,winWidth,145), 5@5, 5@5);
		
		keyTable.do {|list, i|
			
			CompositeView.new(window,Point(15*i+1,25)); // silly spacing
			
			list.do{|char|
				if(char == $_, {
					CompositeView.new(window,Point(30,30)); // silly spacing
				},
				{
					var button = StaticText.new(window,30@30);
					button.string = "    " ++ char.toUpper.asString;
					button.background = Color.grey;
				
					buttonDict.add(char -> button);
				});
			};
			window.view.decorator.nextLine;	
		};
		
		status = StaticText.new(window, 280@30);	
		
		updateButton = Button.new(window,70@30);
		updateButton.states_([["Update", Color.black, Color.grey]]);
		updateButton.canFocus_(false);	
		
		window.front;
	}
	
	setStatus {|string, color|
		status.string = "   " ++ string;
		status.background = color;
	}
	
	setButtonColor {|char, color|
		buttonDict.at(char).background = color;	
	}
	
	// view, char
	keyAction_ {|func|
			
		window.view.removeAction(keyAction, \keyDownAction);
		
		keyAction = func;
		window.view.addAction(func,\keyDownAction);

	}
	
	updateAction_ {|func|
	
		updateAction = func;
		updateButton.action_(func);
		
	}
}

KeyRecorderServer {
	
	var mc;
	var sname;
	
	var path;
	
	var delaySynth;
	var delayBus;
	
	var delay;
	var fadeTime;
	
	var group;
	
	var dict;
	
	var serialNumber = 0;
	

	*new {|net, path, delay=0.2, fadeTime=0.001|
		^super.new.init(net,path, delay, fadeTime);
	}
	
	init {|net, a_path, a_delay, a_fadeTime|
		
		mc = net;
		path = a_path;
		sname = mc.name;
		
		delay = a_delay;
		fadeTime = a_fadeTime;
		
		dict = Dictionary.new;
		
		Server.default.waitForBoot({
			
			fork {
			
				SynthDef(\krsDelay, {|in=0, out=0, delay=0.2|
					
					var sig = DelayC.ar(Mix.ar(SoundIn.ar([0,1]) * 0.5), delay, delay);
					Amplitude.kr(sig).poll; // stupid monitoring
					
					Out.ar(out, sig);
					
				}).add;
				
				SynthDef(\krsRecord, {|in=0, fadeTime = 0.001, gate=1, buffer=0|
					
					var env = EnvGen.ar(Env.asr(fadeTime, 1, fadeTime), gate, doneAction:2);
					var sig = In.ar(in, 1);
					(Amplitude.kr(sig) * -1).poll; // stupid monitoring
					DiskOut.ar(buffer, In.ar([in]) * env);
					
					Line.kr(0,100,20,doneAction:2);
					
				}).add;
			
				group = Group.new;
				delayBus = Bus.audio(Server.default, 1);
			
				1.wait;
				"Starting Audio ...".postln;
				
				delaySynth = Synth.head(group, \krsDelay, [out: delayBus, delay:delay]);
				
				0.25.wait;
				
				"Starting Server ...".postln;
				
				this.pr_addResponders;
			
			};
		});
		
	}
	
	userPath {|name|
		var p = path ++ name ++ "/";
		
		(p.pathMatch.size < 1).if {
			("mkdir "++p).postln.unixCmd;
		};
		
		^p;	
	}
	
	
	sendRecorded {|name|
	
		var files = (this.userPath(name) ++ "*.aif").pathMatch;
		
		var list = List.new;
		
		files.do {|file|
			list.add(file.basename[0].asString);	
		};
		
		list = list.asArray;
		
		mc.sendMsg("/kr/recorded",sname, name, *list); 
		
	}
	
	userDict {|name|
		
		var ret = dict.at(name);
		
		ret.isNil.if {
			ret = (node: nil, buffer: nil, recording:false, name: name);
			dict.add(name -> ret);
			
		};
		
		^ret;
	}
	
	pr_addResponders {
		
		OSCresponder(nil, "/kr/status", {|time, theResponder, message, addr|
			(message[1] ++ " requested his recorded files.").postln;
			this.sendRecorded(message[1]);
		}).add;
		
		OSCresponder(nil, "/kr/record", {|time, theResponder, message, addr|
			
			var name = message[1].asString;
			var fileName = (this.userPath("_temp") ++ name ++ "-" ++ serialNumber ++ ".aif");
			var dict = this.userDict(name);
			
			serialNumber = serialNumber + 1;

			(name ++ " started recording ...").postln;
			
			dict.recording.if {
				this.stop(dict);	
			};
			
			this.record(dict, fileName, message[2].asString);
			
		}).add;	
		
		OSCresponder(nil, "/kr/stop", {|time, theResponder, message, addr|
			
			var name = message[1].asString;
			var dict = this.userDict(name);
			
			(name ++ " stopped recording ...").postln;
			
			dict.recording.if {
				this.stop(dict);	
			};
			
		}).add;
	}
	
	record {|dict, fileName, char|
						
		dict.buffer = Buffer.alloc(Server.default, 4096, 1);
		dict.buffer.write(fileName, "aiff", "int24", 0, 0, true);
		dict.node = Synth.after(delaySynth, \krsRecord, [in:delayBus, buffer:dict.buffer,fadeTime:fadeTime]);
		
		dict.char = char;
		dict.fileName = fileName;
			
		dict.recording = true;
	}
	
	stop {|dict|
		
		var buffer, fileName;
		
		var finalFileName = (this.userPath(dict.name) ++ dict.char ++ ".aif");
		
		buffer = dict.buffer;
		fileName = dict.fileName;
		
		dict.node.set(\gate, 0);
		dict.node = nil;
		dict.buffer = nil;
		dict.char = nil;
		dict.fileNam = nil;
		dict.recording = false;
		
		fork {
			(fadeTime * 8).wait;
			buffer.close;
			0.5.wait;
			buffer.free;
			0.5.wait;
			("mv " ++ fileName ++ " " ++ finalFileName).postln.unixCmd;	
		};
	}	
}