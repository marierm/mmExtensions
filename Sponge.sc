Sponge {
	classvar <>featureList, <featureNames;
	var <port, inputThread, <featureNames, <features;
	var <>action, <values, interpAction;

	*new { arg portName="/dev/ttyUSB0", baudRate=19200;
		^super.new.init(portName, baudRate);
	}

	init { arg pn, br;
		port = SerialPort(
			port:pn,
			baudrate:br,
			databits:8,
			stopbit:true,
			parity:nil,
			crtscts:false,
			xonxoff:false,
			exclusive:false
		);
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
					values = data;
					action.value(*data);
				} {	// if it is the data of one of the sensors
					msb = port.read % 128;
					lsb = port.read % 8;
					data[id] = (msb << 3) + lsb;
				};
			}
		};
		features = List[];
		featureNames = List[];
		// Add Feautres for each sensor
		[
			\acc1x, \acc1y, \acc1z,
			\acc2x, \acc2y, \acc2z,
			\fsr1, \fsr2, \buttons
		].do{|i,j|
			Feature.sensor(i,this,j);
		};
		// this.createAllFeatures;
	}
	
	createAllFeatures {
		Sponge.featureList.collect({|i| i[\name] }).do{ |i|
			this.createFeature(i);
		};
	}

	at { |key|
		^features[featureNames.indexOf(key)];
	}
	
	connect { |prInt|
		interpAction = { |...msg|
			prInt.cursorPos_(msg.keep(prInt.numDim));
		};
		action = action.addFunc(interpAction);
	}

	disconnect {
		action = action.removeFunc(interpAction);
	}

	close {
		inputThread.stop;
		port.close;
	}
	
	createFeature { |key|
		var fe, nameList, inputs;
		// get a list af the names of the features;
		nameList = Sponge.featureList.collect({|i| i[\name] });
		// fe is an event describing a feature.
		fe = Sponge.featureList[nameList.indexOf(key)];
		// check if inputs exist
		fe[\input].do{|i|
			// if they don't, create them.
			this.featureNames.includes(i).not.if{
				this.createFeature(i);
			};
		};
		// fe.input contains names of Features.
		// This gets the actual Feature.
		inputs = fe[\input].collect({|i| this[i]});
		Feature.performList(
			fe[\type],
			[fe[\name], this, inputs, fe[\func], fe[\args]]
		);
	}

	*initClass {
		featureList = List[
			(name:\pitch1, input:[\acc1x, \acc1z],
				func:Feature.langFuncs[\atan], type:\lang
			).know_(false),
			(name:\roll1, input:[\acc1y, \acc1z],
				func:Feature.langFuncs[\atan], type:\lang
			).know_(false),
			(name:\yaw1, input:[\acc1x, \acc1y],
				func:Feature.langFuncs[\atan], type:\lang
			).know_(false),
			(name:\pitch2, input:[\acc2x, \acc2z],
				func:Feature.langFuncs[\atan], type:\lang
			).know_(false),
			(name:\roll2, input:[\acc2y, \acc2z],
				func:Feature.langFuncs[\atan], type:\lang
			).know_(false),
			(name:\yaw2, input:[\acc2x, \acc2y],
				func:Feature.langFuncs[\atan], type:\lang
			).know_(false),
			(name:\pitch, input:[\pitch1, \pitch2],
				func:Feature.langFuncs[\meanMany], type:\lang
			).know_(false),
			(name:\roll, input:[\roll1, \roll2],
				func:Feature.langFuncs[\meanMany], type:\lang
			).know_(false),
			(name:\yaw, input:[\yaw1, \yaw2],
				func:Feature.langFuncs[\meanMany], type:\lang
			).know_(false),
			// bend, twist, fold
			(name:\bend, input:[\pitch1, \pitch2],
				func:Feature.langFuncs[\diff], type:\lang
			).know_(false),
			(name:\twist, input:[\roll1, \roll2],
				func:Feature.langFuncs[\diff], type:\lang
			).know_(false),
			(name:\fold, input:[\yaw1, \yaw2],
				func:Feature.langFuncs[\diff], type:\lang
			).know_(false),
			// fsr mean
			(name:\fsrMean, input:[\fsr1, \fsr2],
				func:Feature.langFuncs[\meanMany], type:\lang
			).know_(false),
			// fsr diff
			(name:\fsrDiff, input:[\fsr1, \fsr2],
				func:Feature.langFuncs[\diff], type:\lang
			).know_(false)
		];
		featureList.collect({|i| i[\name] }).do{|i|
			featureList.add(
				(name:(i ++ \Speed).asSymbol, input:[i],
					func:Feature.langFuncs[\slope], type:\lang
				).know_(false)
			)
		};
		featureList.collect({|i| i[\name] }).do{|i|
			featureList.add(
				(name:(i ++ \LP).asSymbol, input:[i],
					func:Feature.langFuncs[\LP], type:\synth,
					args:[\freq, 3]
				).know_(false)
			);
			featureList.add(
				(name:(i ++ \HP).asSymbol, input:[i],
					func:Feature.langFuncs[\HP], type:\synth,
					args:[\freq, 100]
				).know_(false)
			);
		};
	}
}

// Useful to develop when I don't have the sponge with me.
// Uses random data insteead of sensors.
SpongeEmu : Sponge {
	init { arg func;
		inputThread = fork {
			var data; 
			data = Array.fill(9,0);
			loop {
				data = Array.rand(9, 0, 1023);
				values = data;
				action.value(*data);
				0.1.wait;
			};
		};
		features = List[];
		featureNames = List[];
		// Add Feautres for each sensor
		[
			\acc1x, \acc1y, \acc1z,
			\acc2x, \acc2y, \acc2z,
			\fsr1, \fsr2, \buttons
		].do{|i,j|
			Feature.sensor(i,this,j);
		};
		// this.createAllFeatures;
	}
}