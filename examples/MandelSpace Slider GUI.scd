// be sure to be in a MandelClock !!!

(
var space = MandelClock.instance.space;
var sliderWidth = 30;
var sliderHeight = 200;
var num = 10;
var keys = num.collect {|i|("ctrl" ++ i).asSymbol};
var window;
var actions = List.new;

x = space; 

// initialize values, if not allready present
keys.do {|key| space.getValue(key).isNil.if {space[key] = 0.0;};};

window = Window("MandelSpace Slider Test", Rect(100, 100, num*sliderWidth, sliderHeight + 60));

keys.do {|key, i|
	var slider = Slider(window,Rect(sliderWidth*i, 10, sliderWidth, sliderHeight));
	var who, action, obj, setSetBy;
	
	StaticText(window, Rect(sliderWidth*i, sliderHeight+20, sliderWidth, 20))
		.string_(i.asString)
		.align_(\center);
		
	who = StaticText(window, Rect(sliderWidth*i, sliderHeight+40, sliderWidth, 20))
		.align_(\center);
		
	obj = space.getObject(key);
	
	setSetBy = {who.string_(obj.setBy[0].toUpper.asString);};
					
	slider.action = {
		space.setValue(key, slider.value, strategy: \stream);
		setSetBy.value();
	};
	
	action = {
		{
			slider.value = obj.getValue();
			setSetBy.value();
		}.defer;
	};
	
	action.value();
	
	obj.addDependant(action);
	actions.add([key, action]);
};

// clean up
window.onClose = {
	actions.do {|item|
		space.getObject(item[0]).removeDependant(item[1]);
	};	
};

window.front;
)

/*
// Make a nice movement

(
{
	var num = 10;
	var keys = num.collect {|i|("ctrl" ++ i).asSymbol};
	(0, 0.1..8pi).do {|phi|
		keys.do {|key, i|
			var phase = (i/num) * pi;
			x[key] = sin(phi + phase) + 1.0 * 0.5;
			0.01.wait;
		};
	};
}.fork;

)
*/

/*
// Make sound with it!
// (ProxySpace must have been pushed)
(
var num = 10;
var keys = num.collect {|i|("ctrl" ++ i).asSymbol};

~yeah = {Splay.ar(SinOsc.ar(keys.collect{|key| x[key].kr.lag2(0.2).linexp(0,1,100,5000)}))};
~yeah.play;

)

*/

/*
// map a NodeProxy to a value

~kr = {LFNoise2.kr(2).abs};
~kr <>> x[\ctrl0];

// Stop it!
x[\ctrl0].unmap;

*/

/*
// map values to a NodeProxy

~snd = {|freq, mod| var f = freq.linexp(0, 1, 220, 880); SinOsc.ar(f + SinOsc.ar(f*mod, mul:300))}
~snd.play

x[\ctrl0] <>>.freq ~snd
x[\ctrl1] <>>.mod  ~snd

*/