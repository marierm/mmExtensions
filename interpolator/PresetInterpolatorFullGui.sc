PresetInterpolatorFullGui : AbstractInterpolatorGui {
	// model is a PresetInterpolator.
	calculateLayoutSize {
		var width, height, butHeight=18;
		width = ((54*(model.numDim + 1)) + (butHeight*2)) + 166;
		height = ((model.presets.size + 3) * 22);
		^Rect(0,0,width,height);
	}

	guiBody{ |lay|
		layout = lay;
		model.namesGui(layout);
		model.interpolatorGui(layout);
		// model.gui2D(layout);
	}

	init {
		model.addDependant(this);
		actions = IdentityDictionary[
			\presetAdded -> {|presetInterpolator, what, point|
				layout.view.bounds = this.calculateLayoutSize;
				layout.parent.bounds = this.calculateLayoutSize;
			}
		];
	}
}