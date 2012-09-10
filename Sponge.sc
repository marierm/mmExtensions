Sponge : AbstractSponge {
	var <port, inputThread;

	init { arg br;
		port = SerialPort(
			port:portName,
			baudrate:br,
			databits:8,
			stopbit:true,
			parity:nil,
			crtscts:false,
			xonxoff:false,
			exclusive:false
		);
		inputThread = fork {
			// SPONGEBEE BYTE PARSING
			// ======================
			// 
			// Each sensor is 10 bits.
			// 10 bit values are encoded on 3 bytes.
			// First byte is status byte : 00000xxx
			// where xxx is input sensor number
			// 2nd and third bytes are data bytes : 1xxxxxxx and 10000xxx
			// 7 bit MSB on first data byte and 3 bit LSB on second one.
			// Last status byte (00001000) is for the 10 buttons.
			var data, msb, lsb, id;
			data = Array.fill(9,0);
			id = port.read;
			{(id >> 7) != 0}.while{
				id = port.read;
			};
			{
				9.do{
					msb = port.read % 128;
					lsb = port.read % 8;
					try {
						data[id] = (msb << 3) + lsb;
					};
					id = port.read;
				};
				// invert Z axies test;
				// data[2] = (data[2] * -1) + 1023;
				// data[5] = (data[5] * -1) + 1023;
				values = data;
				action.value(*data);
			}.loop;
		};

		features = List[];
		featureNames = List[];
		// Add Features for each sensor
		[
			\acc1x, \acc1y, \acc1z,
			\acc2x, \acc2y, \acc2z,
			\fsr1, \fsr2, \buttons
		].do{|i,j|
			Feature.sensor(i,this,j);
		};
	}
	
	close {
		features.do{|i|
			i.remove;
		};
		inputThread.stop;
		port.close;
	}
}

// Useful to develop when I don't have the sponge with me.
// Uses random data insteead of sensors.
SpongeEmu : Sponge {
	init { arg func;
		portName = "Emulator"; // Fake a SerialPort name to show it in the
							   // gui.
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
		// Add Features for each sensor
		[
			\acc1x, \acc1y, \acc1z,
			\acc2x, \acc2y, \acc2z,
			\fsr1, \fsr2, \buttons
		].do{|i,j|
			Feature.sensor(i,this,j);
		};
	}
}