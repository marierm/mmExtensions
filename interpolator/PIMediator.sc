PIMediator {
	var <presetInterpolator, <dict;
	
	*new { |presetInterpolator|
		^super.newCopyArgs(presetInterpolator).init;
	}

	init {
		dict = Dictionary();
		// keys are Presets.
		// values are Lists of parameters.
	}

	register { |parameter|
		dict.keys.includes(parameter.preset).if({
			dict[parameter.preset].add(parameter);
		},{
			dict.put(parameter.preset, List[parameter]);
		});
		parameter.mediator_(this);
	}

	registerPreset { |preset, withSibling=true|
		// If withSibling is true, choose a random sibling-preset and add all
		// the sibling parameters to preset.  If it is false, no parameters
		// are added.  This is used when loading PresetInterpolator from a
		// saved file, which means that the preset already has all the
		// parameters.
		preset.mediator_(this);
		dict.put(preset, List[]);

		withSibling.if({
			dict.choose.do({|sibling|
				preset.prAdd(
					preset.paramClass.newFromSibling(
						sibling, preset
					);
				);
			});
		});
	}

	spec_ { |spec, parameter|
		this.getSiblings(parameter).do({|i|
			i.prSpec_(spec);
		});
		presetInterpolator.mapToParameter;
	}

	name_ { |name, parameter|
		this.getSiblings(parameter).do({|i|
			i.prName_(name);
		});
	}

	add { |parameter|
		parameter.isNil.if({
			dict.keysDo({|preset, i|
				preset.prAdd(
					preset.paramClass.new(preset:preset)
				);
			});
		},{
			dict.keysDo({|preset, i|
				preset.prAdd(
					preset.paramClass.newFromSibling(parameter, preset);
				);
			});
			parameter.free; // the parameter itself is never added, just
			// siblings of it.  We free the synth and bus.
		});
		{		presetInterpolator.buildSynthDef;}.try;
	}

	removePreset {|preset|
		dict[preset].do({|parameter|
			parameter.free;
		});
		preset.free;
		dict.removeAt(preset);
		preset.release;
	}

	remove { |parameter|
		this.getSiblings(parameter).do({|i|
			i.prRemove;
			i.preset.prRemove(i);
			dict[i.preset].remove(i);
			i.release;
		});
		{		presetInterpolator.buildSynthDef;}.try;
	}

	getSiblings { |parameter|
		var id;
		id = dict[parameter.preset].indexOf(parameter);
		^dict.collect(_.at(id)).values;
	}
}