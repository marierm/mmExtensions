SpongeCee {
	var <port, inputThread, <featureNames, <features;
	var <>action, <values;

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
		values = IdentityDictionary();
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
		[
			\acc1x, \acc1y, \acc1z,
			\acc2x, \acc2y, \acc2z,
			\fsr1, \fsr2, \buttons
		].do{|i,j|
			Feature.sensor(i,this,j);
		};
	}

	at { |key|
		^features[featureNames.indexOf(key)];
	}
	
	valueAt {}
}
