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
			if (clickCount == 2){ // double click
				model.spec.makeWindow(
					x: this.bounds.right,
					y: this.bounds.bottom - this.bounds.bottom,
					action: { |spec|
						model.spec_(spec);
					},
					name: model.name.asString + "Spec"
				)
			};
		};
	}
	
	guiBody {|lay|
		layout = lay;
		name = TextField( layout, (layout.bounds.width - 70)@16 )
		.string_(model.name)
		.align_(\left)
		.background_(Color.clear)
		.resize_(2)
		.action_({ |tf|
			model.name_(tf.value);
		});
		Button( layout, 16@16)
		.states_([["O"]])
		.resize_(3)
		.action_({
			OscConfigurationGui.new(model).performList(\gui);
		});
		Button( layout, 16@16)
		.states_([["M"]])
		.resize_(3)
		.action_({
			MIDIClient.initialized.not.if{
				MIDIClient.init;
			};
			MidiConfigurationGui.new(model).performList(\gui);
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
