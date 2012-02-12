+ NodeProxy {
	// allows the usage of kr NodeProxies in Patterns directly
	asStream {
		^Pkr(this).asStream;
	}
	
	// map to MandelValue
	publish {|to|
		to.trySetSource(this);	
	}
}