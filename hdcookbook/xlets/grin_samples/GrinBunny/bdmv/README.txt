
The directories stream and clipinf contain the mpeg-2 transport 
stream and the clip information file for the starfield background 
of the GrinBunny game.  These two files together are the logical 
equivalent of a .mov or .wmv file.  As of this writing (Decmeber 2008) 
there were no cookbook tools to produce these files, though they can be 
made with prosumer-level BD authoring tools that are relatively inexpensive.

The playlist directory contains a playlist that can be used to play
this clip.  The playlist is compiled during the build process; see
../build.xml for details.

These files all complement the GenericGame's bdmv directory, which
contains other files used to make a runnable disc.
