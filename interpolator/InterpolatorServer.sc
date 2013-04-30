// Copyright 2011-2013 Martin Marier
// Depends on MCLD plugins (NearestN UGen) and KDTree Quark.

// Or not...  Found a bug in NearestN and, in the mean time, using a simple
// iterative algo to find the nearest point(s).

InterpolatorServer {

	var <n, <server, <group, <points, <pointsBuf, <pointsSynthGrp;
	var <cursorBus, <cursorRadiusBus, <cursorSynth, <cursor;
	var <weightsSynth, <weightsBus, <weights;
	var <>colors, <attachedPoint, connections;
	var <updateTask, <moveAction;
	
	*new{ |numDim = 2, server|
		server = server ? Server.default;
		^super.newCopyArgs(numDim, server).init;
	}
	
	init{
		connections = Array.newClear(n);
		colors = List.new;
		points = List.new;
		weights = List.new;

		moveAction = {
			cursorBus.getn(n, {|v| cursor = v});
			weightsBus.getn(weights.size, {|v|
				weights.putEach((0..weights.size-1), v)
			});
			{this.changed(\weights, (0..(points.size - 1)), weights)}.defer;
		};

		{
			server.bootSync;
			group = Group(server);
			cursorBus = Bus.control(server, n);
			cursorRadiusBus = Bus.control(server, 1);
			weightsBus = Bus.control(server, 1); // avoid error; this will be
												// changed very soon.
			pointsSynthGrp = ParGroup(group);
			this.addTwoPoints;

			server.sync;
			cursorBus.setn([0.5,0.5].extend(n,0));
			this.buildKDTree;
			server.sync;
			updateTask = { 
				loop{
					cursorBus.getn(n, {|v| cursor = v});
					weightsBus.getn(weights.size, {|v|
						weights.putEach((0..weights.size-1), v)
					});
					0.05.wait;
					try{this.changed(\weights, (0..(points.size - 1)), weights)};
				}
			}.fork(AppClock);
		}.fork;

	}

	// Because of a bug in NearestN, not a KDTree for the moment.
	buildKDTree {
		{
			server.sync;
			pointsBuf.free;
			// Format for Buffer:
			// buf.numChannels = numDim (n);
			// buf.numFrames = numPoints;
			pointsBuf = Buffer.sendCollection(
				server, points.flat, n
			);
			this.addSynthDefs;
			// Rebuild the KDTree and fill the buffer.
			// pointsBuf = Buffer.sendCollection(
			// 	server,
			// 	NearestN.makeBufferData( KDTree(
			// 		points.collect({|point, i| point ++ i}),
			// 		lastIsLabel:true
			// 	)).flat,
			// 	n + 3
			// );
			// Create a Weights SynthDef with the right number of points.
			// free everything that will be replaced;
			cursorSynth.free;
			weightsSynth.free;
			weightsBus.free;
			pointsSynthGrp.freeAll;

			server.sync; // Wait for server to be done.

			weightsBus = Bus.control(server, points.size); // with the new
														   // number of
														   // points.
			points.do({|i,j| // Create all the point synths.
				Synth.tail(
					pointsSynthGrp,
					"interpolatorPoint" ++ n ++ "_" ++ points.size, [
						\out, weightsBus.subBus(j),
						\buf, pointsBuf,
						\cursorBus, cursorBus,
						\cursorRadiusBus, cursorRadiusBus,
						\point, i
					]
				)
			});
			// server.sync;
			weightsSynth = Synth.after(
				pointsSynthGrp,
				"interpolatorWeights" ++ points.size,
				[ \in, weightsBus ]
			);
			// server.sync;
			cursorSynth = Synth.before(
				pointsSynthGrp, 
				"interpolatorCursor" ++ n ++ "_" ++ points.size, [
					\out, cursorRadiusBus,
					\buf, pointsBuf,
					\cursorBus, cursorBus
				]
			);
			server.sync;
			// send message to PresetInterpolatorServer so that it knows it
			// has to update its synth as well.
			this.changed(\kdtreeRebuilt)
		}.forkIfNeeded;
	}

	addTwoPoints {
		colors.add(Color.getPresetColor(0));
		colors.add(Color.getNextPresetColor(colors.last));
		points.add( 0!n );
		points.add( 1!n );
		weights.addAll([0.0,0.0])
	}
	
	add { |... newPoints|
		// add something if no point is given.
		(newPoints.size == 0).if({
			var np;
			// Summing the last two points to create a new one keeps things in
			// proportion.
			np = points.last - points.wrapAt(-2) + points.last;
			{points.indexOfEqual(np).notNil}.while({
				np = np + 1;
			});
			newPoints = [np];
		});

		newPoints.do({ |i,j|
			(i.size != n).if({
				i = i.extend(n, 0);
				"Point must be an array of size %".format(n).warn;
				"Point % was added.".format(i).warn;
			});
			points.indexOfEqual(i).isNil.if({
				colors.add(Color.getNextPresetColor(colors.last));
				points.add( i );
				weights.add( 0.0 );
				this.changed(\pointAdded, i);
			},{
				"There is already a point at %".format(i).warn;
				"Point % was not added.".format(i).warn;
			});
		});
		this.buildKDTree;
	}


	addSynthDefs{
		SynthDef("interpolatorCursor" ++ n ++ "_" ++ points.size,{
			arg out = 0, buf, cursorBus=0;
			var cursorRadiusSquared;
			// Cursor coordinates are on a Bus (numChannels = ~numDim);
			# cursorRadiusSquared = NearestDistBuf.kr(
				buf,
				In.kr(cursorBus, n),
				points.size,
				n
			);
			ReplaceOut.kr(out, cursorRadiusSquared )
		}).add;
		SynthDef("interpolatorPoint" ++ n ++ "_" ++ points.size,{
			arg out=0, buf, cursorBus=0, cursorRadiusBus=0;
			var radiusSquared, arr, distSquared, cursor, weight, point;

			cursor = In.kr(cursorBus, n );
			point = \point.kr( 0! n );
			// arr is [nearestDistSquared, SecondNearestDistSquared].
			arr = NearestDistBuf.kr(buf, point, points.size, n);
			distSquared = (point - cursor).squared.sum;
			radiusSquared = distSquared.min(arr[1]);

			weight = InterpolatorWeight.kr(
				distSquared, radiusSquared, In.kr(cursorRadiusBus, 1)
			);

			weight = Select.kr(
				BinaryOpUGen('==', distSquared, 0),
				[weight, 1.0]
			);

			ReplaceOut.kr(out, RemoveBadValues.kr(weight));
		}).add;
		SynthDef("interpolatorWeights" ++ points.size,{
			arg in=0;
			ReplaceOut.kr(in, In.kr(in, points.size).normalizeSum );
		}).add;
	}
	
	duplicatePoint { |point, pointId|
		(pointId == \cursor).if {
			this.changed(\cursorDuplicated, point, pointId);
		} {
			this.changed(\pointDuplicated, point, pointId);
		};
	}

	remove { |i|
		(points.size > 2).if ({
			colors.removeAt(i);
			points.removeAt(i);
			weights.removeAt(i);
			this.changed(\pointRemoved, i);
			this.buildKDTree;
		},{
			"The minimum number of points is 2.".warn;
		})
	}

	cursor_ { |pos|
		(pos.size != n).if({
			pos = cursor.putEach((0..pos.size-1), pos);
		});
		cursor = pos;
		cursorBus.setn(pos);
		this.changed(\cursorMoved);	
		attachedPoint.notNil.if {
			// Change this to drag?
			{ this.movePoint(attachedPoint, pos); }.defer;
		};
	}
	
	// change only one coordinate
	changeCoord { |i, j, val|
		var newPos;
		newPos = points[i];
		newPos[j] = val;
		this.movePoint(i, newPos);
		// this.dragPoint(i, newPos);
	}

	dragPoint { |i, pos|
		// Does not build the KDTree.
		(pos.size != n).if({
			pos = points[i].putEach((0..pos.size-1), pos);
		});
		points[i] = pos;
		this.changed(\pointMoved, i, points[i]);
	}

	dropPoint { |i, oldPos|
		// if two points are on the same spot:
		( points.indicesOfEqual(points[i]).size != 1 ).if({
			oldPos = oldPos ? points[i] * 1.01;
			points[i] = oldPos;
			"There cannot be two points at the same position".warn;
			"Point % was dropped at %.".format(i, points[i]).warn;
		});
		this.changed(\pointMoved, i, points[i]);
		this.buildKDTree;
	}

	// Need to keep this for compatibility.  To deprecate eventually.
	movePoint { |i, pos|
		var oldPos;
		oldPos = points[i];
		this.dragPoint(i, pos);
		this.dropPoint(i, oldPos);
	}
	// when a point is double clicked in the 2DGui, this is called.  When an
	// Interpolator is used inside a preset interpolator, the preset's gui is
	// opened.
	makePointGui { |grabbedPoint|
		(grabbedPoint == -1).if {
			this.changed(\makeCursorGui);
		} {
			this.changed(\makePointGui, grabbedPoint, colors[grabbedPoint]);
		}
	}

	// refreshRads{
	// 	cursorRad = (cursor.nearestDist(points));
	// 	points.do { |i,j|
	// 		var a;
	// 		a = points.deepCopy;
	// 		a.removeAt(j);
	// 		a = a ++ [cursor];
	// 		rads[j] = i.nearestDist(a);
	// 	};
	// 	// this.changed(\weights, weights);
	// }

	// To control the position of the cursor using an interface (the sponge).
	// A Feature is connected to one axis of the interpolator.
	connect { |axis, feature|
		connections[axis] = InterpolatorConnection(this, axis, feature);
	}

	disconnect { |axis|
		connections[axis].disconnect;
	}


	attachedPoint_ { |point|
		attachedPoint = point;
		this.changed(\attachedPoint, point);
	}

	free {

		[ cursorBus, cursorRadiusBus, cursorSynth, pointsSynthGrp, pointsBuf,
			weightsSynth, weightsBus, group ].do(_.free);
		connections.do(_.free);
		updateTask.stop;

	}

	// gui stuff
	guiClass { ^InterpolatorServerGui }

	gui2D { arg  ... args;
		^InterpolatorServer2DGui.new(this).performList(
			\gui,args
		);
	}

}


InterpolatorConnection {
	var interpolator, axis, feature, server, synth, n;
	
	*new { |interpolator, axis, feature|
		^super.newCopyArgs(interpolator, axis, feature).init;
	}

	init {
		server = interpolator.server;
		n = feature.bus.numChannels;
		{
			server.sync;
			SynthDef("interpolatorConnection" ++ n, {
				arg in=0, out=0;
				Out.kr(out, In.kr(in, n));
			}).add;
			server.sync;
			synth = Synth.before(
				interpolator.cursorSynth,
				"interpolatorConnection" ++ n,
				[
					\in, feature.bus,
					\out, interpolator.cursorBus 
				]
			);
		}.fork;
	}

	disconnect {
		synth.free;
	}
}