// Copyright 2010 Martin Marier

HyperInterpolator { //More than 2 dimensions (uses KDTree)
	var <>points,  <rads, <presets, <>currentPreset, <>currentPoint, <>currentRad,
	<>moveAction, <weights, <interPoints, n;
	
	*new{ |numDim = 2|
		^super.new.init(numDim);
	}
	
	init{ |numDim|
		moveAction = { |points, currentPoint, presets, currentPreset, rads, currentRad|
			//find points that intersect with currentPoint
			//calculate weights
			if(points.indexOfEqual(currentPoint).notNil) {
				//if currentPoint is exactly on a preset
				interPoints = [ points.indexOfEqual(currentPoint) ];
				currentPreset.parameters.do{ |i,j|
					i.value_(presets[interPoints[0]].parameters[j].value);
				};
				weights = [1];
			}{
				interPoints = points.selectIndex({ |i,j|
					i.dist(currentPoint) <= (currentRad + rads[j])
				});
				// weights = points[interPoints].collect({ |i,j|
				// 	((
				// 		(
				// 			(4 * i.dist(currentPoint).squared * currentRad.squared) -
				// 			(
				// 				i.dist(currentPoint).squared -
				// 				rads[interPoints[j]].squared +
				// 				currentRad.squared
				// 			).squared;
				// 		) / 4 * i.dist(currentPoint).squared
				// 	).sqrt )/rads[interPoints[j]];
				// }).normalizeSum;
				weights = points[interPoints].collect({ |i,j|
					i.intersectArea(rads[interPoints[j]], currentPoint, currentRad)
					/ (pi * rads[interPoints[j]].squared);
				}).normalizeSum;

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
		n = numDim;
		points = List.new;
		rads = List.new;
		presets = List.new;
		currentPoint = 0!n;
		this.addFirstPoint;
		currentPreset = Preset.newFromSibling(presets[0]);
	}
	
	addFirstPoint {
		points.add( 1!n );
		rads.add(0);
		presets.add(
			Preset(
				Parameter().action_({
					//changing the parameter value updates currentPreset
					moveAction.value(
						points, currentPoint, presets, currentPreset, rads, currentRad
					);
				})
			).color_(Color.hsv(0,0.5,0.7,1))
		);
		this.refreshRads;
	}
	
	addPoint { |point|
		//check if point has correct num of coordinates.
		if (point.size == n) {
			//check if point has pos identical to another point.
			while {points.indexOfEqual(point).notNil} {
				point[0] = point[0] + 0.001;
				//ensures weights array does not contain nan.
			};
			
			points.add( point );
			rads.add( 0 );
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
						points, currentPoint, presets, currentPreset, rads, currentRad
					);
				});
			});
			moveAction.value(
				points, currentPoint, presets, currentPreset, rads, currentRad
			);
		} {
			"Point must be an array of size %".format(n).postln;
		}
	}
	
	addParameter {
		presets[0].add(Parameter());
		presets.do({ |i|
			i.parameters.last.action = i.parameters.last.action.addFunc({
				moveAction.value(
					points, currentPoint, presets, currentPreset, rads, currentRad
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
			rads.removeAt(i);
			this.refreshRads;
			moveAction.value(
				points, currentPoint, presets, currentPreset, rads, currentRad
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

	movePoint {
		
	}

	moveCurrentPoint { |pos|
		if (pos.size == n) {
			currentPoint = pos;
			this.refreshRads;
			moveAction.value(
				points,
				currentPoint,
				presets,
				currentPreset,
				rads,
				currentRad
			);
		} {
			"Position must be an array of size %".format(n).postln;
		}
	}
	
	refreshRads{
		currentRad = (currentPoint.nearestDist(points));
		points.do { |i,j|
			var a;
			a = points.deepCopy;
			a.removeAt(j);
			a = a ++ currentPoint;
			rads[j] = i.nearestDist(a);
		}
	}
	
	save { |path|
		path = path ? (Platform.userAppSupportDir ++"/presetInterpolator.pri");
		[
			points,
			presets.collect(_.saveable),
			currentPoint,
			currentPreset.saveable,
			rads,
			currentRad
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
		path = path ? (Platform.userAppSupportDir ++ "/hyperInterpolator.hpi");
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
		currentRad = loaded[5];		  // set the Radius of currentPoint
		rads = loaded[4];
		loaded[3][2].do{|i,j|	// iterate over each parameter of currentPoint
			currentPreset.parameters[j].name_(i[0]); // set name
			currentPreset.parameters[j].spec_(i[2]); // set spec
			// since other parameters are siblings, their specs are going
			// to be set too.
			currentPreset.parameters[j].setActionString(i[3]); // set action
			// If Parameters of other Presets have action, they will not be
			// set. It does not make sense for them to have action, anyway.
		};

		moveAction.value(points, currentPoint, presets, currentPreset, rads, currentRad);
	}
}
