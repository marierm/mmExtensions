AbstractInterpolatorGui : ObjectGui {
	var <layout, <>actions;

	*new { arg model;
		var new;
		new = super.new;
		new.model_(model);
		^new.init;
	}

	init {
		actions = IdentityDictionary[];
	}

	gui { arg parent, bounds ... args;
		var layout;
		bounds = bounds ?? this.calculateLayoutSize;
		layout=this.guify(parent,bounds);
		layout.flow({ arg layout;
			this.view = layout;
			this.writeName(layout);
			this.performList(\guiBody,[layout] ++ args);
		},bounds).background_(this.background);
		//if you created it, front it
		if(parent.isNil,{
			layout.resizeToFit(true,true);
			layout.front;
		});
		^layout;
	}
	
	calculateLayoutSize {
		^Rect(0,0,400,400)
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

InterpolatorGui : AbstractInterpolatorGui {

	var <>guiItems, butHeight;

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

	guiBody {|lay|
		layout = lay;
		this.drawHeader;
		// new line
		// layout.decorator.nextLine;
		//eventually replace by layout.startRow; (???)
		model.points.do{|point, i|
			this.addPresetLine(point, i);
			// layout.decorator.nextLine;
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


Interpolator2DGui : AbstractInterpolatorGui {
	var uv, x, y, grabbed, grabbedPoint, diff, bounds;

	init {
		actions = IdentityDictionary[
			\weights -> {|model, what, interPoints, weights|
				uv.refresh;
			},
			\pointAdded -> {|model, what, point|
				uv.refresh;
			},
			\pointRemoved -> {|model, what, i|
				uv.refresh;
			},
			\pointMoved -> {|model, what, i, point|
				uv.refresh;
			}
		];
	}
	
	calculateLayoutSize {
		^Rect(0,0,400,400)
	}

	scale { |point, padding=0.1|
		var flop, range, min, max;
		flop = (model.points ++ [model.cursor]).flop;
		// flop = (model.points).flop;
		// min = Point(flop[x].minItem, flop[y].minItem);
		// max = Point(flop[x].maxItem, flop[y].maxItem);
		min = flop.flatten.minItem;
		max = flop.flatten.maxItem;
		range = max - min;
		^((point - min)/range * bounds.extent * (1-padding)) + (bounds.extent * padding * 0.5)
	}

	unscale { |point, padding=0.1|
		var flop, range, min, max;
		flop = (model.points ++ [model.cursor]).flop;
		// flop = (model.points).flop;
		// min = Point(flop[x].minItem, flop[y].minItem);
		// max = Point(flop[x].maxItem, flop[y].maxItem);
		min = flop.flatten.minItem;
		max = flop.flatten.maxItem;
		range = max - min;
		(range == 0).if {range = 1.0};
		^((point - (bounds.extent * padding * 0.5)) / (bounds.extent * (1 - padding)) * range) + min
	}

	getPoint { |i|  //which dimensions are we drawing?
		// 2DGui can be used to visualize 2 axies of an N dimension
		// Interpolator.  This creates returns the right point.
		(i != \cursor).if{
			^Point(model.points[i][x],model.points[i][y])
		} {
			^Point(model.cursor[x],model.cursor[y])
		}
	}

	duplicatePoint { |i, padding = 0.1|
		var point;
		(i != \cursor).if({
			point = model.points[i].copy;
		},{
			point = model.cursor.copy;
		});
		point[x] = point[x] + padding;
		point[y] = point[y] + padding;
		model.add(point);
	}

	addPoint { |i|
		var point;
		point = 0!model.n;
		point[x] = i.x;
		point[y] = i.y;
		model.add(point);
	}


	getColor { |i|
		^Color.red;
	}

	guiBody { |lay, bnds, xAxis = 0, yAxis = 1|
		x = xAxis;
		y = yAxis;
		layout = lay;
		// bnds = layout.innerBounds.insetAll(0,0,20,0);
		bounds = layout.bounds;
		// bounds = bnds;
		// scalePoint = this.calculateScalePoint();

		uv = UserView( layout, bounds );//.resize_(5);

		uv.drawFunc =  {
			Pen.fillColor_(Color.grey(0.2));
			Pen.addRect(Rect(0,0, uv.bounds.width,uv.bounds.height));
			Pen.fill;
			model.points.size.do{ |i|
				Pen.color_(this.getColor(i));
				Pen.fillOval(
					Rect.aboutPoint(this.scale(this.getPoint(i)), 5, 5)
				);
				Pen.stroke;
			};
			model.interPoints.do{ |i,j|
				Pen.color_(this.getColor(i).copy.alpha_(0.7));
				Pen.fillOval(Rect.aboutPoint(
					this.scale(this.getPoint(i)),
					model.weights[j] * bounds.extent.x * 0.2 + 5,
					model.weights[j] * bounds.extent.y * 0.2 + 5)
				);
				Pen.stroke;
			};
			Pen.color_(Color.white.alpha_(0.5));
			Pen.fillOval(
				Rect.aboutPoint(this.scale(this.getPoint(\cursor)), 8, 8)
			);
			Pen.stroke;
		};
	
		uv.mouseDownAction = {
			arg v, x, y, modifiers, buttonNumber, clickCount;
			var scaled;
			var origin; //where the new window will appear
// 			[x,y,mondifiers,buttonNumber,clickCount].postln;
			scaled = this.scale(this.getPoint(\cursor));
			model.points.do{|point,i|
				//if mouse is within 6 pixels of a point, grab it.
				point = this.scale(this.getPoint(i));
				if(((x - point.x).abs < 6) && ((y-point.y).abs < 6)){
					grabbed = true;
					grabbedPoint = i;
					//keeps point at the same spot under te mouse when dragging
					diff = Point((x - point.x), (y-point.y));
				}
			};

			if (((x - scaled.x).abs < 6) && ((y - scaled.y).abs < 6)){
				// user clicked on the cursor
				if ((modifiers bitAnd: 134217728 != 0)) { //alt key is
														  //pressed:
														  //copyCurrentPoint;
					this.duplicatePoint(\cursor);
					grabbed = true;
					grabbedPoint = model.points.size - 1; //grab the point
														  //that was just
														  //created.
				} { 
					if (modifiers bitAnd: 234881024 == 0) {
						// no modifier keys are pressed
						grabbed = true;
						grabbedPoint = -1; //-1 is cursor
						diff = Point((x - scaled.x), (y - scaled.y));
					}
				};
			} {
				if ((modifiers bitAnd: 134217728 != 0) && grabbed) {
					// user clicked on a preset (not on the cursor)
					if (modifiers bitAnd: 33554432 != 0) {
						//alt and shift are both pressed
						model.remove(grabbedPoint);
					} {
						// holding the alt key only
						this.duplicatePoint(grabbedPoint);
						grabbedPoint = model.points.size - 1;
					}
				};
			};
		
			if (clickCount == 2) { //doubleclick adds a point
				if (grabbed) {
					// // first close all other windows.
					// // so that there is only one preset gui at the
					// // time.
					// // Also, store origin to draw new Window
					// //  on the same postion
					// if(model.currentPreset.gui.notNil){
					// 	origin = model.currentPreset.gui.w.bounds.origin;
					// 	model.currentPreset.gui.close;
					// };
					// model.presets.do{|i,j|
					// 	if(i.gui.notNil){
					// 		origin = i.gui.w.bounds.origin;
					// 		i.gui.close;
					// 	}
					// };
					// if (grabbedPoint != -1) {
					// 		model.presets[grabbedPoint].makeGui(origin:origin);
					// }{
					// 		model.currentPreset.makeGui(origin:origin);
					// }
				} {
					this.addPoint(this.unscale(Point(x,y)));
				}
			};

		};
	
		uv.mouseUpAction =  {
			arg v, x, y, modifiers;
			grabbed = false;
			grabbedPoint=0;
			// model.moveAction.value();
		};
	
		uv.mouseMoveAction = {
			arg v, x, y, modifiers, buttonNumber, clickCount;
			var newVal;  
			var newPos;
		
			grabbed.if {
				(grabbedPoint == -1).if {
					// user grabbed the cursor
					model.cursor_(
						this.unscale(
							((Point(x,y) - diff).constrain(uv.bounds))
						).asArray;
					);
				}{
					newPos = this.unscale(
						(Point(x,y) - diff).constrain(uv.bounds)
					);

					// //check if two points have the same position.
					// (model.points.indexOfEqual(newPos).notNil).if{
					// 	newPos.x_(newPos.x + 0.001);
					// 	//ensures weights array does not contain nan.
					// };
					model.movePoint(grabbedPoint, newPos.asArray);
				};
				// model.refreshRads;
				
				// model.moveAction.value(
				// 	model.points,
				// 	model.currentPoint,
				// 	model.presets,
				// 	model.currentPreset
				// );
			};
		
		};
	}
}