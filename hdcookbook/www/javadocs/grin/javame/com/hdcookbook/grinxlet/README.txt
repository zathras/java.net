
This directory has the Java source code and build file for the
skeleton of an xlet that uses the GRIN scene graph.  Many menus,
games and other applications can be built as a GRIN show file, for 
the presentation aspects, and a director, for the underlying logic.  
The actual xlet class can be identical for a large class of xlets, as 
it just does launching and some bookkeeping.

This directory holds the source code and build for a generic grin xlet.
There are two versions of the grin xlet:

    debug:  The xlet includes a pop-up menu that can be used to access
            various debug features, such as a debug log, a feature
            to disable KEY_RELEASE events, etc.
            
    deploy: The xlet turns off all debug features, including the GRIN
            runtime debug output and assertions that are controlled by
            com.hdcookbook.grin.util.Debug.

In addition, GrinView contains a stripped-down version of GrinXlet.
Because of this, you can call the public GrinXlet APIs from a Director,
and still have your app work under GrinView.

An example of a game that uses this generic xlet can be found in
../grin_samples/GrinBunny/.  As you can see, GrinBunny just includes 
the build.xml file defined here.  GrinBunny adapts the build to include 
what it needs by adapting the vars.properties file in ../GrinBunny.  It's 
hoped that many apps can be created by just copying build.xml and 
vars.properties, and then customizing vars.properties.  If you're 
able to do that (rather than copying generic_build.xml and modifying 
it), then you'll automatically integrate any improvements in GrinXlet
whenever you do a bringover.

The build system assumes that the grin xlet has three directories:

    src     Includes all source code and assets used in the xlet runtime

    se_src  Includes source code of anything needed on the desktop at
            compile  time, such as support files for app-specific extensions.
            This directory must exist, but it may be empty.

    xlet_src  Includes the source code of anything needed in the xlet that
            can't be present on SE, such as code that depends on javax.tv
            or org.bluray APIs.  This directory must exist, but it may be
            empty.

The show file must have an exported segment (usually called "S:Initialize").
The xlet will navigate to this segment when it is started.  If the
player destroys the xlet by calling Xlet.destroyXlet(), the xlet
will cause Show.destroy() to be called in the animation thread, which 
will in turn call the director's notifyDestroyed() method.

A disc image that launches the xlet is created by the build system in the
"dist" directory, and the contents of that directory are copied into
a zip file.  Another target, grinview-jar, creates an executable Java 
JAR file that can be used to test the app on Java SE.  This target is 
convenient for sending a desktop mockup of your app that people can 
just double-click and run.


SAMPLES OF GrinXlet USAGE
=========================

As of this writing, there are two sample projects that use GrinXlet
bundled with the cookbook repository, in ../grin_samples/GrinBunny 
and ../grin_samples/SimpleGame.  Since these are models of
stand-alone projects outside of HD cookbook, they don't include the
top-level user.vars.properties file, so you might need to create a
user.vars.properites file for each, pointing to your compilation stubs.

In the future, we're planning to change the "bookmenu" project over to
be based on GrinXlet, too.
