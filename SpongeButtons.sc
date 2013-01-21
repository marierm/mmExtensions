ButtonBank {
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
	var <size, <>action;

	*new { |size, initAction|
		^super.newCopyArgs(size).init(initAction);
	}

	*sponge { |sponge|
		^super.new().spongeInit(sponge);
	}
	
	init { |initAction|
		initAction.value;
	}

	spongeInit { |sponge|
		var oldVal = 0;
		size = 10;
		sponge.action_(
			sponge.action.addFunc({|values|
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
