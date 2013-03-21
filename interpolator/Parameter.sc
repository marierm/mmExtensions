Parameter {

	var <name, <spec, <value, <preset, <>action, <sendOSC, <>netAddr,
	<oscMess, <>sendMIDI, <>midiPort, <>midiCtl, <>midiChan, oscAction, <bus,
	<>mediator;
	//value is unmapped (between 0 and 1);
	
	*new { |name="Parameter", spec, value, preset|
		^super.newCopyArgs(name, spec, value, preset).init;
	}

	*newFromSibling { |sibling, preset|
		//Parameters can have siblings that will share spec, name
		//(but not value)
		^super.newCopyArgs(
			sibling.name,
			sibling.spec,
			sibling.value,
			preset
		).init.initFromSibling(sibling);
	}

	init {
		preset.notNil.if({
			preset.mediator.register(this);
		});
		spec = spec.asSpec;
		spec.addDependant(this);
		value = value ? spec.unmap(spec.default);
		netAddr = NetAddr.localAddr;
		oscMess = "/" ++ name;
		sendOSC = false;
		sendMIDI = false;
		bus = Bus.control();
		bus.set(value);
		action = action.addFunc({|mapped, unmapped| bus.set(unmapped)});
	}

	initFromSibling { |sibling|
		sibling.mediator.register(this);
	}

	// mediator {
	// 	mediator.isNil.if({
	// 		mediator = Mediator();
	// 		mediator.register(this);
	// 	});
	// 	^mediator;
	// }

	*load { |eventString|

	}

	saveable {
		^(
			name: name,
			spec: spec,
			value: value,
			sendOSC: sendOSC,
			netAddr: [netAddr.ip, netAddr.port],
			oscMess: oscMess,
			sendMIDI: sendMIDI,
			midiPort: midiPort,
			midiCtl: midiCtl,
			midiChan: midiChan
		).asCompileString;
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
		// sendMIDI.if{
		// 	midiPort.control(midiChan, midiCtl, \midi.asSpec.map(value));
		// };
	}
	
	oscMess_ { |string|
		oscMess = string;
		this.changed(\OSC);
	}

	initMIDI { |portNum, chan, ctl|
		MIDIClient.initialized.not.if{
			MIDIClient.init;
		};
		midiPort = MIDIOut(portNum);
		midiChan = chan;
		midiCtl = ctl;
		sendMIDI = true;
	}

	spec_ { |sp|
		mediator.spec_(sp, this);
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
