Parameter {

	var <name, <spec, <value, <preset, <sendOSC, <>netAddr, <oscMess,
	oscAction, <>action, <bus, <>mediator;
	//value is unmapped (between 0 and 1);
	
	*new { |name="Parameter", spec, value, preset|
		^super.newCopyArgs(name, spec, value, preset).init;
	}

	*newFromSibling { |sibling, preset|
		//Parameters can have siblings that will share spec and name, but not
		//value.  They are now managed by PIMediator.
		^super.newCopyArgs(
			sibling.name,
			sibling.spec,
			sibling.value,
			preset
		).init;
	}

	*newFromEvent { |ev, preset|
		// Creates a new Parameter from a String returned by .saveable.
		// var ev;
		// ev = string.interpret;
		^super.newCopyArgs(
			ev[\name],
			ev[\spec],
			ev[\value],
			preset,
			ev[\sendOSC],
			NetAddr(*ev[\netAddr]),
			ev[\oscMess]
		).init;
	}


	init {
		preset.notNil.if({
			preset.mediator.register(this);
		});
		spec = spec.asSpec;
		spec.addDependant(this);
		value = value ? spec.unmap(spec.default);
		netAddr = netAddr ? NetAddr.localAddr;
		oscMess = oscMess ? ("/" ++ name);
		sendOSC = sendOSC ? false;
		this.initBus;
	}
	
	initBus {
		bus = Bus.control();
		bus.set(value);
		action = action.addFunc({|mapped, unmapped|
			bus.set(unmapped);
		});
	}


	saveable {
		^(
			name: name,
			spec: spec,
			value: value,
			sendOSC: sendOSC,
			netAddr: [netAddr.ip, netAddr.port],
			oscMess: oscMess
		);
	}
	// initOSC { |netAd, mess|
	// 	netAddr = netAd ? NetAddr.localAddr;
	// 	oscMess = mess ? ("/" ++ name);
	// 	sendOSC = true;
	// }

	sendOSC_ { |bool|
		bool.if({
			sendOSC = true;
			oscAction = {netAddr.sendMsg(oscMess, this.mapped);};
			action = action.addFunc(oscAction);
			this.changed(\OSC, true);
		},{
			sendOSC = false;
			action = action.removeFunc(oscAction);
			this.changed(\OSC, false)
		});
	}
	
	oscMess_ { |string|
		oscMess = string;
		this.changed(\OSC);
	}

	spec_ { |sp|
		mediator.spec_(sp.asSpec, this);
	}

	prSpec_ {|sp|
		spec = sp;
		this.changed(\spec, spec);
	}
	
	name_ { |na|
		mediator.name_(na, this);
	}
	
	prName_ { |na|
		name = na;
		this.changed(\name, name);
		oscMess = oscMess.dirname ++ "/" ++ name;
		this.changed(\OSC);
	}

	remove {
		mediator.remove(this);
	}
	
	prRemove {
		this.free;
		this.changed(\paramRemoved);
	}

	mapped {
		^spec.map(value);
	}
	
	mapped_ { |val|
		this.value_(spec.unmap(val));
	}
	
	value_ { |val|
		var mapped;
		value = val;
		mapped = this.mapped;
		this.changed(\value, mapped, value);
		action.value(mapped, value);
	}

	guiClass { ^ParameterGui2 }

	update { |controlSpec, what ... args|
		// Dependant of its spec only.  this would have to be changed if
		// Parameter comes to depend on something else.

		// Siblings could eventually depend on each other.  This would probably
		// make things simpler.
		this.spec_(controlSpec);
	}

	free {
		bus.free;
	}
}
