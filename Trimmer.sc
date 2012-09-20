// Copyright 2012 Martin Marier

// The trim feature is used to trim other features.  In works like the trim
// automation mode available in most DAW.  Its output is its input and its
// target mixed together.  We have to specify how much the input will affect
// the target.  Therefore, the range of the input (inMin and inMax) and the
// amount applied to the target (amount) must be specified.
TrimFeature : Feature {
	var <target, <inMin, <inMax, <amount, <synth, def;

	// Unlike the other features, input is a Feature, not an Array of
	// Features.
	*new { |name, interface, input, target, inMin=0, inMax=1023, amount=512.0|
		^super.newCopyArgs(name, interface, [input]).init(
			target, inMin, inMax, amount
		);
	}

	saveDictionary {
		var dict;
		dict = super.saveDictionary.interpret;
		dict.put(\target, target.name);
		dict.put(\inMin, inMin);
		dict.put(\inMax, inMax);
		dict.put(\amount, amount);
		^dict.asCompileString;
	}

	init { |tgt, min, max, amnt|
		super.init;
		input[0].dependantFeatures.add(this);
		server.serverRunning.not.if {
			("Synth features need a server to work properly.").warn;
			("Feature '" ++ name ++ "' will not be activated.").warn;
			^nil;
		};
		target = tgt;
		inMin = min;
		inMax = max;
		amount = amnt;
		
		def = SynthDef(
			\trimmer,
			// in0 is the bus number of the feature that will trim another
			// one.  target is is the bus number of the feature that will be
			// trimmed by in0.  out is the bus number of the result.  We have
			// to specify how much the input will affect the target.
			// Therefore, the range of the input (inMin and inMax) and the
			// amount applied to the target (amount) must be specified.
			{ |out=100, in0=0, target=1, inMin=0, inMax=1023, amount=512.0|
				var in, tgt;
				in = In.kr(in0, 1);
				tgt = In.kr(target, 1);
				// scale in between -1.0 and 1.0
				in = ( (2 * (in - inMin)) * (inMax - inMin).reciprocal) - 1;
				Out.kr( out, Mix([tgt, in * amount]) );
			}
		);

		fork {
			bus = Bus.control(server);
			def.add;
			server.sync;
			synth = Synth.tail(
				server,
				def.name,
				[
					\out, bus.index,
					\in0, input[0].bus.index,
					\target, target.bus.index,
					\inMin, inMin,
					\inMax, inMax,
					\amount, amount
				]
			);
			server.sync;
		};

		
		fullFunc = {
			bus.get{|value|
				action.value(value);
				// netAddr.sendMsg(oscPath, value);
			};
		};
		
		interface.action_(interface.action.addFunc(fullFunc));
		interface.features.add(this);
		interface.featureNames.add(name);
		interface.changed(\featureActivated);
	}

	remove {
		super.remove;
		// remove myself from the
		// dependantFeatures list of others.
		input[0].dependantFeatures.remove(this);
	}

	inMin_ { |val|
		inMin = val;
		synth.set(\inMin, val);
		this.changed(\inMin);
	}

	inMax_ { |val|
		inMax = val;
		synth.set(\inMax, val);
		this.changed(\inMax);
	}

	amount_ { |val|
		amount = val;
		synth.set(\amount, val);
		this.changed(\amount);
	}

	guiClass { ^TrimFeatureGui }
}

TrimFeatureGui : FeatureGui {
	var inMinBox, inMaxBox, amountBox;
	init {
		super.init;
		actions.put( \inMin,
			{|model, what|
				inMinBox.value_(model.inMin);
			}
		);
		actions.put( \inMax,
			{|model, what|
				inMaxBox.value_(model.inMax);
			}
		);
		actions.put( \amount,
			{|model, what|
				amountBox.value_(model.amount);
			}
		);
	}

	gui {
		super.gui;
		w.layout.add(
			QHLayout(
				[
					StaticText(w).string_(
						"Minimum Incoming Value: "
					).align_(\right).minWidth_(250).maxSize_(300@22), s:1
				],
				[
					inMinBox = NumberBox(w, 85@22).value_(
						model.inMin
					).action_({ |nb|
						model.inMin_(nb.value);
					}).maxHeight_(22), s:1
				]
			)
		);
		w.layout.add(
			QHLayout(
				[
					StaticText(w).string_(
						"Maximum Incoming Value: "
					).align_(\right).minWidth_(250).maxSize_(300@22), s:1
				],
				[
					inMaxBox = NumberBox(w, 85@22).value_(
						model.inMax
					).action_({ |nb|
						model.inMax_(nb.value);
					}).maxHeight_(22), s:1
				]
			)
		);
		w.layout.add(
			QHLayout(
				[
					StaticText(w).string_(
						"Amount: "
					).align_(\right).minWidth_(250).maxSize_(300@22), s:1
				],
				[
					amountBox = NumberBox(w, 85@22).value_(
						model.amount
					).action_({ |nb|
						model.amount_(nb.value);
					}).maxHeight_(22), s:1
				]
			)
		);
	}
}

+ Feature {
	trim { |input, inMin=0, inMax=1023, amount=512.0|
		// Add a number and increment it if a Feature with the same name
		// already exists.
		var newName, i;
		newName = (name ++ "Trimmed").asSymbol;
		i = 1;
		{interface.featureNames.includes( newName)}.while({
			newName = (name ++ "Trimmed" ++ i).asSymbol;
			i = i + 1;
		});

		^Feature.trim(
			name: newName,
			interface: interface,
			input: input,
			target: this,
			inMin: inMin,
			inMax: inMax,
			amount: amount
		);
	}

	*trim { |name, interface, input, target, inMin=0, inMax=1023, amount=1.0|
		^TrimFeature.new(
			name, interface, input, target, inMin, inMax, amount
		)
	}

}
