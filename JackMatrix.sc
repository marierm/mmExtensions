// Copyright 2011 Martin Marier
// JackMatrix is a matrix GUI to connect Jack clients.
// It depends on BoxMatrix from the crucialviews Quark.

// Arguments:

// prefix: a prefix to the jack_* command line utilities.  Using ssh, it is
// possible to control Jack connections on a remote machine.  Example:
// JackMatrix("ssh mm@192.168.0.102 /usr/bin/")

// autoUpdate: a time interval in seconds.  The GUI will be update at this
// rate.  A value of 0 or less will turn off GUI update.  Very small values
// will probably hang sclang.

JackMatrix {
	var <>prefix, <autoUpdate,
	<grid, <inputs, <outputs, <connections, w, sv, uv, routine;

	*new{ |prefix, autoUpdate=2.0|
		^super.newCopyArgs(prefix, autoUpdate).init;
	}

	init {
		inputs = List[];
		outputs = List[];
		connections = List[];

		this.getNames;
		this.getConnections;

		w = Window("JackMatrix",Rect(100,100,600,600));
		sv = ScrollView(w, Rect(0,0,600,600));

		this.updateGui;
		
		w.onClose_({routine.stop});
		w.front;

		routine = Routine({
			{
				this.getNames;
				this.getConnections;
				this.updateGui;
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
		{uv.remove}.try;
		uv = UserView(
			sv,
			Rect(0,0,(inputs.size * 20) + 200, (outputs.size * 20) + 200)
		);
		uv.drawFunc_({
			outputs.do{|i, j|
				Pen.line(Point(0,152 + (j*20)), Point(150,152 + (j*20)));
				Pen.stroke;
				i.drawRightJustIn(
					Rect(0,152 + (j*20), 150, 20)
				);
			};
			Pen.line(
				Point(0,152 + (outputs.size * 20)),
				Point(150,152 + (outputs.size * 20))
			);
			Pen.stroke;
			Pen.translate(155,145);
			inputs.do{|i|
				Pen.line(-1@5, 65@(-150));
				Pen.stroke;
				Pen.rotate(-pi/2.7);
				i.draw();
				Pen.rotate(pi/2.7);
				Pen.translate(20,0);
			};
			Pen.line(-1@5, 65@(-150));
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
				grid.at(j@l).boxColor = grid.defaultStyle.boxColor
			};
		};

		connections.do{|i|
			// connections is a List of size 2 arrays [output, input];
			var in, out, box;
			out = outputs.indexOfEqual(i[0]); // get the index of the output
			out.notNil.if {
				in = inputs.indexOfEqual(i[1]); // get the index of the input
				box = grid.at(in@out); // get the box
				box.connected = true;  // and change its attributes
				box.boxColor = Color.red; // and looks
			}
		};

		grid.mouseDownAction = { arg box,modifiers,buttonNumber,clickCount;
			var command;
			box.connected.if{
				command = prefix++"jack_disconnect" +
				outputs[box.point.y] + inputs[box.point.x];
				command.postln;
				modifiers.isShift.not.if{
					command.systemCmd;
					this.getConnections;
					this.updateGui;
				};
			} {
				command = prefix++"jack_connect" +
				outputs[box.point.y] + inputs[box.point.x];
				command.postln;
				modifiers.isShift.not.if{
					command.systemCmd;
					this.getConnections;
					this.updateGui;
				};
			}
		};
	}	

	getConnections {
		var stdOut, cons;
		var inc;
		
		cons = List[];
		stdOut = (prefix++"jack_lsp -c").unixCmdGetStdOut.split($\n);
		stdOut.do { |i,j|
			i.containsStringAt(0, "   ").if{
				inc = 1;
				{stdOut[j - inc].containsStringAt(0, "   ")}.while({ inc = inc+1});
				cons.add([stdOut[j - inc], i.copyToEnd(3)])
			}
		};
		connections = cons;
	}

	getNames {
		var stdOut, ins, outs;
		ins = List[];
		outs = List[];
		stdOut = (prefix++"jack_lsp -p").unixCmdGetStdOut.split($\n);
		stdOut.do { |i,j|
			i.contains("input").if {
				ins.add(stdOut[j-1])
			};
			i.contains("output").if {
				outs.add(stdOut[j-1])
			};
		};
		outputs = outs;
		inputs = ins;

		// sort things naturally
		inputs.sort({|a,b|
			a.asString.naturalCompare(b.asString) < 0;
		});

		outputs.sort({|a,b|
			a.asString.naturalCompare(b.asString) < 0;
		});
	}
	
}


