ParameterServer : Parameter {

	var  <synth, group, server;
	
	// *new { |name, spec, value, preset|
	// 	^super.newCopy(name, spec, value, preset);
	// }
	init {
		super.init;
		server = preset.server;
		group = preset.group;
		this.createSynth;
	}

	createSynth {
		var target, addAction, func;
		{
			bus.free;
			server.sync;
			synth.notNil.if({
				target = synth;
				addAction = \addReplace;
			},{
				target = group;
				addAction = \addToTail;
			});
			func = {
				arg out=0, in=0;
				Out.kr(out, spec.map(in))
			};
			server.sync;
			bus = Bus.control(server);
			SynthDef("spec" ++ func.hash.asString, func).add;
			server.sync;
			synth = Synth(
				"spec" ++ func.hash.asString,
				[\out, bus],
				target, 
				addAction
			);// .map(\in, inBus);
		}.forkIfNeeded;
	}

	prSpec_ {|sp|
		spec = sp;
		this.createSynth;
		this.changed(\spec, spec);
	}

	free {
		synth.free;
		bus.free;
	}
}
