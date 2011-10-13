// Copyright 2011 Martin Marier
// 
// 

Looper {

	var inBus, maxDur, server, <>xFade, <buffer, recSynth, pbSynth,
	<start, <end, <outBus, <startBus, <endBus;

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
				arg inBus=0, bufnum=0, startOut=100, endOut=101,
				t_startTrig=0, t_endTrig=0;
				var phase;
				phase = Phasor.kr(t_startTrig,1,0,BufFrames.kr(bufnum));
				Out.kr(startOut, Latch.kr(phase, t_startTrig));
				Out.kr(endOut, Latch.kr(phase, t_endTrig));
				BufWr.kr(In.kr(inBus, numChannels), bufnum, phase);
			}).writeDefFile();
			SynthDef(("looperpbcontrol" ++ numChannels).asSymbol, {
				arg out=0, bufnum=0, t_trig=0, startBus, endBus, fadeTime=1,
				gate=0, t_reset=0, hold=0;
				var phase1, phase2, start, end, player1, player2, env, dur, sig;
				start = In.kr(startBus);
				end = Select.kr(
						In.kr(endBus) <= start,
						[In.kr(endBus), BufFrames.kr(bufnum) + In.kr(endBus)]
				);
				dur = ((end - start) * ControlDur.ir) - fadeTime;
				// fadeTime = fadeTime * ControlRate.ir;
				// phase1 = Phasor.kr(t_trig, 1, start, end + (fadeTime * ControlRate.ir));
				// phase1 = Phasor.kr(t_trig, 1, start, end + (fadeTime * ControlRate.ir));
				phase1 = Phasor.kr(t_trig, 1, start, 2 * end);
				// phase2 = (phase1 + end) % (end + (fadeTime * ControlRate.ir));
				phase2 = (phase1 + end) % (2 * end);
				player1 = BufRd.kr(numChannels, bufnum, phase1);
				player2 = BufRd.kr(numChannels, bufnum, phase2);

				env = DemandEnvGen.kr(
					Dseq([0, 1, 1, 0], inf),
					Dseq([fadeTime, dur,fadeTime, dur], inf),
					gate: gate,
					reset: t_reset
				);
				player1 = player1 * env;
				player2 = player2 * (1 - env); // invert enveloppe
				sig = Mix([player1, player2]);
				sig = SelectX.kr(VarLag.kr(hold, fadeTime, 0), [sig, Latch.kr(sig, t_trig)]);
				// env2 = Linen.kr(hold, fadeTime, 1, fadeTime);
				Out.kr(out, sig );
				// Out.kr(	out, [player1, player2]);
				// Out.kr(	out, player1);
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
			startBus = Bus.control(server, 1);
			endBus = Bus.control(server, 1);
			
			server.sync;

			recSynth = Synth.newPaused(
				recDefName, [
					\inBus, inBus.index,
					\bufnum, buffer.bufnum,
					\startOut, startBus.index,
					\endOut, endBus.index
				],
				server,
				\addToHead
			);
			pbSynth = Synth.newPaused(
				pbDefName, [
					\out, outBus.index,
					\bufnum, buffer.bufnum,
					\trig, 0,
					\startBus, startBus.index,
					\endBus, endBus.index,
					\fadeTime, xFade
				],
				server,
				\addToTail
			);
		};
	}

	startRec {
		this.stopPb;
		// start recording now
		// and set start.
		recSynth.run(true);
		server.sendBundle(
			0.01,
			recSynth.setMsg(\t_startTrig, 1)
		);
	}

	stopRec {
		// var start;
		// // remember start time;
		// startBus.get({|val| start = val; });
		// stop recording in xFade seconds
		server.sendBundle(
			0.01,
			recSynth.setMsg(\t_endTrig, 1, \gate, 0)
		);
		server.sendBundle(
			xFade,
			recSynth.runMsg(false)
		);
	}

	startPb {
		this.stopRec;
		pbSynth.run(true);
		pbSynth.set(\gate, 1, \t_reset, 2, \hold, 0);

		server.sendBundle(
			0.01,
			// recSynth.setMsg(\t_endTrig, 1),
			pbSynth.setMsg(\t_trig, 1, \gate, 1)
			// pbSynth.setnMsg(\env, Env([0,1,0],[xFade, , xFade]).asArray)
		);
	}

	stopPb {
		pbSynth.set(\gate, -1, \hold, 1);
	}
}
