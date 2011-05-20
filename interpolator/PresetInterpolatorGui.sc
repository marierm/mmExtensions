PresetInterpolatorGui : AbstractInterpolatorGui {
	//model is a PresetInterpolator
	var <>guiItems, butHeight;

	init {
		butHeight = 18;
		guiItems = List[];
		actions = IdentityDictionary[
			\presetAdded -> {|prInterpolator, what, preset|
				this.addPresetLine(preset, model.presets.size - 1);
				layout.resizeToFit();
			},
			\presetRemoved -> {|prInterpolator, what, i|
				// Remove all lines starting from the line to be removed.
				(i..guiItems.size - 1).do { |j|
					guiItems[j].flatten.do{ |guiItem|
						guiItem.remove;
					}
				};
				// Remove the objects in guiItems
				guiItems = guiItems.keep(i);
				// Reset layout postion so that new lines appear at the right
				// place.
				layout.decorator.top_(
					((butHeight+4)*(guiItems.size)) + 4
				);
				// Redraw lines
				if (model.presets.size != i) {
					(i..model.presets.size-1).do { |j,k|
						this.addPresetLine(model.presets[j],j);
					};
				};
			},
			\presetName -> {|prInterpolator, what, presetId, name|
				guiItems[presetId][0].string_(name);
			}
		];
	}
	
	calculateLayoutSize {
		var width, height;
		width = 150;
		height = ((butHeight+4)*(model.presets.size + 1)).max(100);
		^Rect(0,0,width,height);
	}

	guiBody {|lay|
		layout = lay;
		this.drawHeader;
		model.presets.do{|preset, i|
			this.addPresetLine(preset, i);
		}
	}

	drawHeader {
		StaticText(layout, (this.calculateLayoutSize.width - 2)@butHeight)
		.string_("Preset Name")
		.align_(\centre);
	}

	addPresetLine { |preset, i|
		//guiItems contains arrays of guiItems:
		//[[presetName, grabButton, editButton], [], ...]
		guiItems.add([
			// Preset name
			TextField(layout, 100@butHeight)
			.string_(preset.name)
			.background_(model.getPresetColor(i))
			.action_({|tf|
				preset.name_(tf.value);
			}),
			// attach button
			Button( layout, (butHeight@butHeight))
			.states_([
				["A",Color.black, Color.clear], ["A", Color.black, Color.red]
			])
			.action_({ |bt|
				// start moving this points values with cursor values.
				bt.value.switch(
					0, {
						model.attachedPoint_(nil);
					},
					1, {
						guiItems.do{ |a,b|
							(b != i).if {
								a[1].value_(0);
							};
						};
						model.attachedPoint_(i);
					}
				)
			}),
			// edit buttonbutton
			Button( layout, (butHeight@butHeight))
			.states_([["E"]])
			.action_({
				preset.gui.background_(model.getPresetColor(i));
			})
		]);
	}
	// Eventually remove (when QT has drag and drop)
	writeName {}
}