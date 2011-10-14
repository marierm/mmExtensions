// Copyright 2011 Martin Marier
// 
// 

Looper {

	var inBus, maxDur, server, <>xFade, <buffer, recSynth, pbSynth,
	<start, <end, <outBus, <lengthBus;

	*initClass {
		16.do{|i|
			var numChannels;
			numChannels = i+1;
			// audio rate disabled
			// 
			// SynthDef(("looperrecaudio" ++ numChannels).asSymbol, {
			// 	arg inBus=0, bufnum=0, startOut=100, endOut=101,
			// 	startTrig, endTrig;
			// 	var phase;
			// 	phase = Phasor.ar(0,1,0,BufFrames.kr(bufnum));
			// 	Out.kr(startOut, Latch.kr(phase, startTrig));
			// 	Out.kr(endOut, Latch.kr(phase, endTrig));
			// 	BufWr.ar(In.ar(inBus, numChannels), bufnum, phase);
			// }).writeDefFile();
			// SynthDef(("looperpbaudio" ++ numChannels).asSymbol, {
			// 	arg out=0, bufnum=0, trig=0, startBus, endBus;
			// 	var phase, start, end;
			// 	start = In.kr(startBus);
			// 	end = In.kr(endBus);
			// 	phase = Phasor.ar(
			// 		trig, 1, start, 
			// 		Select.kr( //end
			// 			end <= start,
			// 			[end, BufFrames.kr(bufnum) + end]
			// 		)
			// 	);
			// 	Out.ar(
			// 		out,
			// 		BufRd.ar(numChannels, bufnum, phase)
			// 	)
			// }).writeDefFile();

			SynthDef(("looperreccontrol" ++ numChannels).asSymbol, {
				arg inBus=0, bufnum=0, lengthBus=100, run=0, t_trig=0, gate=1, fadeTime=1, t_reset=0;
				var recLevel, preLevel;
				recLevel = Linen.kr(gate, 0, 1, fadeTime);
				preLevel = 1 - recLevel;
				RecordBuf.kr(
					In.kr(inBus, numChannels),
					bufnum,
					offset: 0,
					recLevel: recLevel,
					preLevel: preLevel,
					run: run,
					trigger: t_trig
				);
				Out.kr(lengthBus, Latch.kr(Phasor.kr(t_reset,1,0, 750 * 60), t_trig));
			}).writeDefFile();
			SynthDef(("looperpbcontrol" ++ numChannels).asSymbol, {
				arg out=0, bufnum=0, lengthBus;
				var dur, player;
				dur = (In.kr(lengthBus, 1) - 10) * ControlDur.ir;
				player = PlayBuf.kr(
					numChannels: numChannels,
					bufnum: bufnum,
					rate: 1,
					trigger: Impulse.kr(1/dur),
					startPos: 0,
					loop: 1
				);
				Out.kr(out, player);
			}).writeDefFile();
		}
	}

	*new { |inBus, maxDur=60, xFade=0.05|
		^super.newCopyArgs(inBus, maxDur, inBus.server, xFade).init;
	}

	init {
		fork{
			var dur, recDefName, pbDefName;
			dur = maxDur * server.sampleRate * 2;
			(inBus.rate == 'control').if {
				dur = dur div: server.options.blockSize;
			};

			recDefName = ("looperrec" ++ inBus.rate ++ inBus.numChannels).asSymbol;
			pbDefName = ("looperpb" ++ inBus.rate ++ inBus.numChannels).asSymbol;

			buffer = Buffer.alloc( server, dur, inBus.numChannels);
			// outBus = Bus.alloc(inBus.rate, server, inBus.numChannels);
			outBus = Bus.alloc(inBus.rate, server, inBus.numChannels);
			lengthBus = Bus.control(server, 1);
			
			server.sync;

			recSynth = Synth.new(
				recDefName, [
					\inBus, inBus.index,
					\bufnum, buffer.bufnum,
					\lengthBus, lengthBus.index,
					\run, 0,
					\fadeTime, xFade
				],
				server,
				\addToHead
			);
			pbSynth = Synth.newPaused(
				pbDefName, [
					\out, outBus.index,
					\bufnum, buffer.bufnum,
					\lengthBus, lengthBus.index
				],
				server,
				\addToTail
			);
		};
	}

	startRec {
		this.stopPb;
		// start recording now
		server.sendBundle(
			0.01,
			recSynth.setMsg(\run, 1, \t_trig, 1, \gate, 1, \t_reset, 1)
		);
	}

	stopRec {
		// var start;
		// // remember start time;
		// startBus.get({|val| start = val; });
		// stop recording in xFade seconds
		server.sendBundle(
			0.01,
			recSynth.setMsg(\t_trig, 1, \run, 1, \gate, 0)
		);
		server.sendBundle(
			xFade + 0.01,
			recSynth.set(\run, 0)
		);
	}

	startPb {
		this.stopRec;
		server.sendBundle(
			0.01,
			pbSynth.runMsg(true)
		);
	}

	stopPb {
		pbSynth.run(false);
	}
}
