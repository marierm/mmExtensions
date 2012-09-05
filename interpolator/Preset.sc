Preset {
	var <parameters, <name, <>siblings, <>actions;

	*new { arg params, name;
		^super.new.init(params, name);
	}

	*newFromSibling { arg sibling, name;
		^super.new.initFromSibling(sibling, name);
	}
	
	*load { |path|
		//does not consider siblings
		var e;
		e = path.load;
		^this.new(e.at(\parameters), e.at(\name));
	}

	initFromSibling { arg sibling, nm;
		name = nm ? "Preset";
		parameters = List[];
		sibling.parameters.do { |i, j|
			this.add(Parameter.newFromSibling(i));
			parameters[j].addDependant(this);
		};
		siblings = List.newUsing(sibling.siblings ++ [sibling]);
		siblings.do{ |i|
			i.siblings.add(this);
		};
		actions = IdentityDictionary[
			\paramRemoved -> {|param, what|
				this.remove(param);
			},
			\value -> {|param, what, val|
				this.changed(\paramValue, param, parameters.indexOf(param), val);
			}
		];
	}

	init { arg params, nm;
		name = nm ? "Preset";
		parameters = List[];
		params.do({ |i, j|
			this.add(i);
			i.addDependant(this);
		});
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
	
	add { |thing|
		(thing.class == Parameter).if {
			parameters.add(thing);
			thing.addDependant(this);
			siblings.do { |i|
				(i.parameters.size != parameters.size).if {
					i.add(
						Parameter.newFromSibling(thing);
					);
				}
			};
			this.changed(\paramAdded, thing);
		} {
			"Not a Parameter".warn;
		}
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

	makeWindow { ^PresetWindow(this) }
}
