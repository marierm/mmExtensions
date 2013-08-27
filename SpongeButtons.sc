ButtonBank {
	// A ButtonBank manages a bank of buttons.  The input has to be a
	// ButtonInput.  This class makes it easier to assign different behaviors
	// to a set of buttons.

	var <input, <modes, <currentMode, function;
	
	// input is a ButtonInput.
	// modes is an Array of ButtonMode.
	*new { |input, modes|
		^super.newCopyArgs(input, modes).init();
	}
	
	init {
		modes = modes ? [ ButtonMode(this) ];
		this.mode_(0);
	}

	mode_ { |id|
		currentMode = (id).wrap(0, modes.size);
		input.action_({ |buttVal, ids|
			modes[currentMode].value(buttVal, ids);
		});
	}
	
	mode {
		^modes[currentMode];
	}

	nextMode {
		this.mode_(currentMode + 1);
	}

	previousMode {
		this.mode_(currentMode - 1);
	}

	addMode { |mode|
		modes = modes.add(mode);
	}

	newMode {
		this.addMode( ButtonMode(this) );
	}

	removeMode { |id|
		modes = modes.removeAt(id);
	}

	size { ^input.size; }
}

ButtonMode {
	// a ButtonMode has a ButtonFunction for each button.  
	var <bank, <>buttonFunctions, <>level;

	*new { |buttonBank, buttonFuncs|
		^super.newCopyArgs(buttonBank, buttonFuncs).init();
	}

	init {
		level = 0;
		buttonFunctions = buttonFunctions ? Array.fill(
			bank.size, {|i|
				ButtonFunction(this, i, nil);
			}
		);
	}

	value { |val, ids|
		// ids represents the bits that changed (val bitXor: oldVal).  iterate
		// overall bits that changed (only one should change, but it is not
		// guaranteed).
		ids.asBinaryDigits(bank.size).reverse.indicesOfEqual(1).do({ |i|
			buttonFunctions[i].value(val.bitTest(i), i);
		});
	}
	
}

ButtonFunction {
	// Holds the functions for a single button.  A ButtonFunction has two
	// Arrays of functions: one for when the button is turned ON and another
	// one for when it is turned off.  Other buttons can act as modifier keys
	// and change the "level".

	var <buttonMode, <id, <>functions, <>level, <buttonEvaluator;
		
	// functions is an IdentityDictionary with at least two pairs: 1:
	// [{},{}, ...]  and 0: [{},{}, ...]
	// More pairs can be added for double clicks, triple clicks, etc:
	// 2: [{},{}, ..], 3: [{},{}, ..]

	*new { |buttonMode, id, functions|
		^super.newCopyArgs(buttonMode, id).init(functions);
	}

	init { |funcs|
		level = 0;
		functions = funcs ? 
		Dictionary.newFrom([
			1, Array.fill(4, {|i|
				{|val|
					"Button id: ".post; id.postln;
					"Level: ".post; i.postln;
					"Value: ON".postln; 
				}
			}),
			0, Array.fill(4, {|i|
				{|val|
					"Button id: ".post; id.postln;
					"Level: ".post; i.postln;
					"Value: OFF".postln; 
				}
			})
		]);
		buttonEvaluator = ButtonEvaluator(this);
	}

	value {|val, id|
		buttonEvaluator.value(val, id);
		// val.if{ level = buttonMode.level; };
		// functions.at(val).wrapAt(level).value(val, level, id);
	}

	makeModifier { |bit|
		functions[1] = [
			{buttonMode.level_(buttonMode.level.setBit(bit,true) )}
		];
		functions[0] = [
			{buttonMode.level_(buttonMode.level.setBit(bit,false) )}
		];
	}
	
	enableNclick { |numClick=2, function, levels= #[0], delay=0.1|
		buttonEvaluator.nClick_(numClick, function, levels, delay)
	}

	disableNclick { |numClick=2, function, levels= #[0], delay=0.1|
		buttonEvaluator.removeNclick(numClick);
	}
}

ButtonEvaluator {
	// The button evaluator is used to manage double-clicks (or triple,
	// quadruple, etc).  It takes care of the waiting mechanism for such a
	// functionality.  Each ButtonFunction has a ButtonEvaluator.  Multiple
	// clicks can be activated independantly for eah button.

	var <buttFunc, <buttMode, <evalFunc, maxClickCount, clickCount, routine;
	
	*new {|buttonFunction|
		^super.newCopyArgs(buttonFunction, buttonFunction.buttonMode).init();
	}

	value {|...args|
		evalFunc.value(*args);
	}

	init {
		clickCount = 0;
		evalFunc = { |val, id|
			val.if{ buttFunc.level = buttMode.level; };
			buttFunc.functions.at(val.asInt).wrapAt(
				buttFunc.level
			).value(val, buttFunc.level, id);
		};
	}
	
	removeNclick { |numClick|
		buttFunc.functions.removeAt(numClick);
		maxClickCount = buttFunc.functions.keys.maxItem;
	}

	nClick_ { |numClick=2, function, levels= #[0], delay=0.2|
		buttFunc.functions.put(
			numClick, 
			Array.fill(levels.maxItem+1, {|i|
				levels.includes(i).if({
					function
				},{
					nil
				});
			});
		);
		
		maxClickCount = buttFunc.functions.keys.maxItem;
				
		evalFunc = { |val, id|
			val.if({
				routine.stop;
				clickCount = clickCount + 1;
				buttFunc.level = buttMode.level;
				routine = {
					// clickCount.postln;
					// Wait for delay time then evaluate '1' function
					(clickCount < maxClickCount).if({
						delay.wait;
					});
					
					buttFunc.functions.at(clickCount).wrapAt(
						buttFunc.level
					).value(val, buttFunc.level, id);
					clickCount = 0;
				}.fork;
			});
		};
	}
}

ButtonInput {
	// This set of classes was designed to work with the sponge, but it is
	// possible to use any kind of button input.  ButtonInput is a bridge to
	// hardware buttons.

	// The .sponge method initializes a Button input so that it gets its
	// values from an instance of Sponge.  The generic *new method is not
	// really usable at this point.
	var <interface, <size, <>action;

	*new { |interface, size|
		^super.newCopyArgs(interface, size).init();
	}

	*sponge { |sponge|
		^SpongeButtonInput.new(sponge);
	}
	
	init {
		var oldVal = 0;
		// size = numButtons;
		interface.action_(
			interface.action.addFunc({|buttsVal|
				// buttsVal should be an integer.
				var ids;
				// Get the bits that changed.
				ids = (	buttsVal bitXor: oldVal);
				// If at least one bit changed:
				ids.asBoolean.if({
					action.value(buttsVal, ids);
				});
				oldVal = buttsVal;
			});
		);
	}

}

SpongeButtonInput : ButtonInput{
	// *new { |sponge|
	// 	^super.new().init(sponge);
	// }

	init {
		var oldVal = 0;
		size = 10;
		interface.action_(
			interface.action.addFunc({|values|
				var ids;
				// Get the bits that changed.
				ids = (	values[8] bitXor: oldVal);
				// If at least one bit changed:
				ids.asBoolean.if({
					action.value(values[8], ids);
				});
				oldVal = values[8];
			});
		);
	}

}