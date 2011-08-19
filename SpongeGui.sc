SpongeGui : ObjectGui {
	var <>actions, listInactive, listActive, viewActive, viewInactive;

	*new { arg model;
		var new;
		new = super.new;
		new.model_(model);
		^new.init;
	}

	init {
		this.prUpdateLists;
		actions = IdentityDictionary[
			\featureActivated -> {|model, what, featureDef|
				this.prUpdateLists;
				viewActive.items_(listActive);
				viewInactive.items_(listInactive);
			},
			\featureDeactivated -> {|model, what, featureDef|
				this.prUpdateLists;
				viewActive.items_(listActive);
				viewInactive.items_(listInactive);
			}

		];
	}

	prUpdateLists {
		listActive = model.features.collect({ |i| i.name });
		listInactive = Sponge.featureList.collect(
			{ |i| i[\name] } ).removeAll(listActive);
	}

	update { arg model, what ... args;
		var action;
		action = actions.at(what);
		if (action.notNil, {
			action.valueArray(model, what, args);
		});
	}

	gui { arg ... args;
		var w;
		w = Window("A Sponge GUI");
		// w = ScrollView(x, Rect(0,0,x.bounds.width, x.bounds.height));
		w.layout_(QGridLayout());
		w.layout.add(
			viewInactive = ListView(w).items_(listInactive).mouseDownAction_({
				|view, x, y, modifiers, buttonNumber, clickCount|
				(buttonNumber == 0 and: clickCount == 2).if {
					model.activateFeature(view.items[view.value]);
				};
			}),
			0,	// row
			0	// column
		);
		w.layout.add(
			viewActive = ListView(w).items_(listActive).mouseDownAction_({
				|view, x, y, modifiers, buttonNumber, clickCount|
				(buttonNumber == 0 and: clickCount == 2).if {
					model.deactivateFeature(view.items[view.value]);
				};
			}),
			0, // row
			1  // column
		);
		w.front;
	}

}
