// Copyright 2011 Martin Marier
// 
// 

Looper {

	var inBus, maxDur, server, <buffer, recSynth, pbSynth,
	xFade, <start, <end, <outBus, <startBus, <endBus;

	*initClass {
		16.do{|i|
			var numChannels;
			numChannels = i+1;
			SynthDef(("looperrecaudio" ++ numChannels).asSymbol, {
				arg inBus=0, bufnum=0, numFrames=1, phasorOut=100;
				var phase;
				phase = Phasor.ar(0,1,0,numFrames);
				Out.kr(phasorOut, A2K.kr(phase));
				BufWr.ar(In.ar(inBus, numChannels), bufnum, phase);
			}).writeDefFile();
			SynthDef(("looperpbaudio" ++ numChannels).asSymbol, {
				arg out=0, bufnum=0, trig=0, start=0, end=1, resetPos=0;
				var phase;
				phase = Phasor.ar(trig,1,start,end, resetPos);
				Out.ar(
					out,
					BufRd.ar(numChannels, bufnum, phase)
				)
			}).writeDefFile();
			SynthDef(("looperreccontrol" ++ numChannels).asSymbol, {
				arg inBus=0, bufnum=0, startOut=100, endOut=101,
				startTrig, endTrig;
				var phase;
				phase = Phasor.kr(0,1,0,BufFrames.kr(bufnum));
				Out.kr(startOut, Latch.kr(phase, startTrig));
				Out.kr(endOut, Latch.kr(phase, endTrig));
				BufWr.kr(In.kr(inBus, numChannels), bufnum, phase);
			}).writeDefFile();
			SynthDef(("looperpbcontrol" ++ numChannels).asSymbol, {
				arg out=0, bufnum=0, trig=0, startBus, endBus;
				var phase, start, end;
				start = In.kr(startBus);
				end = In.kr(endBus);
				phase = Phasor.kr(
					trig, 1, start, 
					Select.kr( //end
						end <= start,
						[end, BufFrames.kr(bufnum) + end]
					)
				);
				Out.kr(
					out,
					BufRd.kr(numChannels, bufnum, phase)
				)
			}).writeDefFile();
		}
	}

	*new { |inBus, maxDur=60|
		^super.newCopyArgs(inBus, maxDur, inBus.server).init;
	}

	init {
		fork{
			var dur, recDefName, pbDefName;
			dur = maxDur * server.sampleRate;
			(inBus.rate == 'control').if {
				dur = dur div: server.options.blockSize;
			};

			recDefName = ("looperrec" ++ inBus.rate ++ inBus.numChannels).asSymbol;
			pbDefName = ("looperpb" ++ inBus.rate ++ inBus.numChannels).asSymbol;

			buffer = Buffer.alloc( server, dur, inBus.numChannels);
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
					\endBus, endBus.index
				],
				server,
				\addToTail
			);
		};
	}

	startRec {
		server.sendBundle(
			0.01,
			recSynth.runMsg(true),
			recSynth.setMsg(\startTrig, 1)
		);
		server.sendBundle(
			0.05,
			recSynth.setMsg(\startTrig, 0)
		);
		// phasorBus.get({|val|
		// 	start = val;
		// 	pbSynth.set(\start, start);
		// });
	}

	stopRec {
		server.sendBundle(
			0.01,
			recSynth.setMsg(\endTrig, 1)
		);
		server.sendBundle(
			0.05,
			recSynth.setMsg(\endTrig, 0),
			recSynth.runMsg(false)
		);

		// phasorBus.get({|val|
		// 	end = val;
		// 	(end <= start).if{
		// 		end = end + buffer.numFrames;
		// 	};
		// 	pbSynth.set(\end, end);
		// });
		// recSynth.run(false);
	}

	startPb {
		this.stopRec;
		server.sendBundle(0.01, pbSynth.runMsg(true), pbSynth.setMsg(\trig,1));
		server.sendBundle(0.02, pbSynth.setMsg(\trig,0));
		// set crossFade stuff
	}

	stopPb {
		pbSynth.run(false);
	}
}
