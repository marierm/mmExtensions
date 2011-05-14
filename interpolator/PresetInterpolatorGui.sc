PresetInterpolatorGui : AbstractInterpolatorGui {
	guiBody{ |lay|
		layout = lay;
		model.gui(layout);
		model.gui2D(layout);
		layout.resizeToFit(true, true);
	}
}