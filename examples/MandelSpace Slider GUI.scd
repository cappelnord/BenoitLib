// be sure to be in a MandelClock

(
var space = MandelClock.instance.space;
var sliderWidth = 30;
var sliderHeight = 200;
var num = 10;
var keys = num.collect {|i|("ctrl_" + i).asSymbol};
var window;

// initialize values, if not allready present
keys.do {|key| space[key].isNil.if {space[key] = 0.0;};};

window = Window("MandelSpace Slider Test", Rect(100, 100, num*sliderWidth, sliderHeight + 40));

keys.do {|key, i|
	var slider = Slider(window,Rect(sliderWidth*i, 10, sliderWidth, sliderHeight));
	StaticText(window, Rect(sliderWidth*i, sliderHeight+20, sliderWidth, 20))
		.string_(i.asString)
		.align_(\center);
	
	slider.value = space[key];
	slider.action = {space[key] = slider.value};
	space.getObject(key).addDependant({|changer, key, value|
		{slider.value = value;}.defer;
	});
};

window.front;
)