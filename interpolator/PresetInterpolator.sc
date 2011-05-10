//Inspired by Marije Baalman's ParameterSpace
PresetInterpolator : SimpleController {
	var <presets, <cursor, <>action, <colors;

	*new { arg model;
		model = model ? Interpolator();
		^super.newCopyArgs(model).init
	}

	init {
		model.addDependant(this);
		cursor = Preset();
		presets = List[];
		colors = ColorList();
		model.points.do{
			presets.add(Preset.newFromSibling(cursor));
			colors.addNext;
		};
		presets.do{ |i|
			// make the interpolator refresh the weights when a preset is
			// modified.
			i.addDependant(this);
		};
		actions = IdentityDictionary[
			\weights -> {|model, what, interPoints, weights|
				cursor.parameters.do{ |i,j|
					//to each parameters of current Preset
					i.value_( //assign the value
						//of the weighted mean of values
						// of parameters of other presets
						presets[interPoints].collect({|i|
							i.parameters[j].value
						}).wmean(weights));
				};
			},
			\pointAdded -> {|interpolator, what, point|
				colors.addNext;
				presets.add(Preset.newFromSibling(cursor));
				presets.last.addDependant(this);
			},
			\pointDuplicated -> {|interpolator, what, pointId|
				colors.addNext;
				presets.add(Preset.newFromSibling(cursor));
				presets.last.addDependant(this);
				presets.last.parameters.do{ |i,j|
					i.value_(presets[pointId].parameters[j].value);
				};
			},
			\cursorDuplicated ->{|interpolator, what|
				colors.addNext;
				presets.add(Preset.newFromSibling(cursor));
				presets.last.addDependant(this);
				presets.last.parameters.do{ |i,j|
					i.value_(cursor.parameters[j].value);
				};
			},
			\pointRemoved -> {|interpolator, what, i|
				colors.removeAt(i);
				presets.removeAt(i);
			},
			\makeCursorGui -> {|model, what|
				cursor.gui.background_(Color.clear);
			},
			\makePointGui -> {|model, what, point|
				presets[point].gui.background_(ColorList.get(point));
			},
			\paramValue -> {|preset, what, param, paramId, val|
				model.moveAction.value;
			}
		];
	}
	
	guiClass { ^PresetInterpolatorGui }
}