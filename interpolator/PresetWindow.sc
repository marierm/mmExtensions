PresetWindow { 
	var model, window, tree, buttons, window;

	*new { |model|
		^super.newCopyArgs(model).init;
	}

	init {
		model.addDependant(this);

		window = Window(
			model.name.asString,
			Rect.aboutPoint(Window.screenBounds.center, 350, 100)
		);

		tree = TreeView(window, window.bounds.origin_(0@0)).columns_(
			// ["Name", "", "Mapped", "Unmapped", "OSC","MIDI",""]
			["Name", "", "Mapped", "Unmapped", "OSC",""]
		).resize_(5);

		model.parameters.do{|i,j|
			ParameterView(tree,i,j);
		};

		buttons = tree.addItem(["",""]).setView(
			0,
			Button().states_([["Add"]]).action_({
				model.add(Parameter())
			})
		).setView(
			1,
			Button().states_([["Randomize"]]).action_({
				model.randomizeParameters;
			})
		);
		
		window.onClose_({
			model.removeDependant(this);
		});

		window.front;
	}
	
	update { |model, what ... args|
		what.switch(
			\paramAdded, {
				//args[0] is a new Parameter.
				ParameterView(tree,args[0],model.parameters.size - 1);
			},
			\paramRemoved, {
				//args[0] is a Parameter.
				//args[1] is the id.
				// tree.removeItem(args[0].view.treeItem);
				tree.removeItem(tree.itemAt(args[1]));
			},
			\presetName, {
				tree.setProperty(\windowTitle,model.name);
			}
		);
	}

	background_ { |color|
		tree.background_(color);
	}

}
