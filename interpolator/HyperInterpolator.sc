// Copyright 2010 Martin Marier

HyperInterpolator { //More than 2 dimensions (uses KDTree)

	var <>points, <rads, <presets, <>currentPreset, <>currentPoint,
	<>currentRad, <>moveAction, <weights, <interPoints, <n, <>gui,
	<>grabbed;
	
	*new{ |numDim = 3|
		^super.new.init(numDim);
	}
	
	init{ |numDim|
		moveAction = { |points, currentPoint, presets, currentPreset, rads, currentRad|
			//find points that intersect with currentPoint
			//calculate weights
			if(points.indexOfEqual(currentPoint).notNil) {
				// if currentPoint is exactly on a preset
				interPoints = [ points.indexOfEqual(currentPoint) ];
				currentPreset.parameters.do{ |i,j|
					i.value_(presets[interPoints[0]].parameters[j].value);
				};
				weights = [1];
			}{
				interPoints = points.selectIndex({ |i,j|
					i.dist(currentPoint) <= (currentRad + rads[j])
				});
				// if (interPoints.size == 1) {
				// 	weights = [1];
				// }{
				weights = points[interPoints].collect({ |i,j|
					i.intersectArea(rads[interPoints[j]], currentPoint, currentRad)
					/ (pi * rads[interPoints[j]].squared);
				}).normalizeSum;
				// };
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
		grabbed = List.new;
		presets = List.new;
		currentPoint = 0!n;
		this.addFirstPoint;
		currentPreset = Preset.newFromSibling(presets[0]);
	}
	
	addFirstPoint {
		points.add( 1!n );
		rads.add(0);
		grabbed.add(false);
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
			grabbed.add(false);
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
			grabbed.removeAt(i);
			this.refreshRads;
			moveAction.value(
				points, currentPoint, presets, currentPreset, rads, currentRad
			);
		}
	}
	
	copyPoint{ |id|
		this.addPoint( points[id] );
		//copy all parameter values
		presets.last.parameters.do({|i,j|
			i.value_(presets[id].parameters[j].value;)
		});
	}
	
	copyCurrentPoint {
		var vals;
		vals = currentPreset.parameters.collect(_.value);
		this.addPoint( currentPoint );
		//copy all parameter values
		presets.last.parameters.do({|i,j|
			i.value_(vals[j];)
		});
		
	}

	learn { // Not so useful: like copyCurrentPoint but does not copy parameter
			// values.
		var newPoint;
		newPoint = currentPoint;
		this.addPoint( newPoint );
		("Added a new point at" + newPoint).postln;
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
			// Check if a point is grabbed and move it along with the current
			// point.
			grabbed.indicesOfEqual(true).do{ |point|
				this.movePoint(point, pos);
				if (gui.notNil) {
					gui.matrix[point].do{|numBox, i|
						numBox.value_(points[point][i]);
					};
				}
			};
		} {
			"Position must be an array of size %".format(n).postln;
		}
	}
	
	// Makes a point move along with the currentPoint(cursor).  Very handy to
	// place a point the a multi dimensional space using an interface (the
	// sponge!)
	grab { |point| 
		grabbed[point] = true;
	}

	ungrab { |point| 
		grabbed[point] = false;
	}

	changeCoord { |point, coord, val|
		points[point][coord] = val;
		this.refreshRads;
		moveAction.value(
			points,
			currentPoint,
			presets,
			currentPreset,
			rads,
			currentRad
		);
	}

	movePoint { |point, pos|
		if (pos.size == n) {
			points[point] = pos;
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
			a = a ++ [currentPoint];
			rads[j] = i.nearestDist(a);
		}
	}
	

	// Does not save if a point is grabbed at the moment.
	// but is it useful anyway?
	save { |path|
		path = path ? (Platform.userAppSupportDir ++"/presetInterpolator.pri");
		[
			points,
			presets.collect(_.saveable),
			currentPoint,
			currentPreset.saveable,
			rads,
			currentRad,
			n
		].writeArchive(path)
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
		this.init(loaded[6]);						// start from scratch
		// add the loaded points and put them on their position.
		loaded[0].do{|i| this.addPoint(i)};
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

	makeGui { |pos, parent|
		gui = HyperInterpolatorGui(this, parent, pos);
		// moveAction.value(points, currentPoint, presets, currentPreset);
		^gui;
	}

}

HyperInterpolatorGui {

	var <>w, <>x, <>y, <>space, <>grabAction, <>ungrabAction, <>matrix;
	
	*new { |interpolator, w, pos| 
		^super.new.w_(w).init(interpolator, pos);
	}
	
 	init { | interpolator, pos |
		var grabbed;
		pos = pos ? Point(550,400);
		space = interpolator ? HyperInterpolator();
		w = try { CompositeView(w, Rect().origin_(pos).extent_(300@85)) }
			?? { 
				w = Window(
					"HyperInterpolator",
					Rect().origin_(pos).extent_(
						((52*space.n) + 105)@(18*(space.points.size + 1))
					)
				).alwaysOnTop_(false).front; 
				w.view;
			};
		matrix = List[];

		// add button
		Button(w, Rect()
			.origin_(2@2)
			.extent_(97@15)
		)
		.states_([["add"]])
		.action_({|b|
			var origin;
			origin = this.w.findWindow.bounds.origin;
			origin = origin - (0@18);
			this.w.findWindow.close;
			space.addPoint(1!space.n);
			space.makeGui(origin);
		});

		// show interpolation point button
		Button(w, Rect()
			.origin_(101@2)
			.extent_(100@15)
		)
		.states_([["interpolPoint"]])
		.action_({
			if(space.currentPreset.gui.notNil){
				space.currentPreset.gui.close;
			} {
				space.currentPreset.makeGui(
					origin: Point(
						this.w.findWindow.bounds.right,
						this.w.findWindow.bounds.top
					)
				);
			}
		});

		// save button
		Button(w, Rect()
			.origin_(203@2)
			.extent_(50@15)
		)
		.states_([["save"]])
		.action_({
			Dialog.savePanel({ arg path;
				space.save(path);
			});
		});

		// load button
		Button(w, Rect()
			.origin_(255@2)
			.extent_(50@15)
		)
		.states_([["load"]])
		.action_({
			var origin;
			origin = this.w.findWindow.bounds.origin;
			Dialog.getPaths({ arg paths;
				this.w.findWindow.close;
				space.load(paths[0]);
				space.makeGui(origin);
			});
		});
		
		// edit buttons
		space.points.do{|point, i|
			Button(w, Rect()
				.origin_(2@((i*17)+20))
				.extent_(40@15))
			.states_([["edit", Color.black, space.presets[i].color]])
			.action_({
				if(space.presets[i].gui.notNil){
					space.presets[i].gui.close;
				} {
					space.presets[i].makeGui(
						origin: Point(
							this.w.findWindow.bounds.right,
							this.w.findWindow.bounds.top
						)
					);
				}
			});
			// grab buttons
			Button(w, Rect()
				.origin_(44@((i*17)+20))
				.extent_(55@15))
			.states_([
				["grab", Color.black, space.presets[i].color],
				["grabbed"]
			])
			.action_({|but|
				if (but.value == 0) {
					ungrabAction.value(i, but);
					space.ungrab(i);
					grabbed = nil;
				} {
					// grabbed buttons are exclusive in the gui, but not
					// necessarily in the model.  Not sure if it makes sense.
					try{grabbed.valueAction_(0)};
					grabAction.value(i,but);
					space.grab(i);
					grabbed = but;
				}
			});
			// coordinates
			matrix.add(
				Array.fill(point.size, {|j|
					NumberBox(w, Rect()
						.origin_(((j*50)+101)@((i*17)+20))
						.extent_(50@15))
					.value_(point[j])
					.action_({|nb|
						space.changeCoord(i, j, nb.value);
					});
				})
			);

			// remove button
			Button( w, Rect()
				.origin_((101+(50 * space.n))@((i*17)+20))
				.extent_(15@15))
			.states_([["X"]])
			.action_({
				if (space.points.size > 1) {
					var origin;
					origin = this.w.findWindow.bounds.origin;
					origin = origin + (0@18);
					this.w.findWindow.close;
					space.removePoint(i);
					space.makeGui(origin);
				}
			});
		};
	}
}