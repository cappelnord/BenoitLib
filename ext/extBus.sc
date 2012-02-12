+ Bus {
	trySetSynchronous {|value|
		var doSynch = false;
		// wait for fix in 3.5 packages
		// doSynch = try {this.server.hasShmInterface;}; 
		value.postln;
		doSynch.if ({
			this.setSynchronous(value);
		}, {
			this.set(value);
		});	
	}		
}