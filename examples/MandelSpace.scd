m = MandelClock.startLeader("Patrick");

m.space.setValue(\a, 10)
m.space.setDecorator(\a, {|v| v * 10})
m.space.getValue(\a)

m.space.setDecorator(\x, {|v, space| space.getValue(\a) - 1})
m.space.getValue(\x)

m.space.addListener(\a, {|v| ("A: " ++ v).postln;})
m.space.setValue(\a, 15)

m.space.addListener(\x, {|v| ("X: " ++ v).postln;})
m.space.addSubscriber(\a, \x);

m.space.setValue(\a, 5)