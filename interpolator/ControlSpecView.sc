ControlSpecView {
	var parentItem, <spec, <treeItem, paramView;
	var slider, mapped, unmapped, name, curve, widgets;

	*new { |parent, spec, paramView|
		^super.new.init(parent, spec, paramView);
	}

	update { |sp, what ... args|
		what.switch(
			\minval, {
				widgets[0].value_(sp.minval);
			},
			\maxval, {
				widgets[1].value_(sp.maxval);
			},
			\warp, {
				widgets[2].value_(Warp.subclasses.indexOf(sp.warp.class));
			},
			\step, {
				widgets[3].value_(sp.step);
			}
		);
	}

	init { |parent, aSpec, parameterView|
		var maxDecimals = 4;
		parentItem = parent.addChild(["ControlSpec"]);
		spec = aSpec;
		paramView = parameterView;
		parentItem.addChild(["min","max","warp","step","default","units","preset"]);
		treeItem = parentItem.addChild(""!7);
		paramView.parameter.spec.addDependant(this);
		//return = spec.deepCopy;
		widgets = [
			NumberBox()
			.maxDecimals_(maxDecimals)
			.value_(paramView.parameter.spec.minval)
			.action_({|i|
				paramView.parameter.spec.minval_(i.value);
			}),
			NumberBox()
			.maxDecimals_(maxDecimals)
			.value_(paramView.parameter.spec.maxval)
			.action_({|i|
				paramView.parameter.spec.maxval_(i.value);
			}),
			PopUpMenu().items_(Warp.subclasses.collect(_.asSymbol))
			.value_(
				Warp.subclasses.indexOf(paramView.parameter.spec.warp.class);
			)
			.action_({|i|
				paramView.parameter.spec.warp_(Warp.subclasses[i.value]);
				(paramView.parameter.spec.warp.class == CurveWarp).if {
					curve.enabled_(true);
				}{
					curve.enabled_(false);
				}
			}),
			NumberBox()
			.maxDecimals_(maxDecimals)
			.value_(paramView.parameter.spec.step)
			.action_({|i|
				paramView.parameter.spec.step_(i.value);
			}),
			NumberBox()
			.maxDecimals_(maxDecimals)
			.value_(paramView.parameter.spec.default)
			.action_({|i|
				paramView.parameter.spec.default_(i.value);
			}),
			TextField().value_(paramView.parameter.spec.units)
			.action_({|i|
				paramView.parameter.spec.units_(i.value.asString);
			}),
			PopUpMenu().items_(
				[\presets] ++ Spec.specs.select({|i|
					(i.class == ControlSpec)
				}).keys.asArray.sort
			)
			.value_(0)
			.action_({|i|
				var args;
				if (i.value != 0) {
					args = i.item.asSymbol.asSpec.storeArgs;
					//check if the warp is symbol or number.
					// Replace it by an id number to correspond to PopUpMenu and set
					// the curve value appropriatly.
					args[2].isSymbol.if({
						curve.enabled_(false);
						args[2] = Warp.subclasses.indexOf(Warp.warps.at(args[2]));
					},{
						curve.enabled_(true);
						curve.valueAction_(args[2]);
						args[2] = Warp.subclasses.indexOf(CurveWarp);
					});
					// Change the values of all the widgets.
					args.do{|j,k|
						widgets[k].valueAction_(j);
					}
				};
			});
		];
		widgets.do({|i,j| treeItem.setView(j,i) });

		curve = NumberBox().value_(2)
			.action_({|i|
				paramView.parameter.spec.warp_(i.value);
			});
		paramView.parameter.spec.warp.class.switch(
			CurveWarp, {
				curve.enabled_(true);
				curve.value_(paramView.parameter.spec.warp.curve);
			},
			{curve.enabled_(false);}			
		);


		parentItem.addChild(""!3).setView(2, curve);
	}
}