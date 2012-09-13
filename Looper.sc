// Copyright 2011-2012 Martin Marier
// 
// 

Looper {

	var inBus, <xFade, <maxDur, server, <buffer, recSynth, pbSynth,
	<outBus, <lengthBus, <isRecording, <isPlaying;

	*initClass {
		16.do{|i|
			var numChannels;
			numChannels = i+1;
			// audio rate SynthDefs
			SynthDef(("looperrecaudio" ++ numChannels).asSymbol, {
				arg inBus=0, bufnum=0, lengthBus=100, run=0,
				t_trig=1, gate=1, fadeTime=1, t_reset=0;
				var recLevel, preLevel;
				recLevel = VarLag.kr(gate, fadeTime, warp:\welch);
				// recLevel = Linen.kr(gate, 0, 1, fadeTime);
				preLevel = 1 - recLevel;
				RecordBuf.ar(
					In.ar(inBus, numChannels),
					bufnum,
					offset: 0,
					recLevel: recLevel,
					preLevel: preLevel,
					run: run,
					loop: 0,
					trigger: t_trig
				);
				Out.ar(
					lengthBus,
					Latch.ar(
						Phasor.ar(t_reset,1,0, SampleRate.ir * 60),
						t_trig
					)
				);
			}).writeDefFile();
			SynthDef(("looperpbaudio" ++ numChannels).asSymbol, {
				arg out=0, bufnum=0, lengthBus;
				var dur, player;
				dur = In.ar(lengthBus, 1); // * ControlDur.ir;
				player = PlayBuf.ar(
					numChannels: numChannels,
					bufnum: bufnum,
					rate: 1,
					trigger: Phasor.ar(0,1, 0, dur - 1),
					startPos: 0,
					loop: 1
				);
				Out.ar(out, player);
			}).writeDefFile();
			// Control rate SynthDefs
			SynthDef(("looperreccontrol" ++ numChannels).asSymbol, {
				arg inBus=0, bufnum=0, lengthBus=100, run=0,
				t_trig=1, gate=1, fadeTime=1, t_reset=0;
				var recLevel, preLevel;
				recLevel = VarLag.kr(gate, fadeTime, warp:\lin);
				// recLevel = Linen.kr(gate, 0, 1, fadeTime);
				preLevel = 1 - recLevel;
				RecordBuf.kr(
					In.kr(inBus, numChannels),
					bufnum,
					offset: 0,
					recLevel: recLevel,
					preLevel: preLevel,
					run: run,
					loop: 0,
					trigger: t_trig
				);
				Out.kr(
					lengthBus, Latch.kr(
						Phasor.kr(t_reset,1,0, ControlRate.ir * 60).poll(t_trig),
						t_trig
					)
				);
			}).writeDefFile();
			SynthDef(("looperpbcontrol" ++ numChannels).asSymbol, {
				arg out=0, bufnum=0, lengthBus;
				var dur, player;
				dur = In.kr(lengthBus, 1); // * ControlDur.ir;
				player = PlayBuf.kr(
					numChannels: numChannels,
					bufnum: bufnum,
					rate: 1,
					trigger: Phasor.kr(0,1, 0, dur - 1),
					startPos: 0,
					loop: 1
				);
				Out.kr(out, player);
			}).writeDefFile();
		}
	}

	*new { |inBus, xFade=0.1, maxDur=60 |
		^super.newCopyArgs(inBus, xFade, maxDur).init;
	}

	init {
		fork{
			var dur, recDefName, pbDefName;
			server = inBus.server;
			dur = maxDur * server.sampleRate;
			// Make a shorter buffer if we record control rate.
			(inBus.rate == 'control').if {
				dur = dur div: server.options.blockSize;
			};

			recDefName = ("looperrec" ++ inBus.rate ++ inBus.numChannels).asSymbol;
			pbDefName = ("looperpb" ++ inBus.rate ++ inBus.numChannels).asSymbol;

			buffer = Buffer.alloc( server, dur, inBus.numChannels);
			// outBus = Bus.alloc(inBus.rate, server, inBus.numChannels);
			outBus = Bus.alloc(inBus.rate, server, inBus.numChannels);
			lengthBus = Bus.alloc(inBus.rate, server, 1);
			
			server.sync;

			recSynth = Synth.new(
				recDefName, [
					\inBus, inBus.index,
					\bufnum, buffer.bufnum,
					\lengthBus, lengthBus.index,
					\run, 1,
					\fadeTime, xFade
				],
				server,
				\addToTail
			);
			pbSynth = Synth.basicNew( pbDefName, server	);
		};
	}

	startRec {
		isRecording = true;
		this.stopPb;
		// start recording now
		server.sendBundle(
			0.05,
			recSynth.setMsg(\t_trig, 1, \gate, 1, \run, 1, \t_reset, 1)
		);
	}

	stopRec {
		isRecording.if {
			isRecording = false;
			// Stop only if currently recording.

			// Go to beginning of buffer and overdub while
			// crossfading.
			server.sendBundle(
				0.05,
				recSynth.setMsg(\gate, 0, \t_trig, 1)
			);
			// stop recording in xFade seconds
			server.sendBundle(
				xFade + 0.05,
				recSynth.setMsg(\run, 0)
			);
		}
	}

	startPb {
		isPlaying.not.if{
			isPlaying = true;
			this.stopRec;
			server.sendBundle(
				0.05,
				pbSynth.addAfterMsg(
					recSynth, [
						\out, outBus.index,
						\bufnum, buffer.bufnum,
						\lengthBus, lengthBus.index
					]
				);
				// pbSynth.runMsg(true)
			);
		}
	}

	stopPb {
		pbSynth.free;
		isPlaying = false;
	}

	xFade_ { |dur|
		xFade = dur;
		recSynth.set(\fadeTime, dur);
	}

	free {
		pbSynth.free;
		recSynth.free;
		buffer.free;
	}
}

LooperFeature : Feature {
	var <>looper;

	*new { |name, interface, input, xFade=0.1, maxDur=60 |
		^super.newCopyArgs(name, interface, input).init( xFade, maxDur );
	}

	init { |xFade, maxDur|
		super.init;
		input.dependantFeatures.add(this);
		fork {
			looper = Looper(input.bus, xFade, maxDur);
			server.sync;
			bus = looper.outBus;
		};
		fullFunc = {
			bus.get{|value|
				action.value(value);
				// netAddr.sendMsg(oscPath, value);
			};
		};

		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
	}

	startRec { looper.startRec }
	stopRec { looper.stopRec }
	startPb { looper.startPb }
	stopPb { looper.stopPb }

	remove {
		super.remove;
		// remove myself from the
		// dependantFeatures list of others.
		input.dependantFeatures.remove(this);
		looper.free;
	}
}

+ Feature {
	loop { |xFade=0.1, maxDur=60|
		^Feature.looper( name ++ "looper", interface, this,	xFade, maxDur );
	}

	*looper { |name, interface, input, xFade=0.1, maxDur=60|
		^LooperFeature( name, interface, input, xFade, maxDur );
	}
}