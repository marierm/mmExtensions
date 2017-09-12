SpongeSLIP : Sponge {
	var <port, inputThread, <>hold;

	init { arg br;
		hold = false;
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
		values = Int16Array.newClear(9);
		inputThread = fork {
			// SLIP ENCODED SPONGE 
			// ===================
			// 
			// The sponge has 8 sontinuous sensors and 10 buttons.  Each
			// sensor has a 10 bit value encoded in two bytes.  A sponge
			// packet contains 18 bytes:
			// 
			// |  0 | acc1x-MSB   |
			// |  1 | acc1x-LSB   |
			// |  2 | acc1y-MSB   |
			// |  3 | acc1y-LSB   |
			// |  4 | acc1z-MSB   |
			// |  5 | acc1z-LSB   |
			// |  6 | acc2x-MSB   |
			// |  7 | acc2x-LSB   |
			// |  8 | acc2y-MSB   |
			// |  9 | acc2y-LSB   |
			// | 10 | acc2z-MSB   |
			// | 11 | acc2z-LSB   |
			// | 12 | fsr1-MSB    |
			// | 13 | fsr1-LSB    |
			// | 14 | fsr2-MSB    |
			// | 15 | fsr2-LSB    |
			// | 16 | buttons 0-7 |
			// | 17 | buttons 8-9 |
			// 
			// The packets are SLIP encoded using these special characters:
			// end = 8r300 (2r11000000 or 0xc0 or 192)
			// esc = 8r333 (2r11011011 or 0xdb or 219)
			// esc_end = 8r334 (2r011011100 or 0xdc or 220)
			// esc_esc = 8r335 (2r011011101 or 0xdd or 221)

			var data, buffer, serialByte;
			var packetSize = 18;
			var slipEND = 8r300;
			var slipESC = 8r333;
			var slipESC_END = 8r334;
			var slipESC_ESC = 8r335;
			buffer = Int16Array(maxSize:packetSize);
			{
				serialByte = port.read;
				serialByte.switch(
					slipEND, {
						var i = -1;
						(buffer.size == packetSize).if({
							{buffer.size > 0}.while({
								values.wrapPut(
									i, 
									buffer.pop + ((buffer.pop ? 0) << 8);
								);
								i = i - 1;
							});
							hold.not.if({
								action.value(values);
							});
						},{
							"Irregular sponge packet size".warn;
							buffer.postln;
							buffer = Int16Array(maxSize:18);
						})
					},
					slipESC, {
						serialByte = port.read;
						serialByte.switch(
							slipESC_END, { buffer.add(slipEND) },
							slipESC_ESC, { buffer.add(slipESC) },
							{"SLIP encoding error.".warn; buffer.postln; }
						)
					},
					{ buffer.add(serialByte); }
				);
			}.loop
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

		CmdPeriod.doOnce(this);
		ShutDown.add({this.close});
		
		this.class.sponges.add(this);
	}
}

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
				action.value(data);
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
		super.close;
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
				data = Array.rand(8, 0, 1023) ++ 0;
				values = data;
				action.value(data);
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

		CmdPeriod.doOnce(this);
		ShutDown.add({this.close});
		
		this.class.sponges.add(this);
	}

}