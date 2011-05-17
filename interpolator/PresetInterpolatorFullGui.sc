PresetInterpolatorFullGui : AbstractInterpolatorGui {
	// model is a PresetInterpolator.
	calculateLayoutSize {
		var width, butHeight;
		// width = ((54*(model.numDim + 1)) + ((model.butHeight)*2)) + 150;
		width = ((54*(model.numDim + 1)) + ((18)*2)) + 166;
		^Rect(0,0,width,400);
	}

	guiBody{ |lay|
		layout = lay;
		model.namesGui(layout);
		model.interpolatorGui(layout);
		// model.gui2D(layout);
	}
}