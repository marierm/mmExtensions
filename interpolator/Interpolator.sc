// Copyright 2011-2012 Martin Marier

Interpolator {
	var <n, <points, <cursor, algo;
	
	*new { |numDim = 2, algo|
		^super.newCopyArgs(numDim).init(algo);
	}
	
	init { |algorithm|
		points = List[];
		cursor = InterpolatorCursor(this, 0.5 ! n);
		this.algo_(algorithm);
		this.add( 0!n );
	}

	calculateWeights {
		var weights;
		weights = algo.value(cursor, points);
		points.do({|i,j|
			i.weight_(weights[j]);
		});
	}
		
	algo_ { |algorithm|
		algo = algorithm ? { |cursor, points|
			var cursorOnPoint=false; // True if the cursor is exactly on a point.
			var radii, cursorRadius, weights;

			weights = Array.fill(points.size, 0);

			points.do({|i,j|
				(i.position == cursor.position).if({
					weights[j] = 1.0;
					cursorOnPoint = true;
				});
			});

			cursorOnPoint.not.if({
				// calculate radii
				radii = points.collect({ |i|
					var otherPoints;
					otherPoints = points ++ [cursor];
					otherPoints.remove(i);
					i.nearestDist(otherPoints);
				});
				cursorRadius = cursor.nearestDist(points);
				// calculate weights
				points.do({ |i,j|
					// only if circles intersect
					(i.dist(cursor) < (cursorRadius + radii[j])).if({
						weights[j] = i.position.intersectArea(
							radii[j], cursor.position, cursorRadius
						) / (pi * radii[j].squared);
					});
				});
			});
			
			weights.normalizeSum;
		};
	}

	add { |pos|
		var pt;
		pt = InterpolatorPoint(this, pos);
		points.add( pt );
		this.changed(\pointAdded, pt);
		this.calculateWeights;
		^pt;
	}

	allPoints {
		^(points ++ List[cursor]);
	}

	// gui stuff
	makeWindow {
		^InterpolatorWindow(this);
	}

	makeGraph { |x=0, y=1, size=400, spec|
		^InterpolatorGraph(this, x, y, size, spec);
	}
}

AbstractInterpolatorPoint {
	var <interpolator, <position;

	*new { |interpolator, position|
		^super.newCopyArgs(interpolator).init(position);
	}

	== { |point|
		^(position == point.position)
	}

	position_ { |pos|
		pos.keep(interpolator.n).do({ |i,j|
			this.coordinate_(j, i);
		});	
	}

	duplicate { |point, pointId|
		^interpolator.add(this.position);
	}

}

InterpolatorPoint : AbstractInterpolatorPoint {
	var <weight, attached, <color;
	
	init { |pos|
		// make it depend on all points and the cursor so that its weight and
		// radius get updated.
		interpolator.addDependant(this);
		interpolator.cursor.addDependant(this);
		interpolator.points.do({|i|
			(i === this).not.if{	   // make sur this does not depend on
				i.addDependant(this);  // itself.
			}
		});

		// Initialize position with appropriate size.
		position = FloatArray.newClear(interpolator.n);
		// Initialize its values
		pos.notNil.if({
			pos.keep(interpolator.n).do({ |i,j|
				position.put(j, i);
			});
		});

		{ // Make sure no points are at the same position.
			interpolator.points.collect({ |i|
				i.position;
			}).indexOfEqual(position).notNil
		}.while {
			position.put(0, position[0] + 1);
		};
		attached = false;
		
		(interpolator.points.size == 0).if({
			color = Color.getPresetColor(0);
		},{
			color = Color.getNextPresetColor(interpolator.points.wrapAt(-1).color);
		});

	}

	weight_ { |val|
		weight = val;
		this.changed(\pointWeight, weight);
	}


	coordinate_ { |coordinate, value|
		position.put(coordinate, value);
		interpolator.calculateWeights;
		// this.calculateWeight;
		this.changed(\pointPosition, position);
	}


	update { |model, what ... args|
		what.switch(
			\cursorPosition, {
				attached.if({
					this.position_(interpolator.cursor.position);
				});
			},
			\pointPosition, {
			},
			\pointAdded, {
			},
			\pointRemoved, {
			},
			\pointAttached, {
				// Allow only one attached point.
				// Detach this point when another point is attached.
				this.detach;
			},
			\pointDetached, {
			},
			\pointWeight, {
			}
		);
		
	}

	id {
		^interpolator.points.indexOf(this);
	}

	dist { |point|
		^(position - point.position).squared.sum.sqrt;
	}

	nearestDist { |points|
		^position.nearestDist(points.collect(_.position));
	}

	remove {
		interpolator.points.remove(this);
		interpolator.cursor.removeDependant(this);
		interpolator.points.do({|i|
			i.removeDependant(this);
		});
		interpolator.calculateWeights;
		this.changed(\pointRemoved);
	}

	attach {
		attached = true;
		this.changed(\pointAttached)
	}

	detach {
		attached = false;
		this.changed(\pointDetached)
	}

	// when a point is double clicked in the 2DGui, this is called.  When an
	// Interpolator is used inside a preset interpolator, the preset's gui is
	// opened.
	makeGui { |grabbedPoint|
		this.changed(\makePointGui);
	}

}

InterpolatorCursor : AbstractInterpolatorPoint {
	var featureActions;
	
	init { |pos|
		// Initialize position with correct size.
		position = FloatArray.newClear(interpolator.n);
		pos.keep(interpolator.n).do({ |i,j|
			position.put(j, i);
		});

		interpolator.addDependant(this);
		// make it depend on all points so that its radius gets updated.
		interpolator.points.do({|i|
			i.addDependant(this);
		});

		// radius = position.nearestDist(interpolator.points);
		featureActions = IdentityDictionary[];
	}

	coordinate_ { |coordinate, value|
		position.put(coordinate, value);
		interpolator.calculateWeights;
		this.changed(\cursorPosition, position);
	}

	// To control the position of the cursor using an interface (the sponge).
	// A Feature is connected to one axis of the Cursor.
	// axis is an Integer, feature is a Feature.
	connect { |axis, feature|
		var func;
		feature.addDependant(this);
		// The dictionary key is a Feature!
		featureActions.put(
			feature,
			{ |featureValue| this.coordinate_(axis, featureValue) };
		);
	}

	disconnect { |feature|
		feature.removeDependant(this);
		featureActions.removeAt(feature);
	}

	makeGui { |grabbedPoint|
		this.changed(\makeCursorGui);
	}

	nearestDist { |points|
		^position.nearestDist(points.collect(_.position));
	}

	update { arg model, what ... args;
		// Add this.changed in Feature.
		var action;
		what.switch(
			\featureValue, {
				// args[0] is the value of the Feature.
				featureActions.at(model).value(args[0]);
			},
			\pointAdded, {
			},
			\pointPosition, {
			},
			\pointRemoved, {
			}
		);
	}
}
