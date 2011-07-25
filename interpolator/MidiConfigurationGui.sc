MidiConfigurationGui : AbstractInterpolatorGui { 
	calculateLayoutSize {
		^Rect(0,0,400,200);
	}

	guiBody { |lay|
		layout = lay;

		StaticText( layout, 120@18 )
		.string_("MIDI Port")
		.align_(\right);
		PopUpMenu( layout, 244@18 )
		.items_(MIDIClient.destinations.collect(_.name))
		.value_()
		.action_({|me|
			model.midiPort_(MIDIOut(0));
			MIDIOut(0).connect(me.value);
		});

		StaticText( layout, 120@18 )
		.string_("MIDI Chanel")
		.align_(\right);
		PopUpMenu( layout, 244@18 )
		.items_((0..15))
		.value_(model.midiChan)
		.action_({|me|
			model.midiChan_(me.value);
		});

		StaticText( layout, 120@18 )
		.string_("MIDI Controller")
		.align_(\right);
		PopUpMenu( layout, 244@18 )
		.items_((0..127))
		.value_(model.midiCtl)
		.action_({|me|
			model.midiCtl_(me.value);
		});

		StaticText( layout, 120@18 )
		.string_("Send MIDI")
		.align_(\right);
		PopUpMenu( layout, 244@18 )
		.items_(["OFF", "ON"])
		.value_(model.sendMIDI.binaryValue)
		.action_({|me|
			model.sendMIDI_(me.value.asBoolean);
		});

		StaticText( layout, 120@18 )
		.string_("")
		.align_(\right);
		Button( layout, 244@18 )
		.states_([["Reinitialize MIDI"]])
		.action_({|bt|
			MIDIClient.init;
		});
		
	}
}