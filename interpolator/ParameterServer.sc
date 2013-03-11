ParameterServer : Parameter {

	var  <synth, <server, <inBus;
	
	*new { |name, spec, value, server|
		^super.new.init(name, spec, value, server);
	}

	init { |nm, sp, val, serv|
		server = serv ? Server.default;
		name = nm ? "Parameter";
		siblings = List[];
		try {sp = sp.asSpec};
		spec = sp ? ControlSpec();
		spec.addDependant(this);
		try {val = val.clip(0,1)};
		value = val ? spec.unmap(spec.default);
		netAddr = NetAddr.localAddr;
		oscMess = "/" ++ name;
		sendOSC = false;
		sendMIDI = false;
		this.createSynth;
		// action = action.addFunc({|mapped| bus.set(mapped)});
	}

	createSynth {
		var target, addAction, func;
		synth.notNil.if({
			target = synth;
			addAction = \addReplace;
		},{
			target = server;
			addAction = \addToTail;
		});
		func = {
			arg out=0, in=0;
			Out.kr(out, spec.map(in))
		};
		{
			server.sync;
			bus = Bus.control(server);
			SynthDef("spec" ++ func.hash.asString, func).add;
			server.sync;
			synth = Synth(
				"spec" ++ func.hash.asString,
				[\out, bus],
				target, 
				addAction
			).map(\in, inBus);
		}.fork;
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
}
