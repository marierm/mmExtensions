ParameterServer : Parameter {

	var  <synth, group, server;
	
	// *new { |name, spec, value, preset|
	// 	^super.newCopy(name, spec, value, preset);
	// }
	init {
		server = preset.server;
		group = preset.group;
		siblings = List[];
		spec = spec.asSpec;
		spec.addDependant(this);
		value = value ? spec.unmap(spec.default);
		netAddr = NetAddr.localAddr;
		oscMess = "/" ++ name;
		sendOSC = false;
		sendMIDI = false;
		bus = Bus.control(server);
		bus.set(value);
		action = action.addFunc({|mapped, unmapped| bus.set(unmapped)});
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

	spec_ { |sp|
		spec = sp;
		siblings.do{ |param|
			if (param.spec != spec) {
				param.spec_(spec);
			}
		};
		this.createSynth;
		this.changed(\spec, spec);
	}
	
	free {
		synth.free;
		bus.free;
	}
}
