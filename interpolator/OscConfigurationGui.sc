OscConfigurationWindow {
	var model, window, hostname, port, message, sendOSC;

	*new { |model|
		^super.newCopyArgs(model).init;
	}

	init {
		model.addDependant(this);
		
		window = Window(
			"OSC configuration of" + model.name.asString,
			Rect.aboutPoint(Window.screenBounds.center, 125, 70)
		);
		
		window.layout_(
			VLayout(
				HLayout(
					StaticText().string_("Hostname").align_(\right),
					hostname = TextField().value_(
						model.netAddr.hostname
					).action_({|tf|
						model.netAddr.hostname_(tf.value);
					});
				),
				HLayout(
					StaticText().string_("Port").align_(\right),
					port = TextField().value_(
						model.netAddr.port
					).action_({|tf|
						model.netAddr.port_(tf.value.asInteger);
					});
				),
				HLayout(
					StaticText().string_("Message").align_(\right),
					message = TextField().value_(
						model.oscMess
					).action_({|tf|
						model.oscMess_(tf.value);
					});
				),
				HLayout(
					StaticText().string_("Send OSC").align_(\right),
					sendOSC = PopUpMenu().items_(["OFF", "ON"]).value_(
						model.sendOSC.asInteger
					).action_({|me|
						model.sendOSC_(me.value.asBoolean);
					});
				)
			)
		);
		window.front;
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