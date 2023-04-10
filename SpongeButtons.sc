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
	var <bank, <>buttonFunctions, <>level, <>combos;

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
		combos = Dictionary();
	}

	value { |val, ids|
		// ids represents the bits that changed (val bitXor: oldVal).  iterate
		// overall bits that changed (only one should change, but it is not
		// guaranteed).
		ids.asBinaryDigits(bank.size).reverse.indicesOfEqual(1).do({ |i|
			buttonFunctions[i].value(val.bitTest(i).asInteger, i);
			combos.keysValuesDo({|combo, function|
				// check if value is a combo.
				(combo & val == combo).if({
					//check if it was already on.
					((ids bitXor: val) & combo != combo).if({
						function[1].value(val, i);
					});
				}, {// if not a combo
					// check if it was one
					((ids bitXor: val) & combo == combo).if({
						function[0].value(val, i);
					});
				});
			});
		});
	}

	// No levels for combos (no modifier keys).
	addCombo {|comboBits, function, buttState=1|
		var array;
		array = combos[comboBits] ? [nil,nil];
		combos.put(
			comboBits,
			array.put(buttState, function)
		);
	}

	removeCombo {|comboBits|
		combos.removeAt(comboBits);
	}

	addFunc { |button=0, function, levels = #[0], buttState=1|
		// Add a function to a button.

		// button is button number;

		// function is a function to which value (0
		// or 1), id (button number), level (the modifier keys status) and
		// clickCount (if nClick is enabled) are passed;

		// levels are the modifier keys statuses at which the function will be
		// evaluated;

		// buttState is 1 when button is pressed and 0 when it is depressed.
		// Other numbers represent the click count if enableNclick has been
		// called.
		var array;
		array = buttonFunctions[button].functions[
			buttState
		].asArray.extend(levels.maxItem + 1);
		array.putEach(levels, function);
		buttonFunctions[button].functions.put(buttState, array);
	}
}

ButtonFunction {
	// Holds the functions for a single button.  A ButtonFunction has many
	// Arrays of functions: one for when the button is turned ON, one for when
	// it is turned off, and others for higher clickCount.  Other buttons can
	// act as modifier keys and change the "level".

	var <buttonMode, <id, <>functions, <>level, <>evalFunc, clickCount,
	routine, <>clickCountDelay, <>clickCountMinDelay, waitMore=false;

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
			0, [],
			1, []
		]);

		clickCount = 0;
		clickCountDelay = 0.25;
		clickCountMinDelay = 0.05;
		// Double clicking is disabled by default.
		// This allows fast gestures.
		this.disableNclick;
	}

	value {|val, id|
		evalFunc.value(val, id);
	}

	makeModifier { |bit|
		functions[1] = [
			{buttonMode.level_(buttonMode.level.setBit(bit,true) )}
		];
		functions[0] = [
			{buttonMode.level_(buttonMode.level.setBit(bit,false) )}
		];
	}

	enableNclick {
		evalFunc = { |val, id|
			val.asBoolean.if({
				waitMore.not.if({
					routine.stop;
					clickCount = clickCount + 1;
					level = buttonMode.level;
					routine = {
						waitMore = true;
						clickCountMinDelay.wait;
						waitMore = false;
						clickCountDelay.wait;
						clickCount = 0;
					}.fork;
					// clickCount.postln;
					functions.at(clickCount).notNil.if({
						functions.at(clickCount).wrapAt(level).value(
							val, level, id, clickCount
						);
					});
				}, {

					"Double click not registered: it was too quick and was
					considered a rebound.".warn;

				});
			}, {
				// To test: Currently, we have to wait clickCountDelay seconds
				// with the button pressed if we want the "off" function to be
				// evaluated.  The other option would be not to wait and
				// evaluate the "off" function every time, including when
				// there potentially is a double-click coming.
				//
				// In that last case, the disable and enableNclick functions
				// are mostly useless and a static evalFunc could be used.

				// Uncomment the if to wait, comment to not wait.
				// (clickCount == 0).if({
				   // \off.postln;
				functions.at(val).notNil.if({
						functions.at(0).wrapAt(level).value(
							val, level, id, clickCount
						);
				});
			});
		};
	}

	disableNclick {
		evalFunc = { |val, id|
			val.asBoolean.if{ level = buttonMode.level; };
			functions.at(val).notNil.if({
				functions.at(val).wrapAt(level).value(val, level, id);
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