Sponge {
	classvar featureList;
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
		this.createAllFeatures;
	}

	createAllFeatures {
		// pitch, roll, yaw
		Feature.lang(\pitch1, this, [this[\acc1x], this[\acc1z]],
			Feature.langFuncs[\atan]);
		Feature.lang(\roll1, this, [this[\acc1y], this[\acc1z]],
			Feature.langFuncs[\atan]);
		Feature.lang(\yaw1, this, [this[\acc1x], this[\acc1y]],
			Feature.langFuncs[\atan]);
		Feature.lang(\pitch2, this, [this[\acc2x], this[\acc2z]],
			Feature.langFuncs[\atan]);
		Feature.lang(\roll2, this, [this[\acc2y], this[\acc2z]],
			Feature.langFuncs[\atan]);
		Feature.lang(\yaw2, this, [this[\acc2x], this[\acc2y]],
			Feature.langFuncs[\atan]);
		Feature.lang(\pitch, this, [this[\pitch1], this[\pitch2]],
			Feature.langFuncs[\meanMany]);
		Feature.lang(\roll, this, [this[\roll1], this[\roll2]],
			Feature.langFuncs[\meanMany]);
		Feature.lang(\yaw, this, [this[\yaw1], this[\yaw2]],
			Feature.langFuncs[\meanMany]);
		// bend, twist, fold
		Feature.lang(\bend, this, [this[\pitch1], this[\pitch2]],
			Feature.langFuncs[\diff]);
		Feature.lang(\twist, this, [this[\roll1], this[\roll2]],
			Feature.langFuncs[\diff]);
		Feature.lang(\fold, this, [this[\yaw1], this[\yaw2]],
			Feature.langFuncs[\diff]);
		// fsr mean
		Feature.lang(\fsrMean, this, [this[\fsr1], this[\fsr2]],
			Feature.langFuncs[\meanMany]);
		// fsr diff
		Feature.lang(\fsrDiff, this, [this[\fsr1], this[\fsr2]],
			Feature.langFuncs[\diff]);
		// Speed of everything
		features.collect(_.name).do{|i|
			Feature.lang(
				(i ++ \Speed).asSymbol, this, this[i],
				Feature.langFuncs[\slope]
			)
		};
		// lowpass and hipass Feature on evrything;
		features.collect(_.name).do{|i|
			Feature.synth(
				(i ++ \LP).asSymbol, this, this[i],
				Feature.synthFuncs[\LP], [\freq, 3]
			);
			Feature.synth(
				(i ++ \HP).asSymbol, this, this[i],
				Feature.synthFuncs[\HP], [\freq, 3]
			);
		}

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
}
