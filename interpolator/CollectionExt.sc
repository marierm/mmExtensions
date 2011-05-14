// Copyright 2010 Martin Marier

+ Collection {
	// for this to make any sense, Collections have to be coordinates.
	dist { |coll|
		^(this - coll).squared.sum.sqrt;
	}
	
	// this.nearest(coll): <this> should be 2D: a list of coordinates.
	// And coll should be 1D (1 coordinate);
	// returns nearest point index in coll.
	nearest { |coll|
		var dist, id;
		dist = this.dist(coll[0]);
		id = 0;
		coll.do{ |i,j|
			if (this.dist(i) < dist) {
				dist = this.dist(i);
				id = j;
			}
		}
		^id;
	}

	// returns distance to nearest point in coll
	nearestDist { |coll|
		var dist, id;
		dist = this.dist(coll[0]);
		id = 0;
		coll.do{ |i,j|
			if (this.dist(i) < dist) {
				dist = this.dist(i);
				id = j;
			}
		}
		^dist;
	}

	// This will find the intersecting area of two intersecting circles.
	// Here, I (can) use the same formula on n-spheres... I am not sure it makes
	// any sense...
	intersectArea { |rad, coll, collRad|
		var x, y, z, w, dist;
		dist = this.dist(coll);
		w = ((dist.squared + rad.squared - collRad.squared) /(2 * dist * rad));
		if ( (w > 1) || (w < -1) ){"WARNING: a NaN was caught.".postln;};
		x = rad.squared * acos( w );
		y = collRad.squared * acos(
			(dist.squared + collRad.squared - rad.squared) /
			(2 * dist * collRad)
		);
		z = 0.5 * (
			((dist * -1) + rad + collRad) *
			(dist + rad - collRad) *
			(dist - rad + collRad) *
			(dist + rad + collRad)
		).sqrt;
		^(x + y - z);
	}

}
