Parameter {
	var <name, <value, <spec, <>action, <siblings, <sendOSC, netAddr, <oscMess;
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
		try {val = val.clip(0,1)};
		value = val ? spec.unmap(spec.default);
		sendOSC = false;
	}

	initFromSibling { |sblng|
		siblings.array_(sblng.siblings ++ [sblng]);
		siblings.do{ |i|
			i.siblings.add(this);
		}
	}

	initOSC { |netAd, mess|
		netAddr = netAd ? NetAddr.localAddr;
		oscMess = mess ? name;
		sendOSC = true;
	}

	spec_ { |sp|
		spec = sp.asSpec.deepCopy;
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
		sendOSC.if{
			oscMess = oscMess.dirname ++ "/" ++ name;
		};
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
		sendOSC.if{
			netAddr.sendMsg(oscMess, mapped);
		};
	}

	remove {
		this.changed(\paramRemoved);
	}

	guiClass { ^ParameterGui }
}
