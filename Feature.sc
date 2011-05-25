// Copyright 2010 Martin Marier

// Features are streams of data extracted from a (musical) interface.  A
// SensorFeature contains the data straight from sensor (not processed), a
// LangFeature processes data on the language side, a SynthFeature
// processes data in the server.

// The input of a SensorFeature is an index of an array. This array is
// expected to contain all the sensor data coming from the interface (the
// Sponge in this case).

// The input of other Features is a Feature or an array of Features.  This is
// useful if the Feature needs many inputs as is the case when calculating
// yaw, pitch and roll of an accelerometer.

// .value returns the current value.
// .bus returns the bus on which the Features puts its data.
// interface is an physical interface (the sponge).
// .name is a name (most likely a symbol) fro the feature.


Feature {
	var <>name, <interface, <input, fullFunc, <bus;

	*sensor { |name, interface, input|
		^SensorFeature.new(name, interface, input)
	}

	*lang { |name, interface, input, function|
		^LangFeature.new(name, interface, input, function)
	}

	*synth { |name, interface, input, function|
		^SynthFeature.new(name, interface, input, function)
	}

	remove {
		interface.action_(interface.action.removeFunc(fullFunc));
		interface.features.remove(this);
		interface.featureNames.remove(name);
		bus.free;
	}

	collect { |f|
		^f.value(this);
	}
}

SynthFeature : Feature {
	var <>server, <synth;
	// Function is a UGen Graph Function.  Inputs should be named \in0, \in1,
	// \in2, etc  Other args are appended
	*new { |name, interface, input, function, args|
		^super.newCopyArgs(name, interface, input).init(function, args);
	}

	init { |function, args|
		server = Server.default;
		args = input.collect{|i|
			[(\in ++ i).asSymbol, input.bus];
		}.flatten ++ args;
		
		fork {
			bus = Bus.control(server);
			server.sync;
			synth = function.play(server, bus, addAction:\addToTail, args:args);
			server.sync;
		};

		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}

	value {
		var val;
		this.bus.get{|v| val = v};
		^val;
	}

	remove {
		interface.action_(interface.action.removeFunc(fullFunc));
		interface.features.remove(this);
		interface.featureNames.remove(name);
		bus.free;
		synth.free;
	}
	
}

SensorFeature : Feature { // the raw data from the sensor
	var <value;

	*new { |name, interface, input|
		^super.newCopyArgs(name, interface, input).init;
	}
	
	init { 
		fullFunc = { |...msg|
			value = msg[input];
		};
		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}
}

LangFeature : Feature {
	var <>historySize=10, <inputData, <value;

	*new { |name, interface, input, function|
		^super.newCopyArgs(name, interface, input).init(function);
	}

	init { |function|
		inputData = Array.newClear(historySize);
		// input is a feature: process a feature;
		fullFunc = {
			inputData.addFirst(input.collect(_.value));
			(inputData.size > historySize).if { inputData.pop };
			value = function.value(inputData);
		};
		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}
}
