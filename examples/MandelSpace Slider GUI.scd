// be sure to be in a MandelClock !!!

(
var space = MandelClock.instance.space;
var sliderWidth = 30;
var sliderHeight = 200;
var num = 10;
var keys = num.collect {|i|("ctrl" ++ i).asSymbol};
var window;
var actions = List.new;

// initialize values, if not allready present
keys.do {|key| space[key].isNil.if {space[key] = 0.0;};};

window = Window("MandelSpace Slider Test", Rect(100, 100, num*sliderWidth, sliderHeight + 70));

keys.do {|key, i|
	var slider = Slider(window,Rect(sliderWidth*i, 10, sliderWidth, sliderHeight));
	var who, action;
	
	StaticText(window, Rect(sliderWidth*i, sliderHeight+20, sliderWidth, 20))
		.string_(i.asString)
		.align_(\center);
		
	who = StaticText(window, Rect(sliderWidth*i, sliderHeight+50, sliderWidth, 20))
		.string_(space.getObject(key).setBy[0].toUpper.asString)
		.align_(\center);
				
	
	slider.value = space[key];
	slider.action = {
		space[key] = slider.value;
		who.string_(space.mc.name[0].toUpper.asString);
	};
	
	action = {|changer, key, value|
		{
			slider.value = value;
			who.string_(changer.setBy[0].toUpper.asString);
		}.defer;
	};
	space.getObject(key).addDependant(action);
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
// Use the ProxySpace Publishing to make yourself heard!
// (ProxySpace must have been pushed)
(
var num = 10;
var keys = num.collect {|i|("ctrl" ++ i).asSymbol};
var nps = keys.collect {|key| MandelClock.instance.space.getObject(key).mapToProxySpace(0.1)};

~yeah = {Splay.ar(SinOsc.ar(nps.collect{|np| np.kr.linexp(0,1,100,5000)}))};
~yeah.play;

)

*/