NearestDist {
	*kr { |point, otherPoints|
		var dist;
		dist = Dist.kr(point, otherPoints[0]);
		otherPoints.do({ |i,j|
			dist = Select.kr(Dist.kr(point, i) < dist,[
				dist,
				Dist.kr(point,i)
			]);
		});
		^dist;
	}
}

NearestDistBuf {
	// Special format for Buffer:
	// buf.numChannels = numPoints;
	// buf.numFrames = numDim (n);
	*kr { |buf, point, numPoints=2, n=2|
		var dist, res;
		res = [inf, inf];
		numPoints.do({|i|
			dist = (point - BufRd.kr(n, buf, i, 0, 0)).squared.sum;
			res[1] = Select.kr( dist < res[1], [ res[1], dist] );
			res = Select.kr( res[1] < res[0], [ res, [res[1], res[0]] ]);
		});
		// Returns the squared distance.
		^res;
	}
}

Dist {
	*kr { |point, otherPoint|
		^(point - otherPoint).squared.sum.sqrt;
	}

	*ar { |point, otherPoint|
		^(point - otherPoint).squared.sum.sqrt;
	}

}

InterpolatorWeight {
	*kr{ |distSquared, radiusSquared, cursorRadiusSquared|
		var x, y, z, w, dist, radius, cursorRadius;
		dist = distSquared.sqrt;
		radius = radiusSquared.sqrt;
		cursorRadius = cursorRadiusSquared.sqrt;
		

		w = ((distSquared + radiusSquared - cursorRadiusSquared) /(2 * dist * radius));
		x = radiusSquared * acos( w );
		y = cursorRadiusSquared * acos(
			((distSquared + cursorRadiusSquared - radiusSquared) /
			(2 * dist * cursorRadius));
		);
		z = 0.5 * (
			((dist * -1) + radius + cursorRadius) *
			(dist + radius - cursorRadius) *
			(dist - radius + cursorRadius) *
			(dist + radius + cursorRadius)
		).sqrt;
		
		

		^Select.kr(
			((radius + cursorRadius) < dist),
			[ ((x + y - z)/(pi * radiusSquared)),	0 ]
		);

	}
}

// InterpolatorWeight {
// 	*kr{ |point, radius, cursor, cursorRadius|
// 		var weight;
// 		weight = IntersectArea.kr(point, radius, cursor, cursorRadius) /
// 		(pi * radius.squared);
// 		^RemoveBadValues.kr(weight);
// 	}
// }