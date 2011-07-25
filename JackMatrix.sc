JackMatrix {
	var <>prefix, <grid, <inputs, <outputs, <connections, w, sv, uv;

	*new{ |prefix|
		^super.newCopyArgs(prefix).init;
	}
	
	init {
		var pipe, line1, line2;
		inputs = List[];
		outputs = List[];
		connections = List[];
		pipe = Pipe.new(prefix++"jack_lsp -p", "r");
		line1 = pipe.getLine;							
		{line1.notNil}.while {
			line2 = pipe.getLine;
			line2.notNil.if {
				line2.contains("input").if {
					inputs.add(line1)
				};
				line2.contains("output").if {
					outputs.add(line1)
				};
			};
			line1 = line2;
		};
		pipe.close;
		
		inputs.sort({|a,b|
			a.asString.naturalCompare(b.asString) < 0;
		});

		outputs.sort({|a,b|
			a.asString.naturalCompare(b.asString) < 0;
		});

		pipe = Pipe.new(prefix++"jack_lsp -c", "r");
		line1 = pipe.getLine;							
		{line1.notNil}.while {
			line2 = pipe.getLine;
			line2.notNil.if {
				line2.containsStringAt(0, "   ").if {
					connections.add([line1, line2.copyToEnd(3)])
				};
			};
			line1 = line2;
		};
		pipe.close;

		w = Window("JackMatrix",Rect(100,100,600,600));
		sv = ScrollView(w, Rect(0,0,600,600));
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
			Pen.line(Point(0,152 + (outputs.size * 20)), Point(150,152 + (outputs.size * 20)));
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

		inputs.do{|i,j|
			outputs.do{|k,l|
				grid.at(j@l).connected = false;
			};
		};

		connections.do{|i|
			var in, out, box;
			out = outputs.indexOfEqual(i[0]);
			out.notNil.if {
				in = inputs.indexOfEqual(i[1]);
				box = grid.at(in@out);
				box.connected = true;
				box.boxColor = Color.red;
			}
		};
		
		grid.mouseDownAction = { arg box,modifiers,buttonNumber,clickCount;
			var command;
			box.connected.if{
				command = prefix++"jack_disconnect" +
				  outputs[box.point.y] + inputs[box.point.x];
				command.postln;
				modifiers.isShift.not.if{
					command.unixCmd;
					box.connected = false;
					box.boxColor = grid.defaultStyle.boxColor;
				};
			} {
				command = prefix++"jack_connect" +
				  outputs[box.point.y] + inputs[box.point.x];
				command.postln;
				modifiers.isShift.not.if{
					command.unixCmd;
					box.connected = true;
					box.boxColor = Color.red;
				};
			}
		};
		w.front;
	}
	
}


