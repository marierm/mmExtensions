// Copyright 2011-2012 Martin Marier

Interpolator { //More than 2 dimensions

	var <points, <rads, <cursor, <cursorRad, <>moveAction,
	<weights, <interPoints, <n, <>colors, <>action, <attachedPoint,
	connections;
	
	*new{ |numDim = 2|
		^super.new.init(numDim);
	}
	
	init{ |numDim|
		moveAction = {
			// action should not update gui
			action.value(interPoints, weights);
			this.refreshRads;
			//find points that intersect with cursor
			//calculate weights
			if(points.indexOfEqual(cursor).notNil) {
				// if cursor is exactly on a preset
				interPoints = [ points.indexOfEqual(cursor) ];
				weights = [1];
			}{
				interPoints = points.selectIndices({ |i,j|
					i.dist(cursor) < (cursorRad + rads[j])
				});
				weights = points[interPoints].collect({ |i,j|
					i.intersectArea(
						rads[interPoints[j]], cursor, cursorRad
					) / (pi * rads[interPoints[j]].squared);
				}).normalizeSum;
			};
			{
				this.changed(\weights, interPoints, weights);
			}.defer; // gui stuff is defered;
		};
		n = numDim;
		connections = Array.newClear(n);
		colors = List[];
		points = List.new;
		rads = List.new;
		cursor = 0.5!n;
		this.addFirstPoint;
	}
	
	addFirstPoint {
		colors.add(Color.getPresetColor(0));
		points.add( 0!n );
		rads.add(0);
		moveAction.value();
	}
	
	add { |point|
		// add something if no point is given.
		point.isNil.if{
			point = 1!n;
			{points.indexOfEqual(point).notNil}.while{
				point = point + 1;
			}
		};
		//check if point has correct num of coordinates.
		(point.size == n).if {
			//check if point has pos identical to another point.
			//points cannot share the same position.
			points.indexOfEqual(point).isNil.if{
				colors = colors.add(Color.getNextPresetColor(colors.last));
				points.add( point );
				rads.add( 0 );
				this.changed(\pointAdded, point);
				moveAction.value();
			} {
				"There is already a point at %".format(point).warn;
			}
		} {
			"Point must be an array of size %".format(n).postln;
		}
	}

	duplicatePoint { |point, pointId|
		(pointId == \cursor).if {
			this.changed(\cursorDuplicated, point, pointId);
		} {
			this.changed(\pointDuplicated, point, pointId);
		};
	}

	remove { |i|
		if (points.size > 1) {
			colors.removeAt(i);
			points.removeAt(i);
			rads.removeAt(i);
			// this.refreshRads;
			this.changed(\pointRemoved, i);
			moveAction.value();
		} {
			"This Point cannot be removed".warn;
		}
	}

	cursor_ { |pos|
		if (pos.size == n) {
			cursor = pos;
			attachedPoint.notNil.if {
				{ this.movePoint(attachedPoint, pos); }.defer;
			};
			// this.refreshRads;
			moveAction.value();
		} {
			"Position must be an array of size %".format(n).postln;
		}
	}
	
	// change only one coordinate
	changeCoord { |i, j, val|
		var newPos;
		newPos = points[i];
		newPos[j] = val;
		this.movePoint(i, newPos);
	}

	movePoint { |i, pos|
		var otherPoints;
		(pos.size == n).if {
			otherPoints = points.copy;
			otherPoints.removeAt(i);
			otherPoints.indexOfEqual(pos).isNil.if {
				points[i] = pos;
				this.changed(\pointMoved, i, points[i]);
				moveAction.value();
			} {
				"There cannot be two points at the same position".warn;
			}
		} {
			"Position must be an array of size %".format(n).warn;
		}
	}
	// when a point is double clicked in the 2DGui, this is called.  When an
	// Interpolator is used inside a preset interpolator, the preset's gui is
	// opened.
	makePointGui { |grabbedPoint|
		(grabbedPoint == -1).if {
			this.changed(\makeCursorGui);
		} {
			this.changed(\makePointGui, grabbedPoint, colors[grabbedPoint]);
		}
	}

	refreshRads{
		cursorRad = (cursor.nearestDist(points));
		points.do { |i,j|
			var a;
			a = points.deepCopy;
			a.removeAt(j);
			a = a ++ [cursor];
			rads[j] = i.nearestDist(a);
		};
		// this.changed(\weights, weights);
	}

	// To control the position of the cursor using an interface (the sponge).
	// A Feature is connected to one axis of the interpolator.
	connect { |axis, feature|
		var pos, func;
		func = { |value|
			pos = cursor;
			pos[axis] = value;
			this.cursor_(pos);
		};
		connections[axis] = (feature:feature, func:func).know_(false);
		feature.action = action.addFunc(func);
	}

	attachedPoint_ { |point|
		attachedPoint = point;
		this.changed(\attachedPoint, point);
	}

	// gui stuff
	disconnect { |axis|
		connections[axis][\feature].action_(
			connections[axis][\feature].action.removeFunc(
				connections[axis][\func]
			);
		)
	}

	guiClass { ^InterpolatorGui }

	gui2D { arg  ... args;
		^Interpolator2DGui.new(this).performList(
			\gui,args
		);
	}

}
