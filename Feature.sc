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
	classvar <>synthDefs, <>funcs;
	var <>name, <interface, <input, fullFunc, <bus, <>server, <>netAddr,
	<oscPath, <>action, <>dependantFeatures;
	

	*initClass {
		synthDefs = IdentityDictionary[
			\LP -> SynthDef(\featureLP, {
				arg out=0, in0 = 0, freq = 3;
				Out.kr(
					out, LPF.kr(In.kr(in0, 1), freq)
				);
			}, metadata:( specs:(freq:[0.01, 100, \exp]) ) ),
			\HP -> SynthDef(\featureHP, {
				arg out=0, in0 = 0, freq = 100;
				Out.kr(
					out, HPF.kr(In.kr(in0, 1), freq)
				);
			}, metadata:( specs:(freq:[1,1000,\exp]) ) )
		];
		funcs = IdentityDictionary[
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
	
	init {
		server = try{input[0].server} ? Server.default;
		server.serverRunning.not.if {
			("server '" ++ server.name ++ "' not running.\nFeature '" ++
				name ++ "' may not work as expected.").warn;
		};
		netAddr = try{input[0].netAddr} ? NetAddr.localAddr;
		oscPath = try{input[0].oscPath.dirname.withTrailingSlash} ? "/sponge/01/";
		oscPath = oscPath ++ name.asString;
		dependantFeatures = List[];
	}

	oscPath_ { |path|
		oscPath = path;
		this.changed(\oscPath, path);
	}

	oscAddr_ { |addr|
		netAddr.hostname_(addr);
		this.changed(\oscAddr, addr);
	}

	oscPort_ { |port|
		netAddr.port_(port);
		this.changed(\oscPort, port);
	}

	remove {
		interface.action_(interface.action.removeFunc(fullFunc));
		interface.features.remove(this);
		interface.featureNames.remove(name);
		bus.free;
		dependantFeatures.do(_.remove); // remove features that depend on this one.
	}

	// hack...
	// useful to have Feature respond to .collect
	collect { |f|
		^f.value(this, 0);
	}

	guiClass { ^FeatureGui }
}

SynthFeature : Feature {
	var <synth, <def;
	// Inputs of SynthDef should be named \in0, \in1,
	// \in2, etc Other args are appended.  User is responsible for scaling the
	// data inside the synth.
	*new { |name, interface, input, synthDef, args|
		^super.newCopyArgs(name, interface, input).init(synthDef, args);
	}

	init { |synthDef, arguments|
		super.init;
		server.serverRunning.not.if {
			("Synth features need a server to work properly.").warn;
			("Feature '" ++ name ++ "' will not be activated.").warn;
			^nil;
		};
		def = synthDef;
		arguments = input.collect{|i,j|
			i.dependantFeatures.add(this);
			[(\in ++ j).asSymbol, i.bus.index];
		}.flatten ++ arguments;

		fork {
			bus = Bus.control(server);
			def.add;
			server.sync;
			synth = Synth.tail(
				server,
				def.name,
				args: [\out, bus.index] ++ arguments);
			server.sync;
		};

		
		fullFunc = {
			bus.get{|value|
				action.value(value);
				netAddr.sendMsg(oscPath, value);
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

	// set a parameter of the synth.
	set { |argName, value|
		synth.set(argName, value);
	}

	// getParameters {
	// 	var res;
	// 	res = List[];
	// 	forBy(0, args.size-2, 2) {|i|
	// 		res.add(args[i]);
	// 	};
	// 	^res;
	// }

	remove {
		super.remove;
		input.do({|i| i.dependantFeatures.remove(this); }); // remove myself from the
												 // dependantFeatures list of others.
		synth.free;
	}
}

SensorFeature : Feature { // the raw data from the sensor
	var <value;

	*new { |name, interface, input|
		^super.newCopyArgs(name, interface, input).init;
	}

	init { 
		super.init;
		bus = Bus.control(server);
		fullFunc = { |...msg|
			value = msg[input];
			action.value(value);
			bus.set(value);
			netAddr.sendMsg(oscPath, value);
		};
		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}

	remove {
		"\'%\' is a sensor feature and cannot be removed.\n".postf(name);
	}
}

LangFeature : Feature {
	var <>historySize=10, <inputData, <value;

	*new { |name, interface, input, function, args|
		^super.newCopyArgs(name, interface, input).init(function, args);
	}

	init { |function, args|
		super.init;
		bus = Bus.control(server);
		inputData = Array.fill(historySize, 0);
		// input is a feature: process a feature;
		input.do({ |i| i.dependantFeatures.add(this) });
		fullFunc = {
			inputData.addFirst(input.collect(_.value));
			(inputData.size > historySize).if { inputData.pop };
			value = function.value(inputData, *args);
			action.value(value, inputData);
			netAddr.sendMsg(oscPath, value);
			bus.set(value);
		};
		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}
	
	remove {
		super.remove;
		input.do({|i| i.dependantFeatures.remove(this); }); // remove myself from the
												 // dependantFeatures list of others.
	}

}
