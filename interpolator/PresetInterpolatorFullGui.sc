PresetInterpolatorFullGui : AbstractInterpolatorGui {
	// model is an Interpolator.
	calculateLayoutSize {
		^Rect(0,0,1200,400)
	}

	guiBody{ |lay|
		layout = lay;
		model.gui2D(layout);
		model.namesGui(layout);
		model.interpolatorGui(layout);
	}
}