// Copyright 2010 Martin Marier

// Features are streams of data extracted from a (musical) interface.  A
// SensorFeature contains the data straight from sensor (not processed), a
// LangFeature processes data on the language side, a SynthFeature
// processes data in the server.

// The input of a SensorFeature is an index of an array. This array is
// expected to contain all the sensor data coming from the interface (the
// Sponge in this case).

// The input of other Features is an array of Features (or an array of size
// one containing a Feature).  This is useful if many inputs are needed as is
// the case when calculating yaw, pitch and roll of an accelerometer.

// .value returns the current value.  .bus returns the bus on which the
// Features puts its data.  interface is an physical interface (the sponge).
// .name is a name (most likely a symbol) for the feature.

Feature {
	classvar <>synthFuncs, <>langFuncs;
	var <>name, <interface, <input, fullFunc, <bus, <>server, <>netAddr,
	<>oscPath, <>action;
	

	*initClass {
		synthFuncs = IdentityDictionary[
			\LP -> {
				arg in0 = 0, freq = 3;
				LPF.kr(In.kr(in0.linexp(0,1023,0,1), 1), freq);
			},
			\HP -> {
				arg in0 = 0, freq = 100;
				HPF.kr(In.kr(in0.linexp(0,1023,0,1), 1), freq);
			}
		];
		langFuncs = IdentityDictionary[
			\atan -> { |data|
				atan2(
					(data[0][0]).linlin(0,1023,-1,1),
					(data[0][1]).linlin(0,1023,-1,1))
			},
			\diff -> { |data|
				data[0][0] - data[0][1]
			},
			\meanMany -> { |data|
				data[0].mean
			},
			\slope -> { |data| 
				(data[0][0] - data[1][0])
			},
			\meanOne -> { |data|
				data.flatten.mean
			},
			\button -> { |data, button|
				// checks if button changed value.
				var changed, val;
				changed = (
					(data[0][0] & (1<<button) != 0 ) !=
					(data[1][0] & (1<<button) != 0 )
				);
				val = (data[0][0] & (1<<button) != 0 ).asInteger;
				[val, changed];
			}
		];
	}

	*sensor { |name, interface, input|
		^SensorFeature.new(name, interface, input)
	}

	*lang { |name, interface, input, function, args|
		^LangFeature.new(name, interface, input, function, args)
	}

	*synth { |name, interface, input, function, args|
		^SynthFeature.new(name, interface, input, function, args)
	}

	remove {
		interface.action_(interface.action.removeFunc(fullFunc));
		interface.features.remove(this);
		interface.featureNames.remove(name);
		bus.free;
	}

	// hack...
	// useful to have Feature respond to .collect
	collect { |f|
		^f.value(this, 0);
	}
}

SynthFeature : Feature {
	var <synth;
	// Function is a UGen Graph Function.  Inputs should be named \in0, \in1,
	// \in2, etc Other args are appended.  User is responsible of scaling the
	// data inside the synth.
	*new { |name, interface, input, function, args|
		^super.newCopyArgs(name, interface, input).init(function, args);
	}

	init { |function, args|
		netAddr = NetAddr.localAddr;
		oscPath = "/sponge/01";
		server = Server.default;
		args = input.collect{|i,j|
			[(\in ++ j).asSymbol, i.bus];
		}.flatten ++ args;

		fork {
			bus = Bus.control(server);
			server.sync;
			synth = function.play(
				target: server,
				outbus: bus,
				fadeTime: 0.02,
				addAction: \addToTail,
				args: args);
			server.sync;
		};

		
		fullFunc = {
			bus.get{|value|
				action.value(value);
				netAddr.sendMsg(oscPath, name, value);
			};
		};
		
		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}

	get { |func|
		func = func ? {|v| v.postln};
		bus.get(func);
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
		netAddr = NetAddr.localAddr;
		oscPath = "/sponge/01";
		server = Server.default;
		bus = Bus.control(server);
		fullFunc = { |...msg|
			value = msg[input];
			action.value(value);
			bus.set(value);
			netAddr.sendMsg(oscPath, name, value);
		};
		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}
}

LangFeature : Feature {
	var <>historySize=10, <inputData, <value;

	*new { |name, interface, input, function, args|
		^super.newCopyArgs(name, interface, input).init(function, args);
	}

	init { |function, args|
		netAddr = NetAddr.localAddr;
		oscPath = "/sponge/01";
		server = Server.default;
		bus = Bus.control(server);
		inputData = Array.fill(historySize, 0);
		// input is a feature: process a feature;
		fullFunc = {
			inputData.addFirst(input.collect(_.value));
			(inputData.size > historySize).if { inputData.pop };
			value = function.value(inputData, *args);
			action.value(value, inputData);
			netAddr.sendMsg(oscPath, name, value);
			bus.set(value);
		};
		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}
}
