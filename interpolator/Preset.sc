Preset {
	var <parameters, <name, <>siblings, <>actions, <presetInterpolator;

	*new { arg params, name, presetInterpolator;
		^super.new.init(name, presetInterpolator).initParams(params);
	}

	*newFromSibling { arg sibling, name, presetInterpolator;
		^super.new.init(name, presetInterpolator).initFromSibling(sibling);
	}
	
	*load { |path|
		//does not consider siblings
		var e;
		e = path.load;
		^this.new(e.at(\parameters), e.at(\name));
	}

	initFromSibling { arg sibling;
		sibling.parameters.do { |i, j|
			this.add(this.paramClass.newFromSibling(i, this));
			parameters[j].addDependant(this);
		};
		siblings = List.newUsing(sibling.siblings ++ [sibling]);
		siblings.do{ |i|
			i.siblings.add(this);
		};
	}

	initParams {|params|
		params.do({ |i, j|
			this.add(i);
			i.addDependant(this);
		});
	}

	init { arg nm, presetInt;
		parameters = List[];
		name = nm ? "Preset";
		presetInterpolator = presetInt;
		actions = IdentityDictionary[
			\paramRemoved -> {|param, what|
				this.remove(param);
			},
			\value -> {|param, what, val|
				this.changed(\paramValue, param, val);
			}
		];
		siblings = List[];
	}
	
	paramClass {
		^Parameter;
	}

	add { |thing|
		thing.class.switch(
			this.paramClass, { // if arg is an appropriate Parameter
				parameters.add(thing);
				thing.addDependant(this);
				siblings.do { |i|
					(i.parameters.size != parameters.size).if {
						i.add(
							i.paramClass.newFromSibling(thing, i);
						);
					}
				};
				this.changed(\paramAdded, thing);
			},
			Nil, { // if arg is nil, add a "default Parameter"
				this.add(this.paramClass.new(preset:this))
			},
			{"Cannot add this Parameter.".warn;}
		);
	}

	removeAt { |id|
		// ( id < parameters.size && parameters.size > 1).if {
			parameters.removeAt(id);
			siblings.do { |i|
				(i.parameters.size != parameters.size).if {
					i.removeAt(id);
				}
			};
			this.changed(\paramRemoved, parameters[id], id);
		// } {
		// 	"This Parameter cannot be removed".warn;
		// }
	}

	remove { |thing|
		var id;
		// ( parameters.size > 1).if {
			id = parameters.indexOf(thing);
			parameters.remove(thing);
			siblings.do { |i|
				(i.parameters.size != parameters.size).if {
					i.removeAt(id);
				}
			};
			this.changed(\paramRemoved, thing, id);
		// } {
		// 	"This Parameter cannot be removed".warn;
		// }
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
}

PresetServer : Preset {
	var <server, <group;
	
	init { arg nm, presetInt;
		parameters = List[];
		name = nm ? "Preset";
		presetInterpolator = presetInt;
		server = presetInterpolator.model.server;
		group = ParGroup(presetInterpolator.model.group,\addToTail);
		actions = IdentityDictionary[
			\paramRemoved -> {|param, what|
				this.remove(param);
			},
			\value -> {|param, what, val|
				this.changed(\paramValue, param, val);
			}
		];
		siblings = List[];
	}

	paramClass {
		^ParameterServer;
	}

	free {
		group.free;
	}
}