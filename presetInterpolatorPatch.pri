var o, p;
o = [
	Event.prNew,  Array.prNew(20),  List.prNew,  Array.prNew(3),  
	Array.prNew(8),  Array.prNew(8),  Array.prNew(8),  List.prNew,  
	Array.prNew(3),  Color.prNew,  Color.prNew,  Color.prNew,  
	Array.prNew(8),  Array.prNew(3),  Event.prNew,  Array.prNew(8),  
	"Preset",  List.prNew,  Array.prNew(5),  Parameter.prNew,  
	"1",  ControlSpec.prNew,  LinearWarp.prNew,  "",  
	List.prNew,  Array.prNew(3),  Parameter.prNew,  List.prNew,  
	Array.prNew(3),  Parameter.prNew,  List.prNew,  Array.prNew(3),  
	Parameter.prNew,  List.prNew,  Array.prNew(3),  NetAddr.prNew,  
	"127.0.0.1",  "/Parameter",  NetAddr.prNew,  "/Parameter",  
	NetAddr.prNew,  "/Parameter",  NetAddr.prNew,  "/Parameter",  
	Parameter.prNew,  "2",  ControlSpec.prNew,  LinearWarp.prNew,  
	List.prNew,  Array.prNew(3),  Parameter.prNew,  List.prNew,  
	Array.prNew(3),  Parameter.prNew,  List.prNew,  Array.prNew(3),  
	Parameter.prNew,  List.prNew,  Array.prNew(3),  NetAddr.prNew,  
	"/Parameter",  NetAddr.prNew,  "/Parameter",  NetAddr.prNew,  
	"/Parameter",  NetAddr.prNew,  "/Parameter",  Parameter.prNew,  
	"3",  ControlSpec.prNew,  LinearWarp.prNew,  List.prNew,  
	Array.prNew(3),  Parameter.prNew,  List.prNew,  Array.prNew(3),  
	Parameter.prNew,  List.prNew,  Array.prNew(3),  Parameter.prNew,  
	List.prNew,  Array.prNew(3),  NetAddr.prNew,  "/Parameter",  
	NetAddr.prNew,  "/Parameter",  NetAddr.prNew,  "/Parameter",  
	NetAddr.prNew,  "/Parameter",  Parameter.prNew,  "4",  
	ControlSpec.prNew,  LinearWarp.prNew,  List.prNew,  Array.prNew(3),  
	Parameter.prNew,  List.prNew,  Array.prNew(3),  Parameter.prNew,  
	List.prNew,  Array.prNew(3),  Parameter.prNew,  List.prNew,  
	Array.prNew(3),  NetAddr.prNew,  "/Parameter",  NetAddr.prNew,  
	"/Parameter",  NetAddr.prNew,  "/Parameter",  NetAddr.prNew,  
	"/Parameter",  Parameter.prNew,  "5",  ControlSpec.prNew,  
	LinearWarp.prNew,  List.prNew,  Array.prNew(3),  Parameter.prNew,  
	List.prNew,  Array.prNew(3),  Parameter.prNew,  List.prNew,  
	Array.prNew(3),  Parameter.prNew,  List.prNew,  Array.prNew(3),  
	NetAddr.prNew,  "/Parameter",  NetAddr.prNew,  "/Parameter",  
	NetAddr.prNew,  "/Parameter",  NetAddr.prNew,  "/Parameter",  
	Event.prNew,  Array.prNew(8),  List.prNew,  Array.prNew(5),  
	Event.prNew,  Array.prNew(8),  List.prNew,  Array.prNew(5),  
	Event.prNew,  Array.prNew(8),  "cursor",  List.prNew,  
	Array.prNew(5)
];
p = [
	// Event
	0, [ array: o[1],  size: 5,  
		proto: nil,  parent: nil,  
		know: true ],  
	// Array
	1, [ 'points',  o[2],  nil,  nil,  
		'colors',  o[7],  'cursorPos',  o[12],  
		nil,  nil,  nil,  nil,  
		'presets',  o[13],  nil,  nil,  
		'cursor',  o[144],  nil,  nil ],  
	// List
	2, [ array: o[3] ],  
	// Array
	3, [ o[4],  o[5],  o[6] ],  
	// Array
	4, [ 0.146907,  0.243557,  0.500000,  0.500000,  
		0.500000,  0.500000,  0.500000,  0.500000 ],  
	// Array
	5, [ 0.336340,  0.371134,  0,  0,  
		0,  0,  0,  0 ],  
	// Array
	6, [ 0.369845,  0.092784,  0,  0,  
		0,  0,  0,  0 ],  
	// List
	7, [ array: o[8] ],  
	// Array
	8, [ o[9],  o[10],  o[11] ],  
	// Color
	9, [ red: 0.700000,  green: 0.350000,  
		blue: 0.350000,  alpha: 1 ],  
	// Color
	10, [ red: 0.700000,  green: 0.547647,  
		blue: 0.350000,  alpha: 1 ],  
	// Color
	11, [ red: 0.654706,  green: 0.700000,  
		blue: 0.350000,  alpha: 1 ],  
	// Array
	12, [ 0.360825,  0.186856,  0.500000,  0.500000,  
		0.500000,  0.500000,  0.500000,  0.500000 ],  
	// Array
	13, [ o[14],  o[136],  o[140] ],  
	// Event
	14, [ array: o[15],  size: 2,  
		proto: nil,  parent: nil,  
		know: true ],  
	// Array
	15, [ 'name',  o[16],  'parameters',  o[17],  
		nil,  nil,  nil,  nil ],  
	// List
	17, [ array: o[18] ],  
	// Array
	18, [ o[19],  o[44],  o[67],  o[90],  
		o[113] ],  
	// Parameter
	19, [ name: o[20],  value: 0.000000,  
		spec: o[21],  action: nil,  
		siblings: o[24],  sendOSC: false,  
		netAddr: o[42],  oscMess: o[43],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// ControlSpec
	21, [ minval: 0.000000,  maxval: 1.000000,  
		warp: o[22],  step: 0.000000,  
		default: 0.000000,  units: o[23],  
		clipLo: 0.000000,  clipHi: 1.000000 ],  
	// LinearWarp
	22, [ spec: o[21] ],  
	// List
	24, [ array: o[25] ],  
	// Array
	25, [ o[26],  o[29],  o[32] ],  
	// Parameter
	26, [ name: o[20],  value: 0.000000,  
		spec: o[21],  action: nil,  
		siblings: o[27],  sendOSC: false,  
		netAddr: o[40],  oscMess: o[41],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	27, [ array: o[28] ],  
	// Array
	28, [ o[19],  o[29],  o[32] ],  
	// Parameter
	29, [ name: o[20],  value: 0.000000,  
		spec: o[21],  action: nil,  
		siblings: o[30],  sendOSC: false,  
		netAddr: o[38],  oscMess: o[39],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	30, [ array: o[31] ],  
	// Array
	31, [ o[26],  o[19],  o[32] ],  
	// Parameter
	32, [ name: o[20],  value: 0.000000,  
		spec: o[21],  action: nil,  
		siblings: o[33],  sendOSC: false,  
		netAddr: o[35],  oscMess: o[37],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	33, [ array: o[34] ],  
	// Array
	34, [ o[26],  o[19],  o[29] ],  
	// NetAddr
	35, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	38, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	40, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	42, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// Parameter
	44, [ name: o[45],  value: 0.000000,  
		spec: o[46],  action: nil,  
		siblings: o[48],  sendOSC: false,  
		netAddr: o[65],  oscMess: o[66],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// ControlSpec
	46, [ minval: 0.000000,  maxval: 1.000000,  
		warp: o[47],  step: 0.000000,  
		default: 0.000000,  units: o[23],  
		clipLo: 0.000000,  clipHi: 1.000000 ],  
	// LinearWarp
	47, [ spec: o[46] ],  
	// List
	48, [ array: o[49] ],  
	// Array
	49, [ o[50],  o[53],  o[56] ],  
	// Parameter
	50, [ name: o[45],  value: 0.000000,  
		spec: o[46],  action: nil,  
		siblings: o[51],  sendOSC: false,  
		netAddr: o[63],  oscMess: o[64],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	51, [ array: o[52] ],  
	// Array
	52, [ o[44],  o[53],  o[56] ],  
	// Parameter
	53, [ name: o[45],  value: 0.000000,  
		spec: o[46],  action: nil,  
		siblings: o[54],  sendOSC: false,  
		netAddr: o[61],  oscMess: o[62],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	54, [ array: o[55] ],  
	// Array
	55, [ o[50],  o[44],  o[56] ],  
	// Parameter
	56, [ name: o[45],  value: 0.000000,  
		spec: o[46],  action: nil,  
		siblings: o[57],  sendOSC: false,  
		netAddr: o[59],  oscMess: o[60],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	57, [ array: o[58] ],  
	// Array
	58, [ o[50],  o[44],  o[53] ],  
	// NetAddr
	59, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	61, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	63, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	65, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// Parameter
	67, [ name: o[68],  value: 0.000000,  
		spec: o[69],  action: nil,  
		siblings: o[71],  sendOSC: false,  
		netAddr: o[88],  oscMess: o[89],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// ControlSpec
	69, [ minval: 0.000000,  maxval: 1.000000,  
		warp: o[70],  step: 0.000000,  
		default: 0.000000,  units: o[23],  
		clipLo: 0.000000,  clipHi: 1.000000 ],  
	// LinearWarp
	70, [ spec: o[69] ],  
	// List
	71, [ array: o[72] ],  
	// Array
	72, [ o[73],  o[76],  o[79] ],  
	// Parameter
	73, [ name: o[68],  value: 0.000000,  
		spec: o[69],  action: nil,  
		siblings: o[74],  sendOSC: false,  
		netAddr: o[86],  oscMess: o[87],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	74, [ array: o[75] ],  
	// Array
	75, [ o[67],  o[76],  o[79] ],  
	// Parameter
	76, [ name: o[68],  value: 0.000000,  
		spec: o[69],  action: nil,  
		siblings: o[77],  sendOSC: false,  
		netAddr: o[84],  oscMess: o[85],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	77, [ array: o[78] ],  
	// Array
	78, [ o[73],  o[67],  o[79] ],  
	// Parameter
	79, [ name: o[68],  value: 0.000000,  
		spec: o[69],  action: nil,  
		siblings: o[80],  sendOSC: false,  
		netAddr: o[82],  oscMess: o[83],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	80, [ array: o[81] ],  
	// Array
	81, [ o[73],  o[67],  o[76] ],  
	// NetAddr
	82, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	84, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	86, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	88, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// Parameter
	90, [ name: o[91],  value: 0.000000,  
		spec: o[92],  action: nil,  
		siblings: o[94],  sendOSC: false,  
		netAddr: o[111],  oscMess: o[112],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// ControlSpec
	92, [ minval: 0.000000,  maxval: 1.000000,  
		warp: o[93],  step: 0.000000,  
		default: 0.000000,  units: o[23],  
		clipLo: 0.000000,  clipHi: 1.000000 ],  
	// LinearWarp
	93, [ spec: o[92] ],  
	// List
	94, [ array: o[95] ],  
	// Array
	95, [ o[96],  o[99],  o[102] ],  
	// Parameter
	96, [ name: o[91],  value: 0.000000,  
		spec: o[92],  action: nil,  
		siblings: o[97],  sendOSC: false,  
		netAddr: o[109],  oscMess: o[110],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	97, [ array: o[98] ],  
	// Array
	98, [ o[90],  o[99],  o[102] ],  
	// Parameter
	99, [ name: o[91],  value: 0.000000,  
		spec: o[92],  action: nil,  
		siblings: o[100],  sendOSC: false,  
		netAddr: o[107],  oscMess: o[108],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	100, [ array: o[101] ],  
	// Array
	101, [ o[96],  o[90],  o[102] ],  
	// Parameter
	102, [ name: o[91],  value: 0.000000,  
		spec: o[92],  action: nil,  
		siblings: o[103],  sendOSC: false,  
		netAddr: o[105],  oscMess: o[106],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	103, [ array: o[104] ],  
	// Array
	104, [ o[96],  o[90],  o[99] ],  
	// NetAddr
	105, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	107, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	109, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	111, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// Parameter
	113, [ name: o[114],  value: 0.000000,  
		spec: o[115],  action: nil,  
		siblings: o[117],  sendOSC: false,  
		netAddr: o[134],  oscMess: o[135],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// ControlSpec
	115, [ minval: 0.000000,  maxval: 1.000000,  
		warp: o[116],  step: 0.000000,  
		default: 0.000000,  units: o[23],  
		clipLo: 0.000000,  clipHi: 1.000000 ],  
	// LinearWarp
	116, [ spec: o[115] ],  
	// List
	117, [ array: o[118] ],  
	// Array
	118, [ o[119],  o[122],  o[125] ],  
	// Parameter
	119, [ name: o[114],  value: 0.000000,  
		spec: o[115],  action: nil,  
		siblings: o[120],  sendOSC: false,  
		netAddr: o[132],  oscMess: o[133],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	120, [ array: o[121] ],  
	// Array
	121, [ o[113],  o[122],  o[125] ],  
	// Parameter
	122, [ name: o[114],  value: 0.000000,  
		spec: o[115],  action: nil,  
		siblings: o[123],  sendOSC: false,  
		netAddr: o[130],  oscMess: o[131],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	123, [ array: o[124] ],  
	// Array
	124, [ o[119],  o[113],  o[125] ],  
	// Parameter
	125, [ name: o[114],  value: 0.000000,  
		spec: o[115],  action: nil,  
		siblings: o[126],  sendOSC: false,  
		netAddr: o[128],  oscMess: o[129],  
		sendMIDI: false,  midiPort: nil,  
		midiCtl: nil,  midiChan: nil ],  
	// List
	126, [ array: o[127] ],  
	// Array
	127, [ o[119],  o[113],  o[122] ],  
	// NetAddr
	128, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	130, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	132, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// NetAddr
	134, [ addr: 2130706433,  port: 57120,  
		hostname: o[36],  socket: nil ],  
	// Event
	136, [ array: o[137],  size: 2,  
		proto: nil,  parent: nil,  
		know: true ],  
	// Array
	137, [ 'name',  o[16],  'parameters',  o[138],  
		nil,  nil,  nil,  nil ],  
	// List
	138, [ array: o[139] ],  
	// Array
	139, [ o[29],  o[53],  o[76],  o[99],  
		o[122] ],  
	// Event
	140, [ array: o[141],  size: 2,  
		proto: nil,  parent: nil,  
		know: true ],  
	// Array
	141, [ 'name',  o[16],  'parameters',  o[142],  
		nil,  nil,  nil,  nil ],  
	// List
	142, [ array: o[143] ],  
	// Array
	143, [ o[32],  o[56],  o[79],  o[102],  
		o[125] ],  
	// Event
	144, [ array: o[145],  size: 2,  
		proto: nil,  parent: nil,  
		know: true ],  
	// Array
	145, [ 'name',  o[146],  'parameters',  o[147],  
		nil,  nil,  nil,  nil ],  
	// List
	147, [ array: o[148] ],  
	// Array
	148, [ o[26],  o[50],  o[73],  o[96],  
		o[119] ]
];
prUnarchive(o,p);
