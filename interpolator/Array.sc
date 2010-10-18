+Array
{
	expandTree {|base|
		var result;
		if(base.isString){
			base = List[base];
			}{
			base = base ? List[];
			};
		base.add("");
		this.do{|i,j|
			if(i.isArray and: i.isString.not){ // item is an Array
				result = result ++ i.expandTree(base);
				base.pop;
			}{	//item is a String
				if(this[j+1].isArray and: this[j+1].isString.not){
					//next item is an Array.
					base.add(i);
				}{	//next item is not an Array.
					result = result.add(base.inject("",_++_)++i);
					if(this[j+1].isNil){
						base.pop;
					}
				};
			};
		};
		^result;
	}
}