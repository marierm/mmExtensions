// Copyright 2010 Martin Marier

Circle {
	var <>pos, <>rad;
	
	*new { |pos, rad|
		^super.new.init(pos, rad);
	}
	
	init { |p, r|
		if (p.isKindOf(Point)) {
			pos = p;
		} {
			pos = 0@0;
		};
		rad = r ? 0;
	}
	
	intersects { |circle|
		^(this.dist(circle) <= (rad + circle.rad))
	}
	
	dist { |circle|
		^pos.dist(circle.pos)
	}
	
	findNearest { |list|
		^pos.findNearest(list.collect(_.pos));
	}
	
	findNearestDist { |list|
		^pos.findNearestDist(list.collect(_.pos));
	}
	
	//find the area in which two circles intersect
	//formula came from here : http://mathworld.wolfram.com/Circle-CircleIntersection.html
	intersectArea { |circle|
		var x, y, z;
		x = rad.squared * acos(
				(this.dist(circle).squared + rad.squared - circle.rad.squared) /
				(2 * this.dist(circle) * rad)
			);
		y = circle.rad.squared * acos(
				(this.dist(circle).squared + circle.rad.squared - rad.squared) /
				(2 * this.dist(circle) * circle.rad)
			);
		z = 0.5 * (
			((this.dist(circle) * -1) + rad + circle.rad) *
			(this.dist(circle) + rad - circle.rad) *
			(this.dist(circle) - rad + circle.rad) *
			(this.dist(circle) + rad + circle.rad)
		).sqrt;
		^(x + y - z);
	}

	radiusRatio { |circle|
		var result;
		
		result =(((
			(
				(4 * this.dist(circle).squared * circle.rad.squared) -
				(
					this.dist(circle).squared -
					rad.squared +
					circle.rad.squared
				).squared;
			) / 4 * this.dist(circle).squared
		).sqrt ));
		"% / %".format(result, rad).postln;
		result = result/rad;
		^result
	}
	
	area {
		^(pi * rad.squared);
	}
	
	areaRatio { |circle|
		^(this.intersectArea(circle) / this.area)
	}
	
	* { |circle|
		^pos * circle.pos;
	}
	
	== { |circle|
		^(pos == circle.pos && rad == circle.rad)
	}
}
