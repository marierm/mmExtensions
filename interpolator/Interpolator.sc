// Copyright 2011 Martin Marier

Interpolator { //More than 2 dimensions

	var <points, <rads, <cursor, <cursorRad, <>moveAction,
	<weights, <interPoints, <n;
	
	*new{ |numDim = 2|
		^super.new.init(numDim);
	}
	
	init{ |numDim|
		moveAction = {
			this.refreshRads;
			//find points that intersect with cursor
			//calculate weights
			if(points.indexOfEqual(cursor).notNil) {
				// if cursor is exactly on a preset
				interPoints = [ points.indexOfEqual(cursor) ];
				weights = [1];
			}{
				interPoints = points.selectIndex({ |i,j|
					i.dist(cursor) < (cursorRad + rads[j])
				});
				weights = points[interPoints].collect({ |i,j|
					i.intersectArea(rads[interPoints[j]], cursor, cursorRad)
					/ (pi * rads[interPoints[j]].squared);
				}).normalizeSum;
			};
			this.changed(\weights, interPoints, weights);
		};
		n = numDim;
		points = List.new;
		rads = List.new;
		cursor = 0.5!n;
		this.addFirstPoint;
	}
	
	addFirstPoint {
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
				points.add( point );
				rads.add( 0 );
				this.changed(\pointAdded, point);
				moveAction.value();
			} {
				"There is already a point at %".format(point).postln;
			}
		} {
			"Point must be an array of size %".format(n).postln;
		}
	}

	duplicatePoint { |point, pointId|
		points.add( point );
		rads.add( 0 );
		(pointId == \cursor).if {
			this.changed(\cursorDuplicated);
		} {
			this.changed(\pointDuplicated, pointId);
		};
		moveAction.value();
	}

	remove { |i|
		if (points.size > 1) {
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
			// this.refreshRads;
			moveAction.value();
		} {
			"Position must be an array of size %".format(n).postln;
		}
	}
	
	// change only one coordinate
	changeCoord { |i, j, val|
		points[i][j] = val;
		this.changed(\pointMoved, i, points[i]);
		moveAction.value();
	}

	movePoint { |i, pos|
		(pos.size == n).if {
			points.indexOfEqual(pos).isNil.if {
				points[i] = pos;
				this.changed(\pointMoved, i, points[i]);
				moveAction.value();
			}
		} {
			"Position must be an array of size %".format(n).postln;
		}
	}
	
	makePointGui { |grabbedPoint|
		(grabbedPoint == -1).if {
			this.changed(\makeCursorGui);
		} {
			this.changed(\makePointGui, grabbedPoint);
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

	guiClass { ^InterpolatorGui }

	gui2D { arg  ... args;
		^Interpolator2DGui.new(this).performList(
			\gui,args
		);
	}

}

