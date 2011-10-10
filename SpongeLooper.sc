// Copyright 2011 Martin Marier
// 
// 

Looper {

	var inBus, maxDur, server, <buffer, recSynth, pbSynth, dur,
	xFade, start, end, outBus;

	*new { |inBus, maxDur=60, server, rate='control'|
		server = server ? Server.default;
		(rate == 'control').if {
			^Super.newCopyArgs(inBus, maxDur, server).initKr;
		};
		(rate == 'audio').if {
			^Super.newCopyArgs(inBus, maxDur, server).initAr;
		};

	}

	*ar { |inBus, maxDur=60, server|
		^this.new(inBus, maxDur, server, 'audio');
	}

	*kr { |inBus, maxDur=60, server|
		^this.new(inBus, maxDur, server, 'control');
	}

	initAr {
		{
			buffer = Buffer.alloc(
				server, maxDur * server.sampleRate, inBus.numChannels
			);
			outBus = Bus.audio(server, inBus.numChannels);
			
			SynthDef(("looperRec" ++ inBus.numChannels).asSymbol, {
				arg inBus=0, bufnum=0, numFrames=1;
				var phase;
				phase = Phasor.ar(0,1,0,numFrames);
				BufWr.ar(In.ar(inBus, inBus.numChannels), bufnum, phase);
			}).send(server);

			SynthDef(("looperPb" ++ inBus.numChannels).asSymbol, {
				arg out=0, bufnum=0, trig=0, start=0, end=1, resetPos=0;
				var phase;
				phase = Phasor.ar(trig,1,start,end, resetPos);
				Out.ar(
					out,
					BufRd.ar(inBus.numChannels, bufnum, phase)
				)
			}).send(server);

			server.sync;

			recSynth = Synth.head(
				server,
				("looperRec" ++ inBus.numChannels).asSymbol, [
					\out, outBus.index,
					\inBus, inBus,
					\bufnum, buffer.bufnum,
					\numFrames, buffer.numFrames
				]
			);

			pbSynth = Synth.newPaused(
				("looperPb" ++ inBus.numChannels).asSymbol, [
					\out, outBus.index,
					\bufnum, buffer.bufnum,
					\trig, 0,
					\start, 0,
					\end, buffer.numFrames,
					\resetPos, 0
				],
				server,
				\addToTail
			);
		}.fork;
	}

	initKr {
		{
			buffer = Buffer.alloc(
				server, maxDur * server.sampleRate, inBus.numChannels
			);
			outBus = Bus.audio(server, inBus.numChannels);
			server.sync;
			
			recSynth = SynthDef(("looper" ++ inBus.numChannels).asSymbol, {
				arg out=0, inBus=0, bufnum=0, numFrames=1;
				var phase;
				phase = Phasor.ar(0,1,0,numFrames);
				BufWr.ar(In.ar(inBus, inBus.numChannels), bufnum, phase);
			}).play(server, [
				\out, outBus.index,
				\inBus, inBus,
				\bufnum, buffer.bufnum,
				\numFrames, buffer.numFrames
			]);
		}.fork;
	}
}
