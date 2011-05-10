PresetGui : AbstractInterpolatorGui { 
	var <>background, addButton, paramLayouts;
	
	init {
		paramLayouts = List[];
		actions = IdentityDictionary[
			\paramAdded -> {|model, what, param|
				paramLayouts.add(param.gui(layout));
				layout.resizeToFit(true, true);
			},
			\paramRemoved -> {|model, what, param, id|
				paramLayouts[id].view.children[id+1].remove;
				layout.reflowAll;
				// paramLayouts[id].view.resizeToFit(true, true);
				layout.resizeToFit(true, true);
			}
		];
	}

	guiBody {|lay|
		layout = lay;		
		// background = ColorList.get(3);

		addButton = Button(layout, 300@18).states_(
			[["+", background]]
		).action_({
			model.add(Parameter())
		});
				
		model.parameters.do{|i, j|
			paramLayouts.add(i.gui(layout));
			layout.resizeToFit(true, true);
			// i.gui.background_(background);
		};

	}
}
