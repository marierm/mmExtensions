// Copyright 2011 Martin Marier
// 
// 

SpongeLooper {

	var maxDur, numChannels, server, rate, <buffer, wSynth, rSynth, dur,
	xFade, start, end, inBuses, outBus;

	*new { |maxDur=60, numChannels=1, server, rate='control'|
		server = server ? Server.default;
		^Super.newCopyArgs(maxDur, numChannels, server, rate).init;
	}

	*ar { |maxDur=60, numChannels=1, server|
		^this.new(maxDur, numChannels, server, 'audio');
	}

	*kr { |maxDur=60, numChannels=1, server|
		^this.new(maxDur, numChannels, server, 'control');
	}

	init {
		buffer = Buffer.alloc(
			server, maxDur * server.sampleRate, numChannels
		);
		outBus = Bus.alloc(rate, server, numChannels);

		// wDef = SynthDef(\help_RecordBuf, {
		// 	arg out=0, bufnum=0, inBus;
		// 	RecordBuf.ar(formant, bufnum, doneAction: 2, loop: 0);
		// });
		// wSynth = Synth.tail(server, \wDef);
	}

	inBuses_ { |...buses|
		var totalNumChan, pass=true;
		// Check the total number of channels and if rates correspond;
		totalNumChan = buses.collect(_.numChannels).sum;
		buses.do{|i| (i.rate != rate).if{pass=false} };
		pass = pass and: (totalNumChan == numChannels);
		pass.if {
			inBuses = buses;
		} {
			"buses.size is not equal to numChannels".warn;
		}
	}

	rec {}
	
	play {}
}