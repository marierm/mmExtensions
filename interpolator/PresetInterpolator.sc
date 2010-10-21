// Copyright 2010 Martin Marier

PresetInterpolatorView {
	var colors, <space, scalePoint, <>grabbed, <>grabbedPoint, 
	diff, <view;
	
	*new { |parent, bounds|
		bounds = bounds ? Rect(0,0,400,400);
		parent = parent ? Window(
			"Preset Interpolator", bounds.copy
		).front;
		bounds.postln;
		^super.new.init(parent, bounds);
	}
	
	init { |argParent, argBounds|
		view = UserView(argParent, argBounds);
		grabbed = false;
		space = PresetInterpolator();
		space.gui_(this);
		scalePoint = view.bounds.extent;
		grabbedPoint = 0;

		view.onClose_({
			space.gui_(nil);
		});

		view.drawFunc =  {
			Pen.fillColor_(Color.grey(0.2));
			Pen.addRect(Rect(0,0, view.bounds.width,view.bounds.height));
			Pen.fill;
			space.points.size.do{ |i|
				Pen.color_(space.presets[i].color);
				Pen.fillOval(
					Rect.aboutPoint(space.points[i].pos * scalePoint, 5, 5)
				);
				Pen.stroke;
			};
			space.interPoints.do{ |i,j|
				Pen.color_(space.presets[i].color.copy.alpha_(0.7));
				Pen.fillOval(Rect.aboutPoint(
					space.points[i].pos * scalePoint,
					space.weights[j] * scalePoint.x * 0.2 + 5,
					space.weights[j] * scalePoint.y * 0.2 + 5)
				);
				Pen.stroke;
			};
			Pen.color_(Color.white.alpha_(0.5));
			Pen.fillOval(
				Rect.aboutPoint(space.currentPoint.pos * scalePoint, 8, 8)
			);
			Pen.stroke;
		};
	
		view.mouseDownAction = {
			arg v, x, y, modifiers, buttonNumber, clickCount;
			var scaled;
			var origin; //where the new window will appear
// 			[x,y,modifiers,buttonNumber,clickCount].postln;
			scaled = space.currentPoint.pos * scalePoint;
			space.points.do{|point,i|
				//if mouse is within 6 pixels of a point, grab it.
				point = point.pos * scalePoint;
				if(((x - point.x).abs < 6) && ((y-point.y).abs < 6)){
					grabbed = true;
					grabbedPoint = i;
					//keeps point at the same spot under te mouse when dragging
					diff = Point((x - point.x), (y-point.y));
				}
			};

			if (((x - scaled.x).abs < 6) && ((y - scaled.y).abs < 6)){
				// user clicked on the cursor
				if ((modifiers bitAnd: 134217728 != 0)) { //alt key is pressed
					space.copyCurrentPoint;
					grabbed = true;
					grabbedPoint = space.points.size - 1;
				} { 
					if (modifiers bitAnd: 234881024 == 0) {
						// no modifier keys are pressed
						grabbed = true;
						grabbedPoint = -1;
						diff = Point((x - scaled.x), (y - scaled.y));
					}
				};
			} {
				if ((modifiers bitAnd: 134217728 != 0) && grabbed) {
					// user clicked on a preset (not on the cursor)
					if (modifiers bitAnd: 33554432 != 0) {
						//alt and shift are both pressed
						space.removePoint(grabbedPoint);
					} {
						// holding the alt key only
						space.copyPoint(grabbedPoint);
						grabbedPoint = space.points.size - 1;
					}
				};
			};
		
			if (clickCount == 2) { //doubleclick adds a point
				if (grabbed) {
					// first close all other windows.
					// so that there is only one preset gui at the
					// time.
					// Also, store origin to draw new Window
					//  on the same postion
					if(space.currentPreset.gui.notNil){
						origin = space.currentPreset.gui.w.bounds.origin;
						space.currentPreset.gui.close;
					};
					space.presets.do{|i,j|
						if(i.gui.notNil){
							origin = i.gui.w.bounds.origin;
							i.gui.close;
						}
					};
					if (grabbedPoint != -1) {
							space.presets[grabbedPoint].makeGui(origin:origin);
					}{
							space.currentPreset.makeGui(origin:origin);
					}
				} {
					space.addPoint(Point(x,y) / scalePoint);
				}
			};

		};
	
		view.mouseUpAction =  {
			arg v, x, y, modifiers;
			grabbed = false;
			grabbedPoint=0;
			space.moveAction.value(
				space.points,
				space.currentPoint,
				space.presets,
				space.currentPreset
			);
		};
	
		view.mouseMoveAction = {
			arg v, x, y, modifiers, buttonNumber, clickCount;
			var newVal;  
			var newPos;
		
			if (grabbed) {
				if (grabbedPoint == -1) {
					// user grabbed the cursor
					space.currentPoint.pos_(
						(Point(x,y) - diff).constrain(view.bounds) / scalePoint
					);
				}{
					newPos = (Point(x,y) - diff).constrain(view.bounds) / scalePoint;
					//check if two points have the same position.
					if (space.points.collect(_.pos).indexOfEqual(newPos).notNil){
						newPos.x_(newPos.x + 0.001);
						//ensures weights array does not contain nan.
					};
					space.points[grabbedPoint].pos_(newPos);
				};
				space.refreshRads;
				view.refresh;
				space.moveAction.value(
					space.points,
					space.currentPoint,
					space.presets,
					space.currentPreset
				);
			};
		
		};
	}

	space_ { |sp|
		space = sp;
		sp.gui_(this);
		space.presets.do{ |i|
			if (i.gui.notNil) {
				i.makeGui;
			}
		};
		if (space.currentPreset.gui.notNil) {
			space.currentPreset.makeGui;
		};
		//space.refresh;
	}

	refresh {
		view.refresh;
	}

	close {
		view.getParents.last.close;
		// view.parent.close;
	}
}

