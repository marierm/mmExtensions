JackMatrix {
	var <>prefix, <grid, <inputs, <outputs, <connections;

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

		grid = BoxMatrix(
			nil,
			((inputs.size + 2) * 35)@((outputs.size + 2) * 15),
			inputs.size + 2,
			outputs.size + 2
		);

		grid.defaultStyle.fontColor = Color.grey(0.3);
		grid.defaultStyle.boxColor = Color.grey(0.8);
		grid.defaultStyle.borderColor = Color.grey(0.3);
		grid.defaultStyle.center = true;

		inputs.do{|i,j|
			grid.at((j+2)@0).title_(i.asString.split($:)[0]);
			grid.at((j+2)@1).title_(
				i.split($:)[1].findRegexp("[0-9]+$")[0][1]
			);
		};
		outputs.do{|i,j|
			grid.at(0@(j+2)).title_(i.asString.split($:)[0]);
			grid.at(1@(j+2)).title_(
				i.split($:)[1].findRegexp("[0-9]+$")[0][1]
			);
		};
		
		inputs.do{|i,j|
			outputs.do{|k,l|
				grid.at((l+2)@(j+2)).connected = false;
			};
		};

		connections.do{|i|
			var in, out, box;
			out = outputs.indexOfEqual(i[0]);
			out.notNil.if {
				in = inputs.indexOfEqual(i[1]);
				box = grid.at((in + 2)@(out + 2));
				box.connected = true;
				box.boxColor = Color.red;
			}
		};
		
		grid.mouseDownAction = { arg box,modifiers,buttonNumber,clickCount;
			var command;
			box.connected.if{
				command = prefix++"jack_disconnect" +
				  outputs[box.point.y - 2] + inputs[box.point.x - 2];
				command.postln;
				modifiers.isShift.not.if{
					command.unixCmd;
					box.connected = false;
					box.boxColor = grid.defaultStyle.boxColor;
				};
			} {
				command = prefix++"jack_connect" +
				  outputs[box.point.y - 2] + inputs[box.point.x - 2];
				command.postln;
				modifiers.isShift.not.if{
					command.unixCmd;
					box.connected = true;
					box.boxColor = Color.red;
				};
			}
		};
	}
	
}


