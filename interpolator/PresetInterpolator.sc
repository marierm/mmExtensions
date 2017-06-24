//Inspired by Marije Baalman's ParameterSpace
PresetInterpolator {
	var <model, actions, <presets, <cursor, <mediator;

	*new { arg model;
		model = model ? Interpolator();
		^super.newCopyArgs(model).init;
	}

	*newWithDimensions { arg numDim = 2;
		model = Interpolator(numDim);
		^super.newCopyArgs(model).init;
	}

	update { arg theChanger, what ... moreArgs;
		var action;
		action = actions.at(what);
		if (action.notNil, {
			action.valueArray(theChanger, what, moreArgs);
		});
	}

	*load { |path|
		var e;
		e = path.load;
		^super.newCopyArgs(
			Interpolator(e.at(\points)[0].size)
		).init.initWithEvent(e);
	}

	*loadOld { |path|
		var e;
		e = path.load;
		^super.newCopyArgs(
			Interpolator(e.at(\points)[0].size)
		).init.initWithEventOld(e);
	}

	// used by .load
	// e should be structured like this:
	// (
	// points: [point,point,...],
	// cursor: cursor.saveable,
	// cursorPos: cusorPosition, (array)
	// presets: [preset.saveable, preset.saveable, ...]
	// colors: [color, color, ...]
	// )
	initWithEvent { |e|
		model.cursor_(e.at(\cursorPos));
		//move point 0 (it is already there) and remove it from the event.
		model.movePoint(0, e.at(\points)[0]);
		e.at(\points).removeAt(0);
		// add all other points
		e.at(\points).do{|i|
			model.add(i);
		};
		// add parameters to cursor.
		// they will be added to other points as well (they are siblings).
		e.at(\cursor).at(\parameters).do{|i|
			var p;
			p = Parameter(i.name, i.spec, i.value, cursor);
			p.netAddr_(i.netAddr);
			p.oscMess_(i.oscMess);
			p.sendMIDI_(i.sendMIDI);
			p.sendOSC_(i.sendOSC);
			cursor.add(p);
		};
		// name presets set their parameter values.
		presets.do{ |i,j|
			i.name_(e.at(\presets)[j].at(\name));
			i.parameters.do{|k,l|
				k.value_(e.at(\presets)[j].at(\parameters)[l].value);
			};
		};
		// set colors
		model.colors_(e.at(\colors));
	}

	initWithEventOld { |e|
		model.cursor_(e.at(\cursor));
		//move point 0 (it is already there) and remove it from the event.
		model.movePoint(0, e.at(\points)[0]);
		e.at(\points).removeAt(0);
		// add all other points
		e.at(\points).do{|i|
			model.add(i);
		};
		// add parameters to cursor.
		// they will be added to other points as well (they are siblings).
		e.at(\presets)[0].at(\parameters).do{|i|
			cursor.add(
				Parameter().name_(i.name).spec_(i.spec);
			)
		};
		// name presets set their parameter values.
		presets.do{ |i,j|
			i.name_(e.at(\presets)[j].at(\name));
			i.parameters.do{|k,l|
				k.value_(e.at(\presets)[j].at(\parameters)[l].value);
			};
		};
		// set colors
		model.colors_(e.at(\colors));
	}

	init {
		mediator = PIMediator(this);
		model.addDependant(this);
		cursor = Preset(nil, "cursor");
		presets = List[];
		model.points.do{
			presets.add(Preset.newFromSibling(cursor));
		};
		presets.do{ |i|
			// make the interpolator refresh the weights when a preset is
			// modified.
			i.addDependant(this);
		};
		this.initActions;
	}

	initActions {
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
				presets.add(Preset.newFromSibling(cursor));
				presets.last.addDependant(this);
				this.changed(\presetAdded, presets.last);
			},
			\pointDuplicated -> {|interpolator, what, point, pointId|
				var paramValues;
				paramValues = presets[pointId].parameters.collect(_.value);
				model.add(point);
				presets.last.parameters.do{ |i,j|
					i.value_(paramValues[j].value);
				};
				this.changed(\presetAdded, presets.last);
			},
			\cursorDuplicated ->{|interpolator, what, point, pointId|
				var paramValues;
				paramValues = cursor.parameters.collect(_.value);
				model.add(point);
				presets.last.parameters.do{ |i,j|
					i.value_(paramValues[j].value);
				};
				this.changed(\presetAdded, presets.last);
			},
			\pointRemoved -> {|interpolator, what, i|
				presets.removeAt(i);
				mediator.removePreset(presets[i]);
				this.changed(\presetRemoved, i);
			},
			\makeCursorGui -> {|model, what|
				cursor.gui.background_(Color.clear);
			},
			\makePointGui -> {|model, what, point, color|
				presets[point].gui.background_(color);
			},
			\paramValue -> {|preset, what, param, paramId, val|
				model.moveAction.value;
			},
			\presetName -> {|preset, what, name|
				this.changed(
					\presetName, presets.indexOf(preset), name
				);
			},
			\attachedPoint -> {|interpolator, what, point|
				this.changed(\attachedPoint, point);
			}
		];
	}

	save { |path|
		path = path ? (Platform.userAppSupportDir ++"/scratchPreset.pri");
		(
			points: model.points,
			colors: model.colors,
			cursor: cursor.saveable,
			cursorPos: model.cursor,
			presets: presets.collect(_.saveable)
		).writeArchive(path);
	}

	// access to model
	getPresetColor {|i|
		^model.colors[i];
	}

	numDim {
		^model.n;
	}

	cursorPos {
		^model.cursor;
	}

	cursorPos_ {|pos|
		// var newPos;
		// i is the index of an axis (0 for, 1 for y, 2 for z, ...)
		// pos is the position on the axis
		// newPos = model.cursor;
		// newPos[i] = pos;
		model.cursor_(pos);
	}

	action_ { |function|
		model.action_(function);
	}

	attachedPoint_ {|i|
		model.attachedPoint_(i);
	}

	attachedPoint {
		^model.attachedPoint;
	}

	initOSC { |netAd, mess|
		mess = mess ? "/PresetInterpolator";
		cursor.parameters.do{|i|
			i.initOSC(netAd, mess ++ "/" ++ i.name);
		};
	}

	connect { |axis, feature|
		model.connect(axis, feature);
	}

	disconnect { |axis|
		model.disconnect(axis);
	}
	// gui stuff
	guiClass { ^PresetInterpolatorFullGui }

	interpolatorGui { arg  ... args;
		^InterpolatorGui.new(model).performList(\gui,args);
	}

	gui2D { arg  ... args;
		^Interpolator2DGui.new(model).performList(\gui,args);
	}

	namesGui { arg  ... args;
		^PresetInterpolatorGui.new(this).performList(\gui,args);
	}
}
