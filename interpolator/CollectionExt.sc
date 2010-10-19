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
		var x, y, z;
		x = rad.squared * acos(
				(this.dist(coll).squared + rad.squared - collRad.squared) /
				(2 * this.dist(coll) * rad)
			);
		y = collRad.squared * acos(
				(this.dist(coll).squared + collRad.squared - rad.squared) /
				(2 * this.dist(coll) * collRad)
			);
		z = 0.5 * (
			((this.dist(coll) * -1) + rad + collRad) *
			(this.dist(coll) + rad - collRad) *
			(this.dist(coll) - rad + collRad) *
			(this.dist(coll) + rad + collRad)
		).sqrt;
		^(x + y - z);
	}

}
