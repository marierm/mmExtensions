// make a 2D Preset Interpolator.
y = PresetInterpolator();
y.gui(nil, Rect(200,200, 800,200));// with nice gui.

// more dimensions
y = PresetInterpolator(Interpolator(8));
y.gui(nil, Rect(200,200, 800,200));// with nice gui.


// create the sponge obect and connect it to the PresetInterpolator.
a = SpongeBee("/dev/tty.usbserial-A800ekzP");
a.connect(z)
a.close;

//Save and load
y.save(Document.current.dir ++ "/presetInterpolatorPatch.pri");
z = PresetInterpolator.load(Document.current.dir ++ "/presetInterpolatorPatch.pri");
z.gui(nil, Rect(200,200, 800,200));// with nice gui.


// test osc.
(
n = NetAddr.localAddr;
o = OSCresponderNode(n, '/Parameter', { |t, r, msg| ("time:" + t).postln; msg[1].postln }).add;
)