PresetInterpolator { //Inspired by Marije Baalman's ParameterSpace
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
					_.areaRatio(currentPoint)
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

Preset {
	var <parameters, <>name, <>siblings, <>gui, <>color;

	saveable {
		^[name, color, parameters.collect(_.saveable)];
	}
	
	*new { arg ...params;
		^super.new.init(params);
	}
	
	*newFromSibling { arg sibling;
		^super.new.initFromSibling(
			sibling.parameters,
			sibling
		);
	}
	
	init { arg params;
		name = "Preset";
		color = Color.new255(167,167,167);
		parameters = List[];
		params.do({ |i, j|
			this.add(i);
		});
		siblings = List[];
	}
	
	initFromSibling { arg params, sblng;
		name = "Preset";
		parameters = List[];
		params.do { |i, j|
			this.add(Parameter.newFromSibling(i));
		};
		siblings = List[];
		siblings.array_(sblng.siblings ++ [sblng]);
		siblings.do{ |i|
			i.siblings.add(this);
		}
	}
	
	add { |thing|
		if (thing.class == Parameter) {
			parameters.add(thing.preset_(this).id_(parameters.size));
			if (gui.notNil) {
				parameters.last.makeGui(gui.w).background_(color);
			};
			siblings.do { |i|
				if (i.parameters.size != parameters.size) {
					i.add(
						Parameter.newFromSibling(thing);
					);
				}
			};
		} {
			"Not a Parameter".warn;
		}
	}
	
	removeAt { |index|
		var paramRef;
		if ( index < parameters.size && parameters.size > 1) {
			paramRef = parameters[index]; // keep ref to param being removed
			parameters.removeAt(index);
			if (gui.notNil) {
				paramRef.gui.w.remove;
				gui.w.decorator.top_((82*(parameters.size-1))+24);
			};
			if (parameters.size != index) {
				// if removed parameter was not the last one.
				// not parameters.size - 1 because parameters.size is
				// is already shrunk (item was removed 8 lines above).
				(index..parameters.size-1).do { |id|
					parameters[id].id_(id); // give the right id (renumber)
					if (gui.notNil) {
						parameters[id].gui.w.remove;
						gui.w.decorator.top_((82*(id-1))+24);
						parameters[id].makeGui(gui.w).background_(color);
					};
				};
			};
			siblings.do { |i|
				if (i.parameters.size != parameters.size) {
					i.removeAt(index);
				}
			};
		} {
			"This Parameter cannot be removed".warn;
		}
	}
	
	size {
		^parameters.size;
	}
	
	makeGui { arg w, origin = Point(400,900);
		gui = PresetGui.new(this, w, origin.x, origin.y, color);
		^gui;
	}
}

PresetGui { 
	var <>w, <>preset, <>x, <>y, <>background, <>addButton;
	
	*new { |preset, w, x, y, color| 
		^super.new.w_(w).preset_(preset).init(x, y, color);
	}
	
	init { arg x=400, y=900, col;
		var v;
		w = w ?? { 
			w = Window(
				preset.name, 
				Rect(x, y, 324, (90 * preset.size) + 17),
				scroll:true
			).alwaysOnTop_(false).front; 
			w.view.decorator = FlowLayout(
				Rect(0, 0, w.bounds.width, w.bounds.height), 2@2, 2@2
			);
			// w.view.hasHorizontalScroller_(false);
		};
		
		background = col ? Color.new255(167, 167, 167);

		addButton = Button(w, 300@18).states_(
			[["+", background]]
		).action_({
			preset.add(Parameter())
		});
				
		preset.parameters.do{|i, j|
			i.makeGui(w);
			i.gui.background_(background);
		};

		w.onClose_({
			preset.gui_(nil);
		});
	}
	
	close {
		w.close;
	}
}

Parameter {
	var <name, <value, <spec, <>action, <siblings, <>gui, <>preset, <id,
	<>nameIsId, <>actionString, textViewAction;
	//value is unmapped (between 0 and 1);
	
	*new { |name, spec, value|
		^super.new.init(name, spec, value);
	}

	saveable {
		^[name,value,spec,actionString];
	}
	
	*newFromSibling { |sibling|
		//Parameters can have siblings that will share spec, name
		//(but not value)
		^super.new.init(
			nam: sibling.name,
			sp: sibling.spec,
			sblng: sibling,
			id: sibling.id
		).nameIsId_(sibling.nameIsId);
	}
	
	init { |nam, sp, val, sblng, id|
		//try {nam = nam.asString};
		name = nam;// ? String();
		if (name.isNil) {nameIsId = true}{nameIsId = false};
		siblings = List[];
		try {sp = sp.asSpec};
		spec = sp ? ControlSpec();
		try {val = val.clip(0,1)};
		value = val ? spec.unmap(spec.default);
		if (sblng.notNil) {
			siblings.array_(sblng.siblings ++ [sblng]);
			siblings.do{ |i|
				i.siblings.add(this);
			}
		};
		actionString = "{|mapped, unmapped|\n\t\n}";
	}
	
	setActionString { |string|
		action = action.removeFunc(textViewAction);
		textViewAction = string.interpret;
		action = action.addFunc(textViewAction);
		actionString = string;
	}
	
	id_ { |i|
		id = i;
		if (nameIsId) {name = id.asString};
	}
	
	sibling_ { |sblng|
		this.init(
			nam: sblng.name,
			sp: sblng.spec,
			val: value,
			sblng: sblng,
			id: sblng.id
		);
	}
	
	spec_ { |sp|
		spec = sp.asSpec.deepCopy;
		siblings.do{ |param|
			if (param.spec != spec) {
				param.spec_(spec);
			}
		};
		this.refresh;
	}
	
	name_ { |na|
		name = na;
		nameIsId = false;
		siblings.do{ |param|
			if (param.name != name) {
				param.name_(name);
				param.nameIsId_(false);
			}
		};
		this.refresh;
	}

	mapped {
		^spec.map(value);
	}
	
	mapped_ { |val|
		this.value_(spec.unmap(val));
	}
	
	value_ { |val|
		value = val;
		action.value(this.mapped, value);
	}
	
	makeGui { arg w, x=30, y=900, color;
		gui = ParameterGui.new(this, w, x, y, color);
		^gui;
	}
	
	refresh {
		if (gui.notNil) {
			gui.refresh;
		}
	}
	
	refreshSiblings {
		siblings.do{|i|
			i.refresh;
		};
	}
}

ParameterGui { 
	
	var <>w, <>param, <>x, <>y, mapped, unmapped, slider, name,
	refreshFunc, <specWindow, textViewAction,
	textViewWindow;
	
	*new { |param, w, x, y, color| 
		^super.new.w_(w).param_(param).init(x, y, color);
	}
	
	init {
		arg x=30, y=900, color;
		var mouseDownFunc;
		w = try { CompositeView(w, Rect(0,0, 300, 80)) }
			?? { 
				w = Window( param.name , Rect(x, y, 300, 85))
					.alwaysOnTop_(true)
					.front; 
				w.view;
			};
		w.resize_(6);
		w.decorator = FlowLayout(
			Rect(0, 0, w.bounds.width, w.bounds.height), 2@2, 2@2
		);
				
		w.background = color ? Color.new255(167, 167, 167);
		
		refreshFunc = { |mapped, value|
			this.refresh;
		};
		textViewAction = {};
		
		param.action_( param.action.addFunc(refreshFunc) );
		param.action_( param.action.addFunc(textViewAction) );
		
		mouseDownFunc = {
			arg view, x, y, modifiers, buttonNumber, clickCount;
			var win, text;
			win = w.parent ? w;
			// if (buttonNumber == 1 && specWindow.isNil){ //cocoa right click
			// Using swing : left=1, mid=2, right=3
			// Using cocoa : left=0, mid=2, right=1 
			if (clickCount == 2 && specWindow.isNil){ // swing right click
				specWindow = param.spec.makeWindow(
					x: win.bounds.right,
					y: win.bounds.bottom - w.bounds.bottom,
					action: { |spec|
						this.refresh;
						this.param.refreshSiblings;
					},
					name: param.name.asString + "Spec"
				).onClose_({
					specWindow = nil;
				});
			};
			if (modifiers bitAnd: 134217728 != 0) { //alt key is pressed
				// Using swing : middleClick (button 2) acts like alt
				// is pressed
				textViewWindow = Window(
					param.name.asString + "action",
					Rect(
						win.bounds.right,
						win.bounds.bottom - w.bounds.bottom,
						400,
						400
					)
				).front.onClose_({
					param.setActionString(text.string);
					// param.action_(
					// 	param.action.removeFunc(textViewAction)
					// ); 
					// textViewAction = text.string.interpret;
					// param.action_(
					// 	param.action.addFunc(textViewAction)
					// );
					// actionString = text.string;
				});
				text = TextView(textViewWindow, Rect(0,0,400,400))
				// 	.usesTabToFocusNextView_(false) // doesn't work in swing?
					.string_(param.actionString);
			};
		};
		
		name = TextField( w, (w.bounds.width - 22)@16 )
			.string_(param.name)
			.align_(\left)
			.background_(Color.clear)
			.resize_(2)
			.action_({ |tf|
				param.name_(tf.value);
			});
		Button( w, 16@16)
			.states_([["X"]])
			.resize_(3)
			.action_({
				if (param.preset.notNil) {
					param.preset.removeAt(param.id);
				} {
					w.findWindow.close;
				}
			});
		slider = Slider(w, (w.bounds.width - 4)@16)
			.value_(param.value)
			.resize_(2)
			.action_({ |sl|
				unmapped.value_(sl.value);
				param.value_(unmapped.value);
				mapped.value_(param.mapped);
			})
			.mouseDownAction_(mouseDownFunc);
		StaticText(w, 70@16)
			.string_("unmapped")
			.resize_(2)
			.align_(\right);
		unmapped = NumberBox(w, (w.decorator.indentedRemaining.width)@16)
			.value_(param.value)
			.resize_(3)
			.action_({ |nb|
				nb.value_(param.spec.unmap(param.spec.map(nb.value)));
				slider.value_(nb.value);
				param.value_(unmapped.value);
				mapped.value_(param.mapped);
			});
		StaticText(w, 70@16)
			.string_("mapped")
			.resize_(2)
			.align_(\right);
		mapped = NumberBox(w, (w.decorator.indentedRemaining.width)@16)
			.value_(param.spec.map(param.value))
			.resize_(3)
			.action_({ |nb|
				nb.value_(param.spec.constrain(nb.value));
				unmapped.value_(param.spec.unmap(nb.value));
				param.value_(unmapped.value);
				slider.value_(param.value);
			});

		w.onClose_({
			param.action_(param.action.removeFunc(refreshFunc));
			try {
				specWindow.close;
				textViewWindow.close;
			};
			param.gui_(nil);
		});
	}
	
	refresh {
		slider.value_(param.value);
		unmapped.value_(param.value);
		mapped.value_(param.mapped);
		name.string_(param.name);
	}
	
	background_ { |color|
		w.background_(color);
	}
}

