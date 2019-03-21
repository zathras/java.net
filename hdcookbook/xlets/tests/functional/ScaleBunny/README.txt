

This is a version of the GrinBunny game that has been modified to
run at different resolutions.  The GRIN show is scaled using the
automatic scaling feature of the GRIN show compiler, image matrix
generator and fontstrip compiler.  This sample is meant to demonstrate
writing an xlet once, and presenting it at different screen resolutions
without re-authoring the assets.

The show file was written for 1920x1080, and versions are built for:

        1920x1080 (full HD)
        1280x720
        960x540 (QHD)
        720x480 (NTSC SD) @ 16x9

Note that scaling to 720x480 means non-square pixels.

Since this is a model of a stand-alone project outside of HD cookbook, 
the build file doesn't include the top-level user.vars.properties file.
You might need to create a user.vars.properites in this directory,
pointing to your compilation stubs.
