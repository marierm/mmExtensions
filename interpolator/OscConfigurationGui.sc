OscConfigurationGui : AbstractInterpolatorGui { 
	calculateLayoutSize {
		^Rect(0,0,400,200);
	}

	guiBody { |lay|
		layout = lay;

		StaticText( layout, 120@18 )
		.string_("Hostname")
		.align_(\right);
		TextField( layout, 244@18 )
		.value_(model.netAddr.hostname)
		.action_({|tf|
			model.netAddr.hostname_(tf.value);
		});

		StaticText( layout, 120@18 )
		.string_("Port")
		.align_(\right);
		TextField( layout, 244@18 )
		.value_(model.netAddr.port)
		.action_({|tf|
			model.netAddr.port_(tf.value.asInteger);
		});

		StaticText( layout, 120@18 )
		.string_("Message")
		.align_(\right);
		TextField( layout, 244@18 )
		.value_(model.oscMess)
		.action_({|tf|
			model.oscMess_(tf.value);
		});

		StaticText( layout, 120@18 )
		.string_("Send OSC")
		.align_(\right);
		PopUpMenu( layout, 244@18 )
		.items_(["OFF", "ON"])
		.value_(model.sendOSC.asInteger)
		.action_({|me|
			model.sendOSC_(me.value.asBoolean);
		});

	}
}