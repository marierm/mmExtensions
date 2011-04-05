// Copyright 2010 Martin Marier

SpongeBee {
	var <port, inputThread, <sensorNames, spec10bits;
	var <>action, <bus, <>busses, <>synths, busAction;
	var <dn, <server, <name, <>features;

	*new { arg portName="/dev/ttyUSB0", baudRate=19200;
		^super.new.init(portName, baudRate);
	}
	init { arg pn, br;
		port = SerialPort(pn, br);
		inputThread= fork {
			// SPONGEBEE BYTE PARSING
			// ======================
			// 
			// Each sensor is 10 bits.
			// 10 bit values are encoded on 3 bytes.
			// First byte is status byte : 00000xxx
			// where xxx is input sensor number
			// 2nd and third bytes are data bytes : 1xxxxxxx and 10000xxx
			// 7 bit MSB on first data byte and 3 bit LSB on second one.
			// Last status byte (00001000) (8) has only one data byte:
			// the 7 buttons.
			var data, byte, msb, lsb, id, count=0;
			data = Array.fill(9,0);
			loop {
				byte = port.read;
				while {(byte >> 7) != 0} { // read until we find a status byte
					byte = port.read;
				};
				id = byte % 128;				// get sensor number
				if (id == 8) { // if it is the button's data
					msb = port.read % 128;
					data[id] = msb;
					action.value(*data);
				} {	// if it is the data of one of the sensors
					msb = port.read % 128;
					lsb = port.read % 8;
					data[id] = (msb << 3) + lsb;
				};
			}
		};
		spec10bits = ControlSpec(0, 1023, 'linear', 0.0, 512, "");
		sensorNames = [\acc1x, \acc1y, \acc1z, \acc2x, \acc2y, \acc2z,
			\fsr1, \fsr2, \buttons];
		busses = IdentityDictionary.new();
		synths = IdentityDictionary.new();
	}

	createBus { |s|
		server = s ? Server.default;

		if ( bus.isNil) {
			bus = Bus.control( s, 9 );
		};
		// To be able to reference the channels by name :
		sensorNames.do { |i, ii| busses[i] = bus.subBus(ii,1) };
		busAction = { |...msg|
			msg = msg.collect({|i,j| spec10bits.unmap(i)});
			bus.set(*msg);
		};
		action = action.addFunc(busAction);
	}

	createAllFeatures {
		var synthDefs;
		// check for server existence.
		//[featureName, SynthDef, [settings]]
		synthDefs = Dictionary[
			\hiPass -> SynthDef( \hiPass, { arg out=0, in=1, gate=1, freq=100;
				EnvGen.kr( Env.cutoff( 1 ), gate, doneAction: 2 );
				Out.kr(
					out,
					HPF.ar(K2A.ar(In.kr(in, 1)), freq);
				);
			}),
			\loPass -> SynthDef( \loPass, { arg out=0, in=1, gate=1, freq=3;
				EnvGen.kr( Env.cutoff( 1 ), gate, doneAction: 2 );
				Out.kr(
					out,
					LPF.kr(In.kr(in, 1), freq);
				);
			}),
			\atan -> SynthDef( \atan, { arg out=0, in=1, gate=1, in2;
				var input1, input2;
				// expects values between 0 and 1.
				input1 = (In.kr(in, 1)*2) -1;
				input2 = (In.kr(in2, 1)*2) -1;
				EnvGen.kr( Env.cutoff( 1 ), gate, doneAction: 2 );
				Out.kr(
					out,					// outputs angle between 0 and 1.
					(atan2(input1, input2) / (2 * pi) ) + 0.5
				);
			}),
			\difference -> SynthDef( \difference, {
				arg out=0, in=1, gate=1, in2, freq=3;
				var input1, input2;
				input1 = In.kr(in, 1);
				input2 = In.kr(in2, 1);
				EnvGen.kr( Env.cutoff( 1 ), gate, doneAction: 2 );
				Out.kr(
					out,
					LPF.kr((input1 - input2), freq, 0.5, 0.5);
				);
			}),
			\slope -> SynthDef( \slope, { arg out=0, in=1, gate=1;
				var input;
				EnvGen.kr( Env.cutoff( 1 ), gate, doneAction: 2 );
				input = In.kr( in, 1);
				Out.kr( out,
					Slope.kr( input )
				);
			}),
			\schmidt -> SynthDef(\schmidt, { | in=0, lo=0.1, hi=0.3, id = -1|
				var sig, input;
				input = In.kr(in,1);
				sig = Schmidt.kr(
					input.abs,lo, hi
				);
				SendReply.kr(sig, "/schmidt", input, id);
			})
		];

		features = [
			Feature(\acc1xLP, synthDefs[\loPass], this, [\in, this.at(\acc1x)]),
			Feature(\acc1yLP, synthDefs[\loPass], this, [\in, this.at(\acc1y)]),
			Feature(\acc1zLP, synthDefs[\loPass], this, [\in, this.at(\acc1z)]),
			Feature(\acc2xLP, synthDefs[\loPass], this, [\in, this.at(\acc2x)]),
			Feature(\acc2yLP, synthDefs[\loPass], this, [\in, this.at(\acc2y)]),
			Feature(\acc2zLP, synthDefs[\loPass], this, [\in, this.at(\acc2z)]),
			Feature(\fsr1LP, synthDefs[ \loPass], this, [\in, this.at(\fsr1)]),
			Feature(\fsr2LP, synthDefs[ \loPass], this, [\in, this.at(\fsr2)]),
			Feature(\acc1xHP, synthDefs[\hiPass], this, [\in, this.at(\acc1x)]),
			Feature(\acc1yHP, synthDefs[\hiPass], this, [\in, this.at(\acc1y)]),
			Feature(\acc1zHP, synthDefs[\hiPass], this, [\in, this.at(\acc1z)]),
			Feature(\acc2xHP, synthDefs[\hiPass], this, [\in, this.at(\acc2x)]),
			Feature(\acc2yHP, synthDefs[\hiPass], this, [\in, this.at(\acc2y)]),
			Feature(\acc2zHP, synthDefs[\hiPass], this, [\in, this.at(\acc2z)]),
			Feature(\fsr1HP, synthDefs[ \hiPass], this, [\in, this.at(\fsr1)]),
			Feature(\fsr2HP, synthDefs[ \hiPass], this, [\in, this.at(\fsr2)]),
			// Feature(\pressBoth, synthDefs[\mean], this),
			Feature(\pressDiff, synthDefs[\difference], this),
			Feature(\fsr1speed, synthDefs[\slope], this, [\in, this.at(\fsr1)]),
			Feature(\fsr2speed, synthDefs[\slope], this, [\in, this.at(\fsr2)]),
			Feature(\pitch1, synthDefs[\atan], this, [
		 		\in, this.at(\acc1x),
		 		\in2, this.at(\acc1z)]),
			Feature(\pitch2, synthDefs[\atan], this,
				[ \in, this.at(\acc2x), \in2, this.at(\acc2z)]),
			Feature(\roll1, synthDefs[\atan], this,
				[ \in, this.at(\acc1yLP), \in2, this.at(\acc1zLP)]),
			Feature(\roll2, synthDefs[\atan], this,
				[ \in, this.at(\acc2yLP), \in2, this.at(\acc2zLP)]),
			Feature(\yaw1,	  synthDefs[\atan], this,
				[\in, this.at(\acc1xLP), \in2, this.at(\acc1yLP)]),
			Feature(\yaw2,	  synthDefs[\atan], this,
				[\in, this.at(\acc2xLP), \in2, this.at(\acc2yLP)]),
			Feature(\fold,  synthDefs[\difference], this,
				[\in, this.at(\acc1x), \in2, this.at(\acc2x)]),
			Feature(\twist, synthDefs[\difference], this,
				[\in, this.at(\acc1y), \in2, this.at(\acc2y)]),
			Feature(\bend,  synthDefs[\difference], this,
				[\in, this.at(\acc1z), \in2, this.at(\acc2z)]),
			Feature(\foldLP,  synthDefs[\difference], this,
				[\in, this.at(\pitch1LP), \in2,	this.at(\pitch2LP)]),
			Feature(\twistLP, synthDefs[\difference], this,
				[\in, this.at(\roll1LP), \in2, this.at(\roll2LP)]),
			Feature(\bendLP,  synthDefs[\difference], this,
				[\in, this.at(\yaw1LP), \in2, this.at(\yaw2LP)]),
			Feature(\foldSpeed, synthDefs[\slope], this, [\in, this.at(\fold)])
		// 	// [\twistSpeed, "SlopeNode"],
		// 	// [\pitchSpeed, "SlopeNode"],
		// 	// [\rollSpeed, "SlopeNode"],
		// 	// [\tapVelocity,  ],
		];
		// fork {
		// 	var c;
		// 	c = Condition.new();
		// 	features.do{ |it, i|
		// 		busses.put(it[0], Bus.control(server, 1));
		// 		server.sync(c);
		// 		synths.put(
		// 			it[0],
		// 			Synth.tail(
		// 				server, it[1], [
		// 					\in, busses[it[2][0]],
		// 					\out, busses[it[2][1]],
		// 					\in2, busses[it[2][2]]
		// 				]
		// 			)
		// 		);
		// 		server.sync(c);
		// 	}
		// }

		fork{
			features.do{ |i|
				server.sync;
				i.create;
			}
		}
	}
	
	at { |id|
		^busses[id];
	}

	close {
		inputThread.stop;
		port.close;
	}
}

Feature {
	var <>name, <synthDef, <synth, <outBus, <>parameters, <>interface;
	// interface is normally a SpongeBee. Could be any interface... I think.

	*new { |name, synthDef, interf, params|
		^super.new.init(
			name, synthDef, params
		).interface_(interf);
	}

	init { |nam, sd, params|
		name = nam ? \unnamedFeature;
		synthDef = sd;
		parameters = params;
	}

	create {
		fork {
			synthDef.send(interface.server);
			outBus = Bus.control(interface.server, 1);
			interface.busses.put(name, outBus);
			interface.server.sync;
			synth = Synth.tail(
				interface.server,
				synthDef.name,
				[\out, outBus]
			).set(*parameters);
			interface.server.sync;
		}
	}

	set { |...params|
		synth.set(*params);
	}
}
