// Copyright 2011 Martin Marier
// 
// 

Looper {

	var inBus, maxDur, server, <buffer, recSynth, pbSynth,
	xFade, start, end, <outBus, phasorBus;

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
				arg inBus=0, bufnum=0, numFrames=1, phasorOut=100;
				var phase;
				phase = Phasor.kr(0,1,0,numFrames);
				Out.kr(phasorOut, phase);
				BufWr.kr(In.kr(inBus, numChannels), bufnum, phase);
			}).writeDefFile();
			SynthDef(("looperpbcontrol" ++ numChannels).asSymbol, {
				arg out=0, bufnum=0, trig=0, start=0, end=1, resetPos=0;
				var phase;
				phase = Phasor.kr(trig,1,start,end, resetPos);
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
			phasorBus = Bus.control(server, 1);
			
			server.sync;

			recSynth = Synth.head(server, recDefName, [
				\out, outBus.index,
				\inBus, inBus.index,
				\bufnum, buffer.bufnum,
				\numFrames, buffer.numFrames,
				\phasorOut, phasorBus.index
			]);
			pbSynth = Synth.newPaused(pbDefName, [
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
		};
	}

	startRec {
		phasorBus.get({|val| start = val});
	}

	stopRec {
		phasorBus.get({|val| end = val});
	}

	startPb {
		var msg;
		(end <= start).if{
			end = buffer.numFrames + end;
		};
		msg = [
			pbSynth.runMsg(true),
			pbSynth.setMsg( \start, start,\end, end ),
			pbSynth.setMsg(\trig,1),
			pbSynth.setMsg(\trig,0)
		];

		server.listSendBundle(0.01, msg);
		// set crossFade stuff
	}

	stopPb {
		pbSynth.run(false);
	}


}
