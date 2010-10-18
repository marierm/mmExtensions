PresetInterpolator2 { //Inspired by Marije Baalman's ParameterSpace
	var <>rect, <>points, <presets, <>gui, <>currentPreset, <>currentPoint,
		<>moveAction, <>weights, <>interPoints;
	
	*new{ |view|
		^super.new.gui_(view).init;
	}
	
	init{
		moveAction = { |points, currentPoint, presets, currentPreset|
			//find points that intersect with currentPoint
			//calculate weights
			if(points.indexOfEqual(currentPoint).notNil) {
				//if currentPoint is exactly on a preset
				interPoints = points.selectIndex{|i| i==currentPoint};
				currentPreset.parameters.do{ |i,j|
					i.value_(presets[interPoints[0]].parameters[j].value);
				};
				weights = [1];
			}{
				interPoints = points.selectIndex(_.intersects(currentPoint));
				weights = points[interPoints].collect(
					_.radiusRatio(currentPoint)
				).normalizeSum;
				currentPreset.parameters.do{ |i,j|
					//to each parameters of current Preset
					i.value_( //assign the value
						//of the weighted mean of values
						// of parameters of other presets
						presets[interPoints].collect({|i|
							i.parameters[j].value
						}).wmean(weights));
				};
			};
		};
		rect = Rect(0,0,1,1);
		points = List.new;
		presets = List.new;
		currentPoint = Circle(rect.center);
		this.addFirstPoint(0.25@0.25);
		currentPreset = Preset.newFromSibling(presets[0]);
	}
	
	addFirstPoint { |point|
		if ( rect.contains(point) ) { 
			points.add( Circle(point) );
			presets.add(
				Preset(
					Parameter().action_({
							//changing the parameter value updates currentPreset
							moveAction.value(
								points, currentPoint, presets, currentPreset
							);
						})
				).color_(Color.hsv(0,0.5,0.7,1))
			);
			this.refreshRads;
		} {
			"Point's coordinates should be between 0 and 1".warn
		}
	}
	
	addPoint { |point|
		if ( rect.contains(point) ) { 
			//check if point has pos identical to another point.
			if (points.collect(_.pos).indexOfEqual(point).notNil){
				point = Point(point.x + 0.0001, point.y);
				//ensures weights array does not contain nan.
			};
			
			points.add( Circle(point) );
			this.refreshRads;
			presets.add(Preset.newFromSibling(presets[0])
				.color_(presets.last.color.copy.hue_(
					presets.last.color.hue + 0.094117647058824 % 1)
				)
			);
			presets.last.parameters.do({ |i,j|
				//add the moveAction to all parameters
				i.action = i.action.addFunc({
					moveAction.value(
						points, currentPoint, presets, currentPreset
					);
				});
			});
			this.refresh;
			moveAction.value(
				points, currentPoint, presets, currentPreset
			);
		} {
			"Point's coordinates should be between 0 and 1".warn
		}	
	}
	
	addParameter {
		presets[0].add(Parameter());
		presets.do({ |i|
			i.parameters.last.action = i.parameters.last.action.addFunc({
				moveAction.value(
					points, currentPoint, presets, currentPreset
				);	
			})
		});
	}
	
	removePoint{ |i|
		if (points.size > 1) {
			if (presets[i].gui.notNil){
				presets[i].gui.close;
			};
			points.removeAt(i);
			presets.removeAt(i);
			this.refreshRads;
			if (gui.notNil) {
				gui.grabbedPoint=0;
				gui.grabbed=false;
			};
			this.refresh;
			moveAction.value(
				points, currentPoint, presets, currentPreset
			);
		}
	}
	
	copyPoint{ |id|
		this.addPoint( points[id].pos );
		//copy all parameter values
		presets.last.parameters.do({|i,j|
			i.value_(presets[id].parameters[j].value;)
		});
	}
	
	copyCurrentPoint {
		var vals;
		vals = currentPreset.parameters.collect(_.value);
		this.addPoint( currentPoint.pos );
		//copy all parameter values
		presets.last.parameters.do({|i,j|
			i.value_(vals[j];)
		});
		
	}
	
	refreshRads{
		currentPoint.rad_(currentPoint.findNearestDist(points));
		points.do { |i,j|
			var a;
			a = points.asArray ++ currentPoint;
			a.removeAt(j);
			i.rad_(i.findNearestDist(a));
		}
	}
	
	refresh {
		if (gui.notNil) {
			gui.refresh;
		}
	}

	makeGui { |parent, bounds|
		gui = PresetInterpolatorView(
			parent, bounds
		).space_(this);
		moveAction.value(points, currentPoint, presets, currentPreset);
		^gui;
	}
	
	save { |path|
		path = path ? (Platform.userAppSupportDir ++"/presetInterpolator.pri");
		[
			points,
			presets.collect(_.saveable),
			currentPoint,
			currentPreset.saveable
		].writeArchive(path);
		// // close the guis before saving because there are too many open
		// // functions. If I don't close them, I get only warnings when I save,
		// // but it is impossible to load the object : literal > 256...
		// presets.do{|i,j| if(i.gui.notNil){i.gui.close}};
		// if(currentPreset.gui.notNil){currentPreset.gui.close};
		// [points, presets, currentPoint, currentPreset].writeArchive(path);
		// //Archive.global.put(name, this);
		// //this.writeArchive(path);
	}
	
	load { |path|
		var loaded;
		path = path ? (Platform.userAppSupportDir ++"/presetInterpolator.pri");
		loaded = path.load;
		this.init;						// start from scratch
		// add the loaded points and put them on their position.
		loaded[0].do{|i| this.addPoint(i.pos)};
		// add parameters (-1 because there was already one parameter)
		(loaded[3][2].size-1).do{this.addParameter};
		// the first point was the one added at initialization : remove it
		this.removePoint(0);
		// iterate every presets
		loaded[1].do{|i,j|
			presets[j].name_(i[0]);	// set their name
			presets[j].color_(i[1]);	// set their color
			i[2].do{ |k,l|			 // iterate each parameter of each preset
				presets[j].parameters[l].value_(k[1]); // set their value
			};
		};
		currentPoint = loaded[2];	  // set the position of currentPoint
		loaded[3][2].do{|i,j|	// iterate over each parameter of currentPoint
			currentPreset.parameters[j].name_(i[0]); // set name
			currentPreset.parameters[j].spec_(i[2]); // set spec
			// since other parameters are siblings, their specs are going
			// to be set too.
			currentPreset.parameters[j].setActionString(i[3]); // set action
			// If Parameters of other Presets have action, they will not be
			// set. It does not make sense for them to have action, anyway.
		};

		moveAction.value(points, currentPoint, presets, currentPreset);
		if (gui.notNil) {
			gui.close;
			this.makeGui;
		}
	}
}
