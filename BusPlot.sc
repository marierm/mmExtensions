BusPlot {
	var bus, name, <dur, <rate, 
	<plot, oscDef, buffer, synth, numFrames,
	pad=20; // Drop 20 values to avoid glitches in graph.

	*new { |bus, name, dur=5, rate=15|
		^super.newCopyArgs(bus, name, dur, rate).init;
	}

	init {
		this.prGetNumFrames(pad);
		buffer = Buffer.alloc(bus.server, numFrames, bus.numChannels);
		synth = SynthDef(\busPlot, { |numFrames, bufnum, rate|
			var phase;
			phase = Phasor.kr(0,1,0,numFrames);
			BufWr.kr(In.kr(bus.index, bus.numChannels), bufnum, phase);
			SendTrig.kr(Impulse.kr(rate), 0, phase);
		}).play(bus.server, [
			\numFrames, numFrames,
			\bufnum, buffer.bufnum,
			\rate, rate
		]);
		
		plot = Plotter(	name.asString, Rect(600, 30, 500, 400) );
		this.prSetOscDef(synth.nodeID, pad);

		plot.parent.onClose_({
			synth.free;
			buffer.free;
			oscDef.free;
		});
	}

	prGetNumFrames { |pad=20|
		numFrames = (
			bus.server.sampleRate * dur div:
			bus.server.options.blockSize
		) + (pad * bus.numChannels);
	}
	
	prSetOscDef { |id, pad=20|
		oscDef = OSCdef(("busplot" ++ id).asSymbol, { arg msg, time;
			(msg[1] == id).if {
				buffer.loadToFloatArray( 0, numFrames, {|vals|
					vals = vals.as(Array).rotate(
						(msg[3].asInt) * -1 * bus.numChannels
					);
					vals = vals.keep((numFrames-(pad * bus.numChannels)) * -1);
					vals = vals.unlace(bus.numChannels);
					{
						plot.value = vals;
						plot.domainSpecs_(ControlSpec(dur * -1,0, units:"sec" ));
						plot.setProperties(\labelX,"seconds",\labelY,name.asString);
						// plot.resolution_(1);
						plot.findSpecs_(false);
					}.defer;
				});
			}
		},'/tr', bus.server.addr);
	}

	dur_ { |val|
		dur = val;
		this.prGetNumFrames(pad);
		buffer.free;
		buffer = Buffer.alloc(bus.server, numFrames, bus.numChannels);
		synth.set(\numFrames, numFrames, \bufnum, buffer.bufnum);
		this.prSetOscDef(pad);
		plot.domainSpecs_(ControlSpec(dur * -1,0 ));
	}

	rate_ { |val|
		rate = val;
		synth.set(\rate, rate);
	}

}