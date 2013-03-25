OscConfigurationGui : AbstractInterpolatorGui {
	var hostname, port, message, sendOSC;

	calculateLayoutSize {
		^Rect(0,0,400,200);
	}

	guiBody { |lay|
		layout = lay;

		StaticText( layout, 120@18 )
		.string_("Hostname")
		.align_(\right);
		hostname = TextField( layout, 244@18 )
		.value_(model.netAddr.hostname)
		.action_({|tf|
			model.netAddr.hostname_(tf.value);
		});

		StaticText( layout, 120@18 )
		.string_("Port")
		.align_(\right);
		port = TextField( layout, 244@18 )
		.value_(model.netAddr.port)
		.action_({|tf|
			model.netAddr.port_(tf.value.asInteger);
		});

		StaticText( layout, 120@18 )
		.string_("Message")
		.align_(\right);
		message = TextField( layout, 244@18 )
		.value_(model.oscMess)
		.action_({|tf|
			model.oscMess_(tf.value);
		});

		StaticText( layout, 120@18 )
		.string_("Send OSC")
		.align_(\right);
		sendOSC = PopUpMenu( layout, 244@18 )
		.items_(["OFF", "ON"])
		.value_(model.sendOSC.asInteger)
		.action_({|me|
			model.sendOSC_(me.value.asBoolean);
		});
	}

	update { |parameter, what ... args|
		what.switch(
			\OSC, {
				hostname.value_(model.netAddr.hostname);
				port.value_(model.netAddr.port);
				message.value_(model.oscMess);
				sendOSC.value_(model.sendOSC.asInteger);
			}
		);
	}

}