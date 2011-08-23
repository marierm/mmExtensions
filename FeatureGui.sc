FeatureGui : ObjectGui {
	var <>actions, prefixView, portView, addrView, monitor;

	*new { arg model;
		var new;
		new = super.new;
		new.model_(model);
		^new.init;
	}

	init {
		actions = IdentityDictionary[
			\oscPath -> {|model, what, path|
				prefixView.string_(path);
			},
			\oscPort -> {|model, what, port|
				portView.string_(port.asString);
			},
			\oscAddr -> {|model, what, addr|
				addrView.string_(addr);
			},
		];
	}

	update { arg model, what ... args;
		var action;
		action = actions.at(what);
		if (action.notNil, {
			action.valueArray(model, what, args);
		});
	}

	gui { arg ... args;
		var w;
		w = Window("A Feature GUI");
		w.layout_(QVLayout());
		w.layout.add(
			QHLayout(
				StaticText(w).string_(
					model.interface.class.asString + "Feature" + model.name.asString
				).align_(\center).font_(Font.default.size_(24)).maxHeight_(40)
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("Type").align_(\right)
				.minWidth_(85).maxSize_(85@22),
				StaticText(w).string_(model.class.asString).maxHeight_(22)
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("OSC Path").align_(\right).minWidth_(85).maxHeight_(22),
				prefixView = TextField(w).string_(model.oscPath).maxHeight_(22),
				Button(w).states_([["Set (only this feature)"]]).action_({
					model.oscPath_(prefixView.value);
				}).maxHeight_(22)
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("OSC Address").align_(\right).minWidth_(85).maxHeight_(22),
				addrView = TextField(w).string_(model.netAddr.hostname).maxHeight_(22),
				Button(w).states_([["Set (only this feature)"]]).action_({
					model.oscAddr_(prefixView.value);
				}).maxHeight_(22)
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("OSC Port").align_(\right).minWidth_(85).maxHeight_(22),
				portView = TextField(w).string_(model.netAddr.port).maxHeight_(22),
				Button(w).states_([["Set (only this feature)"]]).action_({
					model.oscPort_(prefixView.value.asInteger);
				}).maxHeight_(22)
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("Input(s)").align_(\right)
				.minWidth_(85).maxSize_(85@22),
				*((model.input.size == 0).if{
					StaticText(w).string_("none").maxHeight_(22)
				}{
					model.input.collect{|i|
						Button(w).states_([[i.name]]).action_({i.gui}).maxHeight_(22)
					}
				})
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("Dependant(s)").align_(\right)
				.minWidth_(85).maxSize_(85@22),
				*((model.dependantFeatures.size == 0).if{
					StaticText(w).string_("none").maxHeight_(22)
				}{
					model.dependantFeatures.collect{|i|
						Button(w).states_([[i.name]]).action_({i.gui}).maxHeight_(22)
					}
				})
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("Bus").align_(\right)
				.minWidth_(85).maxSize_(85@22),
				StaticText(w).string_(model.bus.index).maxHeight_(22),
				// monitor depends on GNUPlot Quark.
				Button(w).states_(
					[["monitor"],["stop"]]
				).action_({ |i|
					(i.value == 1).if{
						monitor = BusMonitor(model.bus, 500, 1).dt_(0.001);
						monitor.start;
						monitor.gnuplot.autoscaleY;
						monitor.gnuplot.sendCmd(
							"set title \"" ++ model.name.asString ++ "\"");
					} {
						monitor.stop;
					}
				}).maxHeight_(22)
			)
		);
		// Parameters
		
		(model.class == SynthFeature).if {
			model.def.metadata.specs.keysValuesDo{ |key,val,i|
				var slider, text;
				w.layout.add(
					QHLayout(
						[StaticText(w).string_(key).align_(\right)
						.minWidth_(85).maxSize_(85@22), s:0],
						[slider = Slider(w, 200@22).action_({ |sl|
							model.synth.set(key,val.asSpec.map(sl.value));
							text.string_(val.asSpec.map(sl.value).asString);
						}).maxHeight_(22), s:1],
						text = StaticText(w).string_("").align_(\left)
						.minWidth_(85).maxSize_(85@22)
					)
				);

				model.synth.get(key, {|i|
					{slider.valueAction_(val.asSpec.unmap(i))}.defer;
				})
			};
		};

		w.layout.add(QHLayout(StaticText(w)));
		w.onClose_({monitor.stop});
		w.front;
	}

}
