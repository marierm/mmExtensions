PresetInterpolatorGui : AbstractInterpolatorGui {

	calculateLayoutSize {
		^Rect(0,0,800,400)
	}

	guiBody{ |lay|
		layout = lay;
		model.interpolatorGui(layout);
		model.gui2D(layout);
		// layout.resizeToFit(true, true);
	}
}