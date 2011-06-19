//List serial devices
SerialPort.listDevices;

// create the sponge obect and connect it to the PresetInterpolator.
a = Sponge("/dev/tty.usbserial-A800ekzP");
// modify OSC parameters:
a.setOSCport(8000);
a.setOSCaddr("localhost");
a.setOSCprefix("/new/prefix/01");
// close the port
a.close;


//reopen
a = Sponge("/dev/tty.usbserial-A800ekzP");

// get a list of the current features of the sponge
a.featureNames; // only the raw sensors are there

// get a list of the available features
Sponge.featureList.do{|i| i[\name].postln}

// add one of the available features
a.createFeature(\fsr1LP);
a.featureNames; // it's there!

// now send OSC to OSCulator
a.setOSCport(8000); // OSCulator port has to be 8000, of course.


a.createFeature(\fsr1Speed);

Function
a.action.array[9].def.code
a.features.last.netAddr
//*****************************************************************
//*****************************************************************
//*****************************************************************

// make a 2D Preset Interpolator.
y = PresetInterpolator();
y.gui(nil, Rect(200,200, 800,200));// with nice gui.

// more dimensions
y = PresetInterpolator(Interpolator(8));
y.gui(nil, Rect(200,200, 800,200));// with nice gui.


//list the serial devices
SerialPort.listDevices;

// create the sponge obect and connect it to the PresetInterpolator.
a = Sponge("/dev/tty.usbserial-A800ekzP");
a.action_({|...data| data.postln;})
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

