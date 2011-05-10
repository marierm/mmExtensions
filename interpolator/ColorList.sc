ColorList : List {
	*new { |firstColor|
		^super.new.init(firstColor);
	}

	*get { |i|
		^Color.hsv(0,0.5,0.7,1).hue_(
			Color.hsv(0,0.5,0.7,1).hue + (0.094117647058824 * i) % 1
		);
	}

	init { |fc|
		fc = fc ? Color.hsv(0,0.5,0.7,1);
		this.add(fc);
	}

	addNext {
		this.add(
			this.last.copy.hue_( this.last.hue + 0.094117647058824 % 1 );
		);
	}
}
