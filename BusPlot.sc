BusPlot {
	var bus, name, <>plotSize, <>refreshInterval, 
	<plot, vals, getThread, showThread;

	*new { |bus, name, plotSize=500, refreshInterval=0.05|
		^super.newCopyArgs(bus, name, plotSize, refreshInterval).init;
	}

	init {
		plot = Plotter(
			name.asString,
			Rect(600, 30, 500, 400)
		);
		vals = Array.fill(plotSize,0);
		getThread = {
			{
				bus.get({|v|
					vals = vals.insert( 0, v );
					vals = vals.keep(plotSize);
				});
				// Update array at every block size;
				(bus.server.options.blockSize/bus.server.sampleRate).wait;
			}.loop
		}.fork;
		showThread = {
			{plot.value = vals;refreshInterval.wait}.loop;
		}.fork(AppClock);
		plot.parent.onClose_({
			showThread.stop;
			getThread.stop;
		});
	}

}