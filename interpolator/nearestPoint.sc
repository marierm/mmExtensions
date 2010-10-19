// Copyright 2010 Martin Marier

+ Point {
	findNearest { |listOfPoints|
		var d, p;
		d = this.dist(listOfPoints[0]);
		p = 0;
		listOfPoints.do{ |i,j|
			if (this.dist(i) < d) {
				d = this.dist(i);
				p = j;
			}
		}
		^p;
	}
	
	findNearestDist { |listOfPoints|
		var d;
		d = this.dist(listOfPoints[0]);
		listOfPoints.do{ |i,j|
			if (this.dist(i) < d) {
				d = this.dist(i);
			}
		}
		^d;
	}
	
	asCircle {
		^Circle(this, 0);
	}
}