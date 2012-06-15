Parameter {

	var <name, <value, <spec, <>action, <siblings, <sendOSC, <>netAddr,
	<oscMess, <>sendMIDI, <>midiPort, <>midiCtl, <>midiChan, oscAction;
	//value is unmapped (between 0 and 1);
	
	*new { |name, spec, value|
		^super.new.init(name, spec, value);
	}

	*newFromSibling { |sibling|
		//Parameters can have siblings that will share spec, name
		//(but not value)
		^super.new.init(
			nm: sibling.name,
			sp: sibling.spec
		).initFromSibling(sibling);
	}
	
	init { |nm, sp, val|
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
	}

	initFromSibling { |sblng|
		siblings.array_(sblng.siblings ++ [sblng]);
		siblings.do{ |i|
			i.siblings.add(this);
		}
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
		spec = sp;
		siblings.do{ |param|
			if (param.spec != spec) {
				param.spec_(spec);
			}
		};
		this.changed(\spec, spec);
	}
	
	name_ { |na|
		name = na;
		siblings.do{ |param|
			if (param.name != name) {
				param.name_(name);
			}
		};
		this.changed(\name, name);
		// sendOSC.if{
			oscMess = oscMess.dirname ++ "/" ++ name;
		// };
		this.changed(\OSC);
	}

	mapped {
		^spec.map(value);
	}
	
	mapped_ { |val|
		this.value_(spec.unmap(val));
	}
	
	value_ { |val|
		var mapped;
		mapped = this.mapped;
		value = val;
		this.changed(\value, mapped, value);
		action.value(this.mapped, value);
	}

	remove {
		this.changed(\paramRemoved);
	}

	guiClass { ^ParameterGui2 }

	update { |controlSpec, what ... args|
		// Dependant of its spec only.  this would have to be changed if
		// Parameter comes to depend on something else.

		// Siblings could eventually depend on each other.  This would probably
		// make things simpler.
		this.spec_(controlSpec);
	}
}
