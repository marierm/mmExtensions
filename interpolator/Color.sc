+Color {
	// returns a nice color for a Preset.
	*getPresetColor { |i|
		^Color.hsv(0,0.5,0.7,1).hue_(
			Color.hsv(0,0.5,0.7,1).hue + (0.094117647058824 * i) % 1
		);
	}

	*getNextPresetColor { |color|
		^color.copy.hue_(
			(color.hue + 0.094117647058824) % 1
		);
	}
}
