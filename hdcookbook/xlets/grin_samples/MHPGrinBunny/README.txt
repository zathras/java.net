This Game has been copied from GrinBunny with all blu-ray packaging
and code removed.  This is intended for MHP.

This is an arcade-style game called "MHPGrinBunny."  It is a rewrite
of the Gun Bunny game that is included in the HD cookbook
disc image.  This version is built using the GRIN framework, it is
targetd to the MHP platform using the ../GrinMHPXlet, 
and shows how Java code can drive a GRIN scene graph to produce 
this kind of game.

For the original, see xlets/hdcookbook_discimage/gunbunny.

Gun bunny was re-written to use GRIN, and to use the GenericXlet
framework that's now a part of the HD cookbook project.
The game xlet has a debug screen you can get to with the 0 key.  
This lets you suppress KEY_RELEASED events, so you can test
how a game would play on a player that doesn't generate them.  The
MHPGrinBunny game doesn't do anything on KEY_RELEASED, so when you do this
test with MHPGrinBunny you'll find that its behavior is unchanged.

Since this is a model of a stand-alone project outside of HD cookbook, 
the build file doesn't include the top-level user.vars.properties file.
You might need to create a user.vars.properites in this directory,
pointing to your compilation stubs.
