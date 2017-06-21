// Copyright 2011 Martin Marier
// JackMatrix is a matrix GUI to connect Jack clients.
// It depends on BoxMatrix from the crucialviews Quark.

// Arguments:

// prefix: a prefix to the jack_* command line utilities.  Using ssh, it is
// possible to control Jack connections on a remote machine.  Example:
// JackMatrix("ssh mm@192.168.0.102 /usr/bin/")

// autoUpdate: a time interval in seconds.  The GUI will be updated at this
// rate.  A value of 0 or less will turn off GUI update.  Very small values
// will probably hang sclang.

JackMatrix {
	var <>prefix, <autoUpdate, <>alias,
	<grid, <ports, <mousePos, w, sv, uv, routine;

	*new{ |prefix, autoUpdate=1.0, alias=2| // alias 0,1 or 2. Can display port
		// aliases instead of names.  I like to create aliases for my Jack
		// ports because the ones created automatically are too long and do
		// not make much sense to me.
		^super.newCopyArgs(prefix, autoUpdate, alias).init;
	}

	init {
		mousePos = Point(0,0);
		this.initPorts;
		this.initConnections;

		w = Window("JackMatrix",Rect(100,100,700,700));
		w.acceptsMouseOver_(true);
		sv = ScrollView(w, Rect(
			0,0,
			(ports.select({|i| i.props.contains("input")}).size * 20) + 180,
			(ports.select({|i| i.props.contains("output")}).size * 20) + 180
		));

		// this.updateGui;
		
		w.onClose_({routine.stop});
		w.front;

		routine = Routine({
			{
				this.initPorts;
				\port.postln;
				this.initConnections;
				\connections.postln;
				this.updateGui;
				\updateGui.postln;
				// \update.postln;
				autoUpdate.wait;
			}.loop;
		});

		(autoUpdate > 0).if { routine.play(AppClock); };

	}

	autoUpdate_ { |val|
		(val <= 0).if { routine.stop; autoUpdate = val };
		(val > 0).if { routine.play(AppClock); autoUpdate = val };
	}

	updateGui {
		var outputs, inputs;
		var outputsTextWidth=180, inputsTextWidth=180;

		outputs = ports.select({|i| i.props.contains("output")});
		inputs = ports.select({|i| i.props.contains("input")});
		
		{uv.remove}.try;
		uv = UserView(
			sv,
			Rect(0,0,(inputs.size * 20) + 200, (outputs.size * 20) + 200)
		);
		uv.drawFunc_({
			// draw outputs (left)
			outputs.do{|port, j|
				var name;
				name = ([port.name] ++ port.aliases).at(alias) ? port.name;
				Pen.line(Point(0,152 + (j*20)), Point(150,152 + (j*20)));
				Pen.stroke;
				name.drawRightJustIn(
					Rect(0,152 + (j*20), 150, 20)
				);
			};
			Pen.line(
				Point(0,152 + (outputs.size * 20)),
				Point(150,152 + (outputs.size * 20))
			);
			Pen.stroke;
			Pen.translate(155,145);
			inputs.do{|port|
				var name;
				name =  ([port.name] ++ port.aliases).at(alias) ? port.name;
				Pen.line(-3@8, -69@(-150));
				Pen.stroke;
				Pen.rotate(pi/2.7);
				name.drawRightJustIn(
					Rect(-150,-16, 157, 20)
				);
				Pen.rotate(-pi/2.7);
				Pen.translate(20,0);
			};
			Pen.line(-3@2, -69@(-150));
			Pen.stroke;
		});

		{grid.remove}.try;
		
		grid = BoxMatrix(
			sv,
			Rect(150,150, inputs.size * 20, outputs.size * 20),
			inputs.size,
			outputs.size
		);

		grid.defaultStyle.fontColor = Color.grey(0.3);
		grid.defaultStyle.boxColor = Color.grey(0.8);
		grid.defaultStyle.borderColor = Color.grey(0.3);
		grid.defaultStyle.center = true;

		// init everything disconnected (for the GUI)
		inputs.do{|i,j|
			outputs.do{|k,l|
				grid.at(j@l).connected = false;
				// (mousePos.x == j || mousePos.y == l).if({
				// 	grid.at(j@l).boxColor = Color.grey(0.9);
				// },{
					grid.at(j@l).boxColor = grid.defaultStyle.boxColor;
				// });
			};
		};

		inputs.do({ |port|
			var box, in, out;
			port.connections.do({ |connection|
				box = grid.at( // get the box
					Point(
						inputs.indexOf(port), // id of input
						outputs.indexOf( // id of output
							outputs.select({|p| p.name == connection})[0]
						)
					)
				); 
				box.connected = true;  // and change its attributes
				box.boxColor = Color.red; // and looks
			});
		});

		grid.mouseDownAction = { arg box,modifiers,buttonNumber,clickCount;
			var command;
			box.connected.if{
				command = prefix++"jack_disconnect" +
				outputs[box.point.y].name.shellQuote + inputs[box.point.x].name.shellQuote;
				command.postln;
				modifiers.isShift.not.if{
					command.systemCmd;
					this.initConnections;
					this.updateGui;
				};
			} {
				command = prefix++"jack_connect" +
				outputs[box.point.y].name.shellQuote + inputs[box.point.x].name.shellQuote;
				command.postln;
				modifiers.isShift.not.if{
					command.systemCmd;
					this.initConnections;
					this.updateGui;
				};
			}
		};

		grid.mouseOverAction_({ |box|
			mousePos = box.point;
			// grid.numRows.do({ |i|
			// 	grid.numCols.do({ |j|
			// 		(grid.at(i@j).connected == false).if({
			// 			// box.postln;
			// 			(i == box.point.x || j == box.point.y).if({
			// 				grid.at(i@j).boxColor_(Color(0.9,0.9,0.9));
			// 			},{
			// 				grid.at(i@j).boxColor_(Color(0.8,0.8,0.8));
			// 			});
			// 		});
			// 	});
			// });
		});
	}	

	initConnections {
		var stdOut;
		var inc;
		
		inc = 0;
		
		stdOut = (prefix++"jack_lsp -c").unixCmdGetStdOut.split($\n);

		ports.do({ |port|
			var cons;
			(port.name == stdOut[inc]).if({
				inc = inc + 1;
				{"^   ".matchRegexp(stdOut[inc])}.while({
					cons = cons ++ [ stdOut[inc][3..] ];
					inc = inc + 1;
				});
			});
			port.put(\connections, cons);
		});
	}

	initPorts {
		var stdOut, i;
		ports = List[];
		i = 0;
		stdOut = (prefix++"jack_lsp -Ap").unixCmdGetStdOut.split($\n);
		// stdOut = "jack_lsp -Ap | grep -v \"^Jack: \"".unixCmdGetStdOut.split($\n);
		// Maybe useful if Jack is verbose.
		{stdOut[i].notNil && stdOut[i] !=""}.while({
			var name, aliases, props;
			("^[a-zA-Z0-9]".matchRegexp(stdOut[i])).if({
				name = stdOut[i];
			}, {
				"Jack port names may be messed up.".warn;
			});
			i = i + 1;
			{"^   ".matchRegexp(stdOut[i])}.while({
				aliases = aliases ++ [ stdOut[i][3..] ];
				i = i + 1;
			});
			("^\t".matchRegexp(stdOut[i])).if({
				props = stdOut[i][1..];
			});
			ports = ports ++ [(name: name, aliases: aliases, props: props)];
			i=i+1;
		});


	}
	
}


