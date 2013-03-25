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

	var <buttonMode, <id, <>functions, <>level;
		
	// functions is an IdentityDictionary with two pairs: true: [{},{}, ...]
	// and false: [{},{}, ...]

	*new { |buttonMode, id, functions|
		^super.newCopyArgs(buttonMode, id).init(functions);
	}

	init { |funcs|
		level = 0;
		functions = funcs ? 
		Dictionary.newFrom([
			true, Array.fill(4, {|i|
				{|val|
					"Button id: ".post; id.postln;
					"Level: ".post; i.postln;
					"Value: ON".postln; 
				}
			}),
			false, Array.fill(4, {|i|
				{|val|
					"Button id: ".post; id.postln;
					"Level: ".post; i.postln;
					"Value: OFF".postln; 
				}
			})
		]);
	}

	value {|val, id|
		val.if{ level = buttonMode.level; };
		functions.at(val).wrapAt(level).value(val, level, id);
	}

	makeModifier { |bit|
		functions[true] = [
			{buttonMode.level_(buttonMode.level.setBit(bit,true) )}
		];
		functions[false] = [
			{buttonMode.level_(buttonMode.level.setBit(bit,false) )}
		];
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