+ QWindow {
	asPageLayout { arg title, bounds;
		^MultiPageLayout.on( this.asView, bounds );
	}
}

+ QView {
	asPageLayout { arg title, bounds;
		// though it won't go multi page
		// FlowView better ?
		^MultiPageLayout.on( this, bounds );
	}
}