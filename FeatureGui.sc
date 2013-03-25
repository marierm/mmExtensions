FeatureGui : ObjectGui {
	var <>actions, prefixView, portView, addrView, monitor, w;

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
		// var w;
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
				Button(w).states_(
					[["monitor"]]
				).action_({ |i|
					BusPlot(model.bus, model.name);
				}).maxHeight_(22)
			)
		);
		w.layout.add(
			QHLayout(
				Button(w).states_(
					[["Create a Looper for this Feature"]]
				).action_({ |i|
					model.loop;
				}).maxHeight_(22)
			)
		);

		w.layout.add(
			QHLayout(
				[
					StaticText(w).string_("Trim this Feature With:").align_(\right)
					.minWidth_(250).maxSize_(300@22), s:1
				],
				[
					PopUpMenu(w, 85@22).items_(
						model.interface.featureNames.asArray;
					).action_({ |menu|
						model.trim(
							model.interface[menu.item.asSymbol]
						);
					}).maxHeight_(22), s:1
				]
			);
		);

		(model.class == LooperFeature).if {
			var list;
			w.layout.add(
				QHLayout(
					[
						StaticText(w).string_("Control Recording With:").align_(\right)
						.minWidth_(250).maxSize_(300@22), s:1
					],
					[
						PopUpMenu(w, 85@22).items_(
							list = Sponge.featureList.collect({|i|
								i[\name]
							}).select({|i|
								"button.*".matchRegexp(i.asString);
							})
						).value_(
							model.looperControl.recTrig.notNil.if({
								list.indexOf(model.looperControl.recTrig.name)
							},{nil})
						).action_({ |menu|
							model.looperControl.recTrig_(
								menu.item.featurize(model.interface)
							);
						}).maxHeight_(22), s:1
					]
				)
			);
			w.layout.add(
				QHLayout(
					[
						StaticText(w).string_("Control Playback With:").align_(\right)
						.minWidth_(250).maxSize_(300@22), s:1
					],
					[
						PopUpMenu(w, 85@22).items_(
							list
						).value_(
							model.looperControl.pbTrig.notNil.if({
								list.indexOf(model.looperControl.pbTrig.name)
							},{nil})
						).action_({ |menu|
							model.looperControl.pbTrig_(
								menu.item.featurize(model.interface)
							);
						}).maxHeight_(22), s:1
					]
				)
			);
		};
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
