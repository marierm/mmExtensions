[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=marierm&url=https://github.com/marierm/mmExtensions&title=mmExtensions&language=en_GB&tags=github&category=software)

The mmExtensions for SuperCollider
==================================

At this point, this repository contains the files required to use to the
PresetInterpolator and the Sponge.

Installation
============

These sc files go inside your SuperCollider Extensions directory:

~/share/SuperCollider/Extensions/	    	                 (Linux)
~/Library/Application\ Support/SuperCollider/Extensions/	 (Mac OSX)


Dependencies
============

- SenseWorld DataNetwork Quark (by Marije Baalman)
- wslib Quark (by Wouter Snoei)
- ddwGUIEnhancements
- crucial library


Description
===========

PresetInterpolator() is a preset interpolation system similar in concept to
the Metasurface found in AudioMulch (created by Ross Bencina) or the max patch
SpaceMaster (by Ali Momeni and David Wessel).  There is an important
difference, though: PresetInterpolator() can interpolate spaces of any number
of dimensions.

Interpolator() is the interpolation system used by PresetInterpolator().  It
does not do much: it simply outputs weights.


How to use it
=============

These classes are not documented yet, but here are a few notes to get you
started.

There is only one parameter to Interpolator: the number of dimensions.
This creates a 2D interpolator:

    x = Interpolator(2);
    x.gui; // nice GUI.

* The (bigger) grey dot is the cursor;
* The colored dots are the data points;
* The size of the transparent circles around the data points is
  proportional to the weight of that preset;

In the Interpolator window:
===========================
* Double-click in the area to add new data points;
* Alt-shift-click deletes a presets;

When using the Interpolator to interpolate between presets:
==========================================================
(see PresetInterpolator below)
* Alt-click and drag to duplicate a data point (a preset);
* Alt-click and drag also works on the cursor (the grey point);
* Double-click on a preset to edit it;


A PresetInterpolator takes an Interpolator as an argument.  If no argument
is provided, a 2D Interpolator is created for you;
    y = PresetInterpolator(x);
    y.gui;

In the PresetInterpolator window:
================================
There is a row for each data point (which is associated with a preset).
Here is a description of the columns:
   
1. The preset name (pressing enter after editing the textField).
2. The "Attach" button. This button attaches the data point to the
   cursor.  Every time the cursor is moved, the attached point follows
   it. This is useful to position a point in a multidimensional space
   using an interface.  There will be an screencast of an example soon.
3. The "Edit" button. Opens a PresetGui window (edit the preset).
4. The data point id (an integer).
5. The data points coordinates.
6. The "Remove" button (X).
7. The weight of the point.

The last row is different:
1. "Cursor".
2. "Edit" button.
3. One PopUpMenu for each coordinate. (See note)
4. "2D Gui" button: creates an interpolator GUI (2D).

The PopUpMenus have 3 possible values: nothing, x, y.  They are used to
select which coordinates will be used by the 2D GUI.  Especially useful to
visualize multidimensonal data.


In preset window:
================= 
* Click on the 'Add' to add more parameters.
The presets can have any number of parameters and these parameters can
represent anything.
* Click on the '+' to edit the parameter's ControlSpec.
* Click on the "O" button to open the OSC preferences of the parameter. (Temporarily not working.)
* Click on the "M" button to open the MIDI preferences of the parameter. (Temporarily not working.)
* Parameters can be named.

Each preset of a PresetInterpolator always have the same number of
Parameters.  Changing the Spec or the name of a Parameter will change
the spec or the name of the corresponding Parameters of other presets.
Example: if I rename the third parameter of a preset to "foo", the third
parameter of all presets will be renamed "foo".

    y.cursor; // this 'preset' represents the cursor

When adding an action to the currentPreset's parameters, moving the cursor
does something!

    (
    y.cursor.parameters[0].action_({|mapped, unmapped|
    	"Parameter 0: ".post;
	mapped.postln;
    });
    )
Each parameter has a ControlSpec (it can be edited in the GUI as well)

    y.cursor.parameters[0].spec_(ControlSpec(20,20000,\exp));
    y.cursor.parameters[0].spec_(\midi); //this works too.

// Proper doc is on the way.




The algorithm
=============

It is not a planetary model (weight = inverse of distance).  It is not natural
neighbour interpolation (like in the metasurface).  It is based on
intersecting circles.  Here is how it works (briefly):

1- A (invisible) circle is drawn around each points (including the cursor).
   The radius of each circle is equal to the distance to the nearest
   neighbour.  This means that the size of the circles varies when points are
   moved.

2- The points which have a circle that intersects the cursor's circle have a
   non nil weight.  (Not all points do, but at least one does.)

3- The weight of each point is calculated like this: (intersecting area of the
   2 circles) / (total area of the circle).

4- Then we do a weighted sum for each parameter using these weights.

I know, it is not very clear.  Just look at the code!


And it works in spaces with more than 2D (I tried up to 10D).


Cheers!

Martin Marier

