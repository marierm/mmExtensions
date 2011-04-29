// Copyright 2011 Martin Marier

Interpolator { //More than 2 dimensions

	var <points, <rads, <cursor, <cursorRad, <>moveAction,
	<weights, interPoints, <n;
	
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
		cursor = 0!n;
		this.addFirstPoint;
	}
	
	addFirstPoint {
		points.add( 1!n );
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

}

InterpolatorGui : ObjectGui {

	var <>grabAction, <>ungrabAction, <>guiItems, butHeight,
	<layout, grabbed, <>actions;

	*new { arg model;
		var new;
		new = super.new;
		new.model_(model);
		^new.init;
	}

	init {
		butHeight = 18;
		guiItems = List[];
		actions = IdentityDictionary[
			\weights -> {|model, what, interPoints, weights|
				guiItems.do{ |guiItem, i|
					interPoints.includes(i).if{
						guiItem[3].string_(
							weights[interPoints.indexOf(i)].asString
						);
					} {
						guiItem[3].string_("0");
					}
				}
			},
			\pointAdded -> {|model, what, point|
				this.addPresetLine(point, model.points.size - 1);
				layout.resizeToFit(true,true);
			},
			\pointRemoved -> {|model, what, i|
				layout.postln;
				// Remove all lines starting from the line to be removed.
				(i..guiItems.size - 1).do { |j|
					guiItems[j].flatten.do{ |guiItem|
						guiItem.remove;
					}
				};
				// Remove the objects in guiItems
				guiItems = guiItems.keep(i);
				// Reset layout postion so that new lines appear at the right
				// place.
				layout.decorator.top_(
					(butHeight+4)*(guiItems.size)
				);
				// Redraw lines
				if (model.points.size != i) {
					(i..model.points.size-1).do { |j,k|
						this.addPresetLine(model.points[j],j);
					};
				};
			},
			\pointMoved -> {|model, what, i, point|
				guiItems[i][1].do{ |numBox, j|
					numBox.value_(point[j]);
				};
			}
		];
	}
	
	calculateLayoutSize {
		var width, height;
		width = ((54*(model.n + 1)) + ((butHeight+4)*2));
		height = ((butHeight+4)*(model.points.size + 1)).max(100);
		^Rect(0,0,width,height);
	}

	gui { arg parent, bounds;
		^super.gui(parent, bounds ?? {this.calculateLayoutSize})
	}

	guiBody { |lay|
		layout = lay;
		this.drawHeader;
		// new line
		layout.decorator.nextLine;
		//eventually replace by layout.startRow; (???)
		model.points.do{|point, i|
			this.addPresetLine(point, i);
			layout.decorator.nextLine;
		}
	}

	drawHeader {
		// add button
		Button(layout, (this.calculateLayoutSize.width - 2)@butHeight)
		.states_([["Add Point"]])
		.action_({|b|
			model.add();
		});
	}

	addPresetLine { |point, i|
		//guiItems contains arrays of guiItems:
		//[[pointNumber, [coords], removeBut, weight],...]
		// Resize the window
		guiItems.add([ 
			StaticText(layout, butHeight@butHeight)
			.string_(i.asString),

			// coordinates
			Array.fill(point.size, {|j|
				NumberBox(layout, 50@butHeight)
				.value_(point[j])
				// .decimals_(3)
				.action_({|nb|
					model.changeCoord(i, j, nb.value);
				});
			}),
			// remove button
			Button( layout, (butHeight@butHeight))
			.states_([["X"]])
			.action_({
				model.remove(i);
			}),
			StaticText(layout, 50@butHeight)
			.string_("0")
			// .align_(\right)
		]);
	}

	update { arg model, what ... args;
		var action;
		action = actions.at(what);
		if (action.notNil, {
			action.valueArray(model, what, args);
		});
	}
	// Eventually remove (when QT has drag and drop)
	writeName {}
}
