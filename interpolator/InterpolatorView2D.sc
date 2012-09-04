InterpolatorView2D {

	var model, xAxis, yAxis, size, uv, grabbedPoint, diff, bounds, pointsSpec,
	xSpec, ySpec, window;

	*new { |model, x=0, y=1, size=400, spec|
		^super.newCopyArgs(model, x, y, size).init(spec);
	}

	init { |spec|
		// xAxis and yAxis are the dimensions of the Interpolator to be graphed.
		// size is size in pixels (square).
		// spec is the size of the virtual area.
		var flop, axies;

		model.addDependant(this);
		model.allPoints.do(_.addDependant(this));
		axies = (1..model.n).clump(6).collect({ |i,j|
			i.collect({|k,l|
				([$x, $y, $z, $w, $v, $u].at(l) ! (j+1)).as(String);
			}).sort;
		}).flatten.at([xAxis, yAxis]);

		window = Window(
			"Interpolator 2D (axies % and %)".format(*axies),
			Rect.aboutPoint(Window.screenBounds.center, size + 8, size +8)
		);

		bounds = Rect(4,4,size,size);
		this.calculateSpecs(spec);
		uv = UserView( window, bounds );
		uv.drawFunc = {
			Pen.fillColor_(Color.grey(0.2));
			Pen.addRect(Rect(0,0, uv.bounds.width,uv.bounds.height));
			Pen.fill;
			model.points.do{ |point, i|
				Pen.color_( point.color );
				Pen.fillOval(
					Rect.aboutPoint(this.scale(this.getPoint(point)), 5, 5)
				);
				Pen.stroke;
				Pen.color_(point.color.copy.alpha_(0.7));
				Pen.fillOval(Rect.aboutPoint(
					this.scale(this.getPoint(point)),
					point.weight * bounds.extent.x * 0.2 + 5,
					point.weight * bounds.extent.y * 0.2 + 5
				));
				Pen.stroke;
			};
			Pen.color_(Color.white.alpha_(0.5));
			Pen.fillOval(
				Rect.aboutPoint(this.scale(this.getPoint(model.cursor)), 8, 8)
			);
			Pen.stroke;
		};
		
		uv.mouseDownAction = { |v, x, y, modifiers, buttonNumber, clickCount|
			var mousePos;
			mousePos = Point(x,y);
			
			model.allPoints.do{|point,i|
				var pixelDist, scaledPoint;
				scaledPoint = this.scale(this.getPoint(point));
				pixelDist = scaledPoint.dist(mousePos);
				( pixelDist < 6 ).if({
					grabbedPoint = point;
					diff = mousePos - scaledPoint;
				});
			};

			grabbedPoint.notNil.if({
				(modifiers.isAlt && modifiers.isShift).if ({
					//alt and shift are both pressed
					grabbedPoint.remove;
					grabbedPoint = nil;
				},{
					modifiers.isAlt.if {
						grabbedPoint = grabbedPoint.duplicate;
					};
				})
			});

			(clickCount == 2).if { //doubleclick adds a point
				grabbedPoint.notNil.if {
					// if double click on a point, open Preset Gui.
					grabbedPoint.makeGui;
				} {
					var coordinates;
					coordinates = FloatArray.newClear(model.n);
					coordinates.putEach(
						[xAxis, yAxis], this.unscale(mousePos).asArray
					);
					model.add( coordinates );
				}
			}
		};
			
		uv.mouseUpAction =  { |v, x, y, modifiers|
			grabbedPoint = nil;
			// model.moveAction.value();
		};
		
		uv.mouseMoveAction = {
			arg v, x, y, modifiers, buttonNumber, clickCount;
			grabbedPoint.notNil.if {
				grabbedPoint.coordinate_(
					xAxis, pointsSpec.map(xSpec.unmap(x - diff.x))
				);
				grabbedPoint.coordinate_(
					yAxis, pointsSpec.map(ySpec.unmap(y - diff.y))
				);
			};
		};

		window.front;
	}

	update { |model, what ... args|
		what.switch(
			\pointWeight, {
				uv.refresh;
			},
			\pointAdded, {
				// args[0] is the new point
				(
					(pointsSpec.minval > args[0].position.minItem) or: 
					(pointsSpec.maxval < args[0].position.maxItem)
				).if {
					this.calculateSpecs();
				};
				args[0].addDependant(this);
				uv.refresh;
			},
			\pointRemoved, {
				uv.refresh;
			},
			\pointPosition, {
				// args[0] is the position
				(
					(pointsSpec.minval > args[0].minItem) or:
					(pointsSpec.maxval < args[0].maxItem)
				).if {
					this.calculateSpecs();
				};
				uv.refresh;
			},
			\cursorPosition -> {
			}
		)
	}
	
	calculateLayoutSize {
		^Rect(0,0,408,408)
	}

	scale { |point|
		point = pointsSpec.unmap(point);
		^Point(xSpec.map(point.x), ySpec.map(point.y))
	}

	unscale { |point|
		point.x = xSpec.unmap(point.x);
		point.y = ySpec.unmap(point.y);
		^pointsSpec.map(point)
	}
	getPoint { |point|  //which dimensions are we drawing?
		// 2DGui can be used to visualize 2 axies of an N dimension
		// Interpolator.  This creates returns the right point.
		^point.position.at([xAxis, yAxis]).asPoint;
	}

	// duplicatePoint { |point, padding = 5|
	// 	point.duplicate;
	// 	var point, tmp;
	// 	(i != \cursor).if({
	// 		// Because there cannot be two points with the same coordinates,
	// 		// we need to offset the point a bit.  Padding is in pixels.
	// 		point = model.points[i].copy;
	// 		tmp = this.unscale(this.scale(Point(point[x],point[y])) + padding);
	// 	},{
	// 		// If cursor is duplicated, position is not padded. The new point
	// 		// appears exactly at cursor position.
	// 		point = model.cursor.copy;
	// 		tmp = this.unscale(this.scale(Point(point[x],point[y])));
	// 	});
	// 	point[x] = tmp.x;
	// 	point[y] = tmp.y;
	// 	model.duplicatePoint(point, i);
	// }

	// addPoint { |i|
	// 	var point;
	// 	point = 0!model.n;
	// 	point[x] = i.x;
	// 	point[y] = i.y;
	// 	model.add(point);
	// }

	// movePoint{ |i, newPos|
	// 	newPos = this.unscale(newPos);
	// 	model.changeCoord(i, x, newPos.x);
	// 	model.changeCoord(i, y, newPos.y);
	// }

	// moveCursor { |newPos|
	// 	var point;
	// 	point = model.cursor.copy;
	// 	newPos = this.unscale(newPos);
	// 	point[x] = newPos.x;
	// 	point[y] = newPos.y;
	// 	model.cursor_(point);
	// }

	calculateSpecs { |spec|
		var min, max, array;
		spec.notNil.if{
			spec = spec.asSpec;
		};
		array = (model.points.collect(_.position) ++ model.cursor.position).flat;
		min = array.minItem;
		max = array.maxItem;
		pointsSpec = spec ? ControlSpec(min, max);
		xSpec = ControlSpec(6, bounds.width - 6);
		ySpec = ControlSpec(6, bounds.height - 6);
		// xSpec = ControlSpec(bounds.width * padding * 0.5, bounds.width * (1 - (padding * 0.5)));
		// ySpec = ControlSpec(bounds.height * padding * 0.5, bounds.height * (1 - (padding * 0.5)));
	}

}
