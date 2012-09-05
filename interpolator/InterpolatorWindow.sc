InterpolatorWindow {
	var model, tree, addButtonItem, cursorLine, pointViews, cursorView;

	*new { |model|
		^super.newCopyArgs(model).init;
	}

	init {
		var dimensions;
		// Create the name of the dimensions.  "dimensions"" will be an array
		// of strings that could look like this (if numDim is 14):
		// [ u, v, w, x, y, z, uu, vv, ww, xx, yy, zz, xxx, yyy ]
		dimensions = (1..model.n).clump(6).collect({ |i,j|
			i.collect({|k,l|
				([$x, $y, $z, $w, $v, $u].at(l) ! (j+1)).as(String);
			}).sort;
		}).flatten;
		
		pointViews = List[];
		
		// Define the treeView and its Header.
		tree = TreeView().columns_(
			["Weight", "Delete"] ++ dimensions
		).front;

		// the add button
		tree.addItem([""]).setView(
			0,
			Button().states_([["Add Point"]]).action_({ |v|
				model.add;
			})
		);

		// Cursor
		cursorView = InterpolatorCursorView(model.cursor, tree);

		model.points.do{ |point, num|
			pointViews.add(InterpolatorPointView(point, tree));
		};


		tree.setProperty(\windowTitle,"Interpolator");
		tree.onClose_({
			model.removeDependant(this);
		});
		tree.front;
		^this;
	}

	update { |model, what ... args|
		what.switch(
			\pointAdded, {
				// args[0]: the point.
				pointViews.add(InterpolatorPointView(args[0], tree));
			},
			\cursorPosition, {
			}
		)
	}
}

InterpolatorPointView {
	var <point, <parent, <item;
	var weight, coordinates;

	*new { |point, parent|
		^super.newCopyArgs(point, parent).init;
	}
	
	init {
		point.addDependant(this);
		item = parent.insertItem(parent.numItems - 2, [""]);
		coordinates = point.position.collect({ |i,j|
			NumberBox().value_(i).action_({|nb|
				point.coordinate_(j, nb.value);
			})
		});
		
		// columns: weight, delete, ... coordinates.
		([
			weight = Slider(nil,200@20).value_(point.weight),
			Button().states_([
				["X",Color.grey(0.3), Color.white],
			]).action_({
				point.remove;
			})
		] ++ coordinates
		).do({|i,j| item.setView(j,i) });

		parent.onClose_(
			parent.onClose.addFunc({
				point.removeDependant(this);
			})
		);
	}

	update { |model, what ... args|
		what.switch(
			\pointWeight, {
				// args[0] : weight
				weight.value_(args[0]);
			},
			\pointRemoved, {
				// args[0]: index of removed point
				parent.removeItem(item);
			},
			\pointPosition, {
				// args[0]: position array
				coordinates.do({|i,j|
					i.value_(args[0][j])
				});
			},
			\cursorPosition, {
			}
		)
	}
}

InterpolatorCursorView {
	var <cursor, <parent, <item;
	var coordinates;

	*new { |cursor, parent|
		^super.newCopyArgs(cursor, parent).init;
	}
	
	init {
		cursor.addDependant(this);
		item = parent.insertItem(parent.numItems - 1, [""]);
		coordinates = cursor.position.collect({ |i,j|
			NumberBox().value_(i).action_({|nb|
				cursor.coordinate_(j, nb.value);
			})
		});
		
		([
			StaticText().string_("Cursor"),
			StaticText().string_("Cursor")
		] ++ coordinates
		).do({|i,j| item.setView(j,i) });

		parent.onClose_(
			parent.onClose.addFunc({
				cursor.removeDependant(this);
			})
		);
	}

	update { |model, what ... args|
		what.switch(
			\cursorPosition, {
				// args[0]: position array
				coordinates.do({|i,j|
					i.value_(args[0][j])
				});
			}
		)
	}
}