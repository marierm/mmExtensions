AbstractSponge {
	classvar <>featureList, <>sponges;
	var <portName, <featureNames, <features;
	var <>action, <values, <interpActions;

	*new { arg portName, baudRate=115200;
		Platform.case(
			\linux, {
				SerialPort.devicePattern = "/dev/tty[A,U]*"
			},
			\osx, {
				SerialPort.devicePattern = "/dev/tty.usb*"
			},
			\windows, {
				SerialPort.devicePattern = nil
			},
		);
		// If the portname is not specified, select the first matching port.
		// This is platform dependant, but not thoroughly tested.
		portName = portName ? SerialPort.devices[0];
		
		^super.newCopyArgs(portName).init(baudRate);
	}

	save { |path|
		var file, content;
		path = path ? (Platform.userAppSupportDir ++"/scratch.sponge");
		file = File.new( path, "w" );
		content = this.features.collect(_.saveDictionary).asCompileString;
		file.write(content);
		file.close;
	}

	load {|path|
		var file, content;
		path = path ? (Platform.userAppSupportDir ++"/scratch.sponge");
		file = File.new( path, "r" );
		content = file.readAllString.interpret;
		file.close;

		//deactivate everything
		features.do({|i|
			(i.class != SensorFeature).if({
				i.remove;
			});
		});

		content.do({ |i|
			i.at(\class).switch(
				SensorFeature, {},
				LangFeature, {
					this.activateFeature(i[\name]);
				},
				ButtonFeature, {
					this.activateFeature(i[\name]);
				},
				SynthFeature, {
					this.activateFeature(i[\name], *i[\args]);
				},
				LooperFeature, {
					this.at(i[\input][0]).loop(
						i[\xFade],
						i[\maxDur]
					).controlWith(
						i[\recControl],
						i[\pbControl]
					);
				},
				TrimFeature, {
					this.at(i[\target]).trim(
						this.at(i[\input][0]),
						i[\inMin],
						i[\inMax],
						i[\amount]
					);
				}
			)
		});
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
		var func;
		interpActions = interpActions ? Dictionary(); // Should probably be in
													  // init method.
		func = { |msg|
			prInt.cursorPos_(msg.keep(prInt.numDim));
		};
		interpActions.put(prInt, func);
		action = action.addFunc(interpActions[prInt]);
	}

	disconnect {|prInt|
		action = action.removeFunc(interpActions.at(prInt));
		interpActions.removeAt(prInt);
		// This is messy.  If the sponge is connected to more than one
		// interpolator,this will not work.
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
	
	// createFeature { |key ... args|
	// 	this.deprecated(
	// 		thisMethod,
	// 		this.class.findRespondingMethodFor(\activateFeature)
	// 	);
	// 	this.activateFeature(key, *args);
	// }
	
	activateFeature { |key ... args|
		var fe, nameList, inputs, newFeat;
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
		// use the args supplied as arguments to activateFeatures.
		// (Replace the default ones.)
		args.pairsDo({|i,j|
			fe[\args][fe[\args].indexOf(i) + 1] = j;
		});
		// use the right method (.sensor, .lang or .synth)
		newFeat = Feature.performList(
			fe[\type],
			[fe[\name], this, inputs, fe[\func], fe[\args]]
		);
		this.changed(\featureActivated, fe);
		^newFeat;
	}

	deactivateFeature { |key|
		features[featureNames.indexOf(key)].remove;
		this.changed(\featureDeactivated, key);
	}

	guiClass { ^SpongeGui }

	*initClass {
		Class.initClassTree(Feature);
		sponges = List[]; // A list of all active sponges.
		
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
			featureList.add(
				(name:(i ++ \BP).asSymbol, input:[i],
					func:Feature.synthDefs[\BP], type:\synth,
					args:[\freq, 4.0, \rq, 1.0]
				).know_(false)
			);
			featureList.add(
				(name:(i ++ \Trig).asSymbol, input:[(i ++ \HP).asSymbol],
					func:Feature.synthDefs[\trig], type:\synth,
					args:[\thresh, 8, \dur, 2.0833333333333e-05, \scale, 0.001]
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

	close {
		features.copy.do{|i|
			i.remove;
		};
		this.class.sponges.remove(this);
	}

	cmdPeriod {
		this.close;
	}
}


SpongePD : AbstractSponge {
	var oscDef, <pdProcess;

	init { arg br;
		pdProcess = this.pdCommand(br).unixCmd({|res,pid|
			"Pure Data is now dead.".postln;
		});
		
		values = Int16Array.newClear(9);

		oscDef = OSCdef(\sponge, {|data|
			values = Int16Array.newFrom(data[1..]);
			// values.putEach((0..8), data[1..]);
			action.value(values);
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

		CmdPeriod.doOnce(this);
		ShutDown.add({this.close});
		
		this.class.sponges.add(this);
	}

	pdCommand { |br|
		^("pdextended -nogui -noaudio -nomidi"
		+ "-send \";udpsend connect 127.0.0.1" + NetAddr.langPort
		+ ";comport baud" + br ++ ";comport devicename" + portName
		++ "\"" 
		+ Platform.userExtensionDir +/+ "mmExtensions/serialSpongeSLIP.pd");
	}

	close {
		super.close;
		// features.copy.do{|i|
		// 	i.remove;
		// };
		("kill" + pdProcess).unixCmd;
		// ("killall pdextended").unixCmd;
		this.class.sponges.remove(this);
	}

	kill {
		this.close;
		// ("killall pd").unixCmd;
	}

	cmdPeriod {
		this.close;
	}
}

SpongeOSC : AbstractSponge {
	var oscDef, oscAddr, oscDefName;

	init {|oscAddr, oscDefName|
		oscAddr = "/sponge/mm";
		oscDefName = \sponge;
		values = Int16Array.newClear(9);
		oscDef = OSCdef(oscDefName, {|data|
			data[1].pairsDo({|msb, lsb, i|
				values[i/2] = ((msb << 3) | (lsb & 7));
			});
			action.value(values);
		}, oscAddr, nil, 0x6D6D);// 0x6D = 109 = m in ascii. So port = my
								 // initials.

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

		CmdPeriod.doOnce(this);
		ShutDown.add({this.close});

		this.class.sponges.add(this);
	}

	close {
		oscDef.remove;
		this.class.sponges.add(this);
	}

	cmdPeriod {
		this.class.sponges.remove(this);
		this.close;
	}
}

SpongePDOSC : SpongePD {

	pdCommand {
		^("pdextended -nogui -noaudio -nomidi"
		+ "-send \";udpsend connect 127.0.0.1" + NetAddr.langPort
		++ "\"" 
		+ Platform.userExtensionDir +/+ "mmExtensions/spongeSLIP-OSC.pd");
	}
}