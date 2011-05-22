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
			},
			\presetName -> {|preset, what, name|
				layout.window.name_(name);
			}
		];
	}

	gui { arg parent, bounds ... args;
		var layout, title;
		bounds = bounds ?? this.calculateLayoutSize;
		title = model.name ? model.asString;
		layout=this.guify(parent, bounds, title);
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

	calculateLayoutSize{
		^Rect(0,0,300,400);
	}


	guiBody {|lay|
		layout = lay;		

		addButton = Button(layout, 300@18).states_(
			[["Add Parameter", background]]
		).action_({
			var name;
			// model.parameters.collect(_.name);
			model.add(Parameter());
		});
				
		model.parameters.do{|i, j|
			paramLayouts.add(i.gui(layout));
			layout.resizeToFit(true, true);
			// i.gui.background_(background);
		};

	}
}
