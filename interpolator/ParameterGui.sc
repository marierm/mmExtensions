ParameterGui : AbstractInterpolatorGui { 
	var <name, <slider, <mapped, <unmapped, mouseDownFunc, specWindow;

	calculateLayoutSize {
		^Rect(0,0,300,80)
	}

	init {
		actions = IdentityDictionary[
			\spec -> {|model, what, spec|
				slider.value_(model.value);
				mapped.value_(model.mapped);
				unmapped.value_(model.value);
			},
			\value -> {|model, what, mppd, val|
				slider.value_(val);
				mapped.value_(mppd);
				unmapped.value_(val);
			},
			\name -> {|model, what, nm|
				name.value_(nm);
			}
		];
		mouseDownFunc = {
			arg view, x, y, modifiers, buttonNumber, clickCount;
			var win, text;
			// win = w.parent ? w;
			// // if (buttonNumber == 1 && specWindow.isNil){ //cocoa right click
			// // Using swing : left=1, mid=2, right=3
			// // Using cocoa : left=0, mid=2, right=1 
			if (clickCount == 2){ // swing right click
				model.spec.makeWindow(
					x: this.bounds.right,
					y: this.bounds.bottom - this.bounds.bottom,
					action: { |spec|
						model.spec_(spec);
					},
					name: model.name.asString + "Spec"
				)
			};
			// if (modifiers bitAnd: 134217728 != 0) { //alt key is pressed
			// 	// Using swing : middleClick (button 2) acts like alt
			// 	// is pressed
			// 	textViewWindow = Window(
			// 		param.name.asString + "action",
			// 		Rect(
			// 			win.bounds.right,
			// 			win.bounds.bottom - w.bounds.bottom,
			// 			400,
			// 			400
			// 		)
			// 	).front.onClose_({
			// 		param.setActionString(text.string);
			// 		// param.action_(
			// 		// 	param.action.removeFunc(textViewAction)
			// 		// ); 
			// 		// textViewAction = text.string.interpret;
			// 		// param.action_(
			// 		// 	param.action.addFunc(textViewAction)
			// 		// );
			// 		// actionString = text.string;
			// 	});
			// 	text = TextView(textViewWindow, Rect(0,0,400,400))
			// 	// 	.usesTabToFocusNextView_(false) // doesn't work in swing?
			// 	.string_(param.actionString);
			// };
		};
	}
	
	guiBody {|lay|
		layout = lay;
		name = TextField( layout, (layout.bounds.width - 24)@16 )
		.string_(model.name)
		.align_(\left)
		.background_(Color.clear)
		.resize_(2)
		.action_({ |tf|
			model.name_(tf.value);
		});
		Button( layout, 16@16)
		.states_([["X"]])
		.resize_(3)
		.action_({
			model.remove;
		});
		slider = Slider(layout, (layout.bounds.width - 4)@16)
		.value_(model.value)
		.resize_(2)
		.action_({ |sl|
			model.value_(sl.value);
		})
		.mouseDownAction_(mouseDownFunc);
		StaticText(layout, 70@16)
		.string_("unmapped")
		.resize_(2)
		.align_(\right);
		unmapped = NumberBox(layout, (layout.decorator.indentedRemaining.width)@16)
		.value_(model.value)
		.resize_(3)
		.action_({ |nb|
			model.value_(nb.value);
		});
		StaticText(layout, 70@16)
		.string_("mapped")
		.resize_(2)
		.align_(\right);
		mapped = NumberBox(layout, (layout.decorator.indentedRemaining.width)@16)
		.value_(model.mapped)
		.resize_(3)
		.action_({ |nb|
			model.mapped_(nb.value);
		});
	}
}
