FeatureGui : ObjectGui {
	var <>actions, prefixView, portView, addrView;

	*new { arg model;
		var new;
		new = super.new;
		new.model_(model);
		^new.init;
	}

	init {
		actions = IdentityDictionary[
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
				).align_(\center).font_(Font.default.size_(24))
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("OSC Path").align_(\right).minWidth_(80),
				prefixView = TextField(w).string_(model.oscPath),
				Button(w).states_([["Set (only this feature)"]]).action_({
					model.oscPath_(prefixView.value);
				})
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("OSC Address").align_(\right).minWidth_(80),
				addrView = TextField(w).string_(model.netAddr.hostname),
				Button(w).states_([["Set (only this feature)"]]).action_({
					model.netAddr.hostname_(prefixView.value);
				})
			)
		);
		w.layout.add(
			QHLayout(
				StaticText(w).string_("OSC Port").align_(\right).minWidth_(80),
				portView = TextField(w).string_(model.netAddr.port),
				Button(w).states_([["Set (only this feature)"]]).action_({
					model.netAddr.port_(prefixView.value.asInteger);
				})
			)
		);
		w.front;
	}

}
