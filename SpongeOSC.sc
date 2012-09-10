AbstractSponge {
	classvar <>featureList;
	var <portName, <featureNames, <features;
	var <>action, <values, interpAction;

	*new { arg portName="/dev/ttyUSB0", baudRate=57600;
		^super.newCopyArgs(portName).init(baudRate);
	}

	activateAllFeatures {
		Sponge.featureList.collect({|i| i[\name] }).do{ |i|
			this.activateFeature(i);
		};
	}

	at { |key|
		^features[featureNames.indexOf(key)];
	}
	
	// Connect or disconnect to a PresetInterpolator
	connect { |prInt|
		interpAction = { |...msg|
			prInt.cursorPos_(msg.keep(prInt.numDim));
		};
		action = action.addFunc(interpAction);
	}

	disconnect {
		action = action.removeFunc(interpAction);
	}

	setOSCport { |port|
		this.features.do{ |i| i.oscPort_(port) }
	}
	
	setOSCaddr { |addr|		
		this.features.do{ |i| i.oscAddr_(addr) }
	}
	
	setOSCprefix { |prefix|
		this.features.do{ |i|
			i.oscPath = prefix.asString ++ "/" ++ i.oscPath.split.last;
		}
	}
	
	createFeature { |key ... args|
		this.deprecated(
			thisMethod,
			this.class.findRespondingMethodFor(\activateFeature)
		);
		this.activateFeature(key, *args);
	}
	
	activateFeature { |key ... args|
		var fe, nameList, inputs;
		this.featureNames.includes(key).if{
			"Feature % already exists.\n".postf(key);
			^this[key];
		};
		// get a list af the names of the features;
		nameList = Sponge.featureList.collect({|i| i[\name] });
		// fe is an Event describing a feature.
		fe = Sponge.featureList[nameList.indexOf(key)];
		// if type is sensor, no need to do this.
		(fe[\type] != \sensor).if{
			// check if inputs exist
			fe[\input].do{|i|
				// if they don't, activate them.
				this.featureNames.includes(i).not.if{
					this.activateFeature(i);
				};
			};
			// fe.input contains names of Features.
			// This gets the actual Feature.
			inputs = fe[\input].collect({|i| this[i]});
		} {
			inputs = fe[\input];
		};
		// use the right method (.sensor, .lang or .synth)
		Feature.performList(
			fe[\type],
			[fe[\name], this, inputs, fe[\func], fe[\args]]
		);
		this.changed(\featureActivated, fe);
	}

	deactivateFeature { |key|
		features[featureNames.indexOf(key)].remove;
		this.changed(\featureDeactivated, key);
	}

	guiClass { ^SpongeGui }

	*initClass {
		// featureList contains a list of predefined features.  They have to
		// be activated with aSponge.activateFeature(\name) This list is
		// ordered because of dependancies: Features that depend on others
		// (their input is another feature) are at the end of the list.  This
		// way, it is possible to iterate over this list to activate all
		// features.
		featureList = List[
			(name:\acc1x, input:0, type:\sensor),
			(name:\acc1y, input:1, type:\sensor),
			(name:\acc1z, input:2, type:\sensor),
			(name:\acc2x, input:3, type:\sensor),
			(name:\acc2y, input:4, type:\sensor),
			(name:\acc2z, input:5, type:\sensor),
			(name:\fsr1, input:6, type:\sensor),
			(name:\fsr2, input:7, type:\sensor),
			(name:\buttons, input:8, type:\sensor),
			(name:\accelGlob1, input:[\acc1x, \acc1y, \acc1z],
				func:Feature.funcs[\accelGlobal], type:\lang
			).know_(false),
			(name:\accelGlob2, input:[\acc2x, \acc2y, \acc2z],
				func:Feature.funcs[\accelGlobal], type:\lang
			).know_(false),
			(name:\acc1xIntegral, input:[\acc1x],
				func:Feature.funcs[\integrate], type:\lang
			).know_(false),
			(name:\acc1yIntegral, input:[\acc1y],
				func:Feature.funcs[\integrate], type:\lang
			).know_(false),
			(name:\acc1zIntegral, input:[\acc1z],
				func:Feature.funcs[\integrate], type:\lang
			).know_(false),
			(name:\acc2xIntegral, input:[\acc2x],
				func:Feature.funcs[\integrate], type:\lang
			).know_(false),
			(name:\acc2yIntegral, input:[\acc2y],
				func:Feature.funcs[\integrate], type:\lang
			).know_(false),
			(name:\acc2zIntegral, input:[\acc2z],
				func:Feature.funcs[\integrate], type:\lang
			).know_(false),
			(name:\pitch1, input:[\acc1x, \acc1z],
				func:Feature.funcs[\atan], type:\lang
			).know_(false),
			(name:\roll1, input:[\acc1y, \acc1z],
				func:Feature.funcs[\atan], type:\lang
			).know_(false),
			(name:\yaw1, input:[\acc1x, \acc1y],
				func:Feature.funcs[\atan], type:\lang
			).know_(false),
			(name:\pitch2, input:[\acc2x, \acc2z],
				func:Feature.funcs[\atan], type:\lang
			).know_(false),
			(name:\roll2, input:[\acc2y, \acc2z],
				func:Feature.funcs[\atan], type:\lang
			).know_(false),
			(name:\yaw2, input:[\acc2x, \acc2y],
				func:Feature.funcs[\atan], type:\lang
			).know_(false),
			(name:\pitch, input:[\pitch1, \pitch2],
				func:Feature.funcs[\meanCircle], type:\lang
			).know_(false),
			(name:\roll, input:[\roll1, \roll2],
				func:Feature.funcs[\meanCircle], type:\lang
			).know_(false),
			(name:\yaw, input:[\yaw1, \yaw2],
				func:Feature.funcs[\meanCircle], type:\lang
			).know_(false),
			// bend, twist, fold
			(name:\bend, input:[\pitch1, \pitch2],
				func:Feature.funcs[\diffCircle], type:\lang
			).know_(false),
			(name:\twist, input:[\roll1, \roll2],
				func:Feature.funcs[\diffCircle], type:\lang
			).know_(false),
			(name:\fold, input:[\yaw1, \yaw2],
				func:Feature.funcs[\diffCircle], type:\lang
			).know_(false),
			(name:\pseudoBend, input:[\acc1x, \acc2x],
				func:Feature.funcs[\diff], type:\lang
			).know_(false),
			// fsr mean
			(name:\fsrMean, input:[\fsr1, \fsr2],
				func:Feature.funcs[\meanMany], type:\lang
			).know_(false),
			// fsr diff
			(name:\fsrDiff, input:[\fsr1, \fsr2],
				func:Feature.funcs[\diff], type:\lang
			).know_(false)
		];
		featureList.collect({|i| i[\name] }).do{|i|
			featureList.add(
				(name:(i ++ \Slope).asSymbol, input:[i],
					func:Feature.funcs[\slope], type:\lang
				).know_(false)
			)
		};
		featureList.collect({|i| i[\name] }).do{|i|
			featureList.add(
				(name:(i ++ \LP).asSymbol, input:[i],
					func:Feature.synthDefs[\LP], type:\synth,
					args:[\freq, 3]
				).know_(false)
			);
			featureList.add(
				(name:(i ++ \HP).asSymbol, input:[i],
					func:Feature.synthDefs[\HP], type:\synth,
					args:[\freq, 100]
				).know_(false)
			);
		};
		// buttons
		10.do{|i|
			featureList.add(
				(name:(\button ++ i.asString).asSymbol,
					input:[\buttons],
					type: \button,
					args: [i] //passes the button number.
				).know_(false);
			);
		};
	}
}


SpongePD : AbstractSponge {
	var oscDef, pdProcess;

	init { arg br;
		var pdCommand;
		pdCommand = ("pd -send \";udpsend connect 127.0.0.1" + NetAddr.langPort
			+ ";comport baud" + br
			++ ";comport devicename" + portName ++ "\"" 
			+ Platform.userExtensionDir +/+ "mmExtensions/serialSponge.pd");

		pdProcess = pdCommand.unixCmd({|res,pid| "Pure Data is now dead.".warn;});
		
		values = Int16Array.newClear(9);

		oscDef = OSCdef(\sponge, {|data|
			values.putEach((0..8), data[1..]);
			action.value(*data[1..]);
		},"/sponge");
		
		features = List[];
		featureNames = List[];
		// Add Features for each sensor
		[
			\acc1x, \acc1y, \acc1z,
			\acc2x, \acc2y, \acc2z,
			\fsr1, \fsr2, \buttons
		].do{|i,j|
			Feature.sensor(i,this,j);
		};
	}

	close {
		features.do{|i|
			i.remove;
		};
		("kill" + pdProcess).unixCmd;
	}

}