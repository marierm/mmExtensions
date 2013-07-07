ParameterGui2 : ObjectGui { 
	gui {
		var tree;
		// layout.resize_(3);
		tree = TreeView().columns_(
			["Name", "", "Mapped", "Unmapped", "OSC",""]
		).resize_(3);
		ParameterView(tree, model, 0);
		tree.front;
	}
}

ParameterView {
	var tree, <parameter, <treeItem;
	var slider, mapped, unmapped, name, oscButt;

	*new { |parent, parameter, id|
		^super.new.init(parent, parameter, id)
	}

	init { |parent, aParameter, id|
		tree = parent;
		parameter = aParameter;
		parameter.addDependant(this);
		treeItem = tree.insertItem( id,[""]);
		[
			name = TextField().string_(
				parameter.name
			).action_({ |tf|
				parameter.name_(tf.value);
			}),
			slider = Slider(nil, 200@20).value_(
				parameter.value
			).action_({ |sl|
				parameter.value_(sl.value);
			}),
			mapped = NumberBox().maxDecimals_(12).value_(
				parameter.mapped
			).action_({ |nb|
				parameter.mapped_(nb.value);
			}),
			unmapped = NumberBox().maxDecimals_(12).value_(
				parameter.value
			).action_({ |nb|
				parameter.value_(nb.value);
			}),
			oscButt = Button().states_(
				parameter.sendOSC.if({
					[[parameter.oscMess, Color.black, Color.white]]
				},{
					[["none",Color.grey(0.3), Color.grey(0.4)]]
				})
			).value_(
				parameter.sendOSC.asInteger;
			).action_({|butt|
				OscConfigurationGui.new(parameter).performList(\gui);
			}),
			// Button().states_([
			// 	["",Color.grey(0.3), Color.grey(0.4)],
			// 	["X",Color.black, Color.white]
			// ]).value_(
			// 	parameter.sendMIDI.asInteger;
			// ),
			Button().states_([
				["X",Color.grey(0.3), Color.white],
			]).action_({
				parameter.remove;
			})
		].do({|i,j| treeItem.setView(j,i) });

		ControlSpecView(
			treeItem,
			parameter.spec,
			this
		);
		
		// parameter.view_(this);
	}

	update { |parameter, what ... args|
		what.switch(
			\spec, {
				slider.value_(parameter.value);
				mapped.value_(parameter.mapped);
				unmapped.value_(parameter.value);
			},
			\value, {
				slider.value_(parameter.value);
				mapped.value_(parameter.mapped);
				unmapped.value_(parameter.value);
			},
			\name, {
				name.string_(parameter.name);
			},
			\paramRemoved, {
				parameter.removeDependant(this);
				// tree.removeItem(treeItem);
			},
			\OSC, {
				parameter.sendOSC.if({
					oscButt.states_(
						[[parameter.oscMess, Color.black, Color.white]]
					)				
				},{
					oscButt.states_(
						[["none",Color.grey(0.3), Color.grey(0.4)]]
					)
				});
			}
		);
	}
}

