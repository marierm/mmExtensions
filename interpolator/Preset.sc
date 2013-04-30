Preset {
	var <name, <presetInterpolator, <parameters, <>actions, <>mediator;

	*new { arg params, name="Preset", presetInterpolator;
		^super.newCopyArgs(name, presetInterpolator).init.initParams(params);
	}

	*newFromSibling { arg sibling, name="Preset";
		^super.newCopyArgs(
			name, 
			sibling.presetInterpolator
		).init.initFromSibling(sibling);
	}
	
	*load { |path|
		//does not consider siblings
		var e;
		e = path.load;
		^this.new(e.at(\parameters), e.at(\name));
	}

	initFromSibling { arg sibling;
		sibling.mediator.registerPreset(this);
	}

	initParams {|params|
		params.do({ |i, j|
			this.add(i);
		});
	}

	init {
		presetInterpolator.isNil.if({
			PIMediator().registerPreset(this);
		},{
			presetInterpolator.mediator.registerPreset(this);
		});
		parameters = List[];
		actions = IdentityDictionary[
			\value -> {|param, what, val|
				this.changed(\paramValue, param, val);
			}
		];
	}
	
	paramClass {
		^Parameter;
	}

	add { |parameter|
		mediator.add(parameter);
	}

	prAdd { |parameter|
		parameters.add(parameter);		
		this.changed(\paramAdded, parameter);
	}

	removeAt { |id|
		mediator.remove(parameters[id]);
	}

	prRemove { |parameter|
		var id;
		id = parameters.indexOf(parameter);
		parameters.remove(parameter); // Should be done by Mediator?
		this.changed(\paramRemoved, parameter, id);
	}
	
	remove { |parameter|
		mediator.remove(parameter);
	}

	name_ { |nm|
		name = nm.asString;
		this.changed(\presetName, nm);
	}

	at {|i|
		^parameters[i];
	}

	update { arg model, what ... args;
		var action;
		action = actions.at(what);
		if (action.notNil, {
			action.valueArray(model, what, args);
		});
	}

	size {
		^parameters.size;
	}

	randomizeParameters {
		parameters.do({ |i|
			i.value_(1.0.rand);
		});
	}

	saveable {
		^(name: name, parameters: parameters);
	}

	guiClass { ^PresetGui2 }
	
	free {}
}

PresetServer : Preset {
	var <server, <group;

	init {
		super.init;
		server = presetInterpolator.model.server;
		group = ParGroup(presetInterpolator.model.group,\addToTail);
	}

	paramClass {
		^ParameterServer;
	}

	free {
		group.free;
	}

	prAdd { |parameter|
		parameters.add(parameter);		
		// this.changed(\cursorParamAdded, parameter);
		this.changed(\paramAdded, parameter);
	}

	prRemove { |parameter|
		var id;
		id = parameters.indexOf(parameter);
		parameters.remove(parameter); // Should be done by Mediator?
		// this.changed(\cursorParamRemoved, parameter, id);
		this.changed(\paramRemoved, parameter, id);
	}

}