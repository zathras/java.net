
This directory contains a test of sync frame accurate animation,
using the BD API and com.hdcookbook.grin.animator.SFAAEngine.
It starts an xlet with some video of a red bear outline, and 
tracks PNG images of the bear over the video.

This test runs in QHD, but the area it covers is still greater than
the maximum area suggested by the BD minimum performance numbers, so
don't be surprised if about half of the frames get dropped.  If you're
thinking of using SFAA yourself, be sure also to read the class comments
in com.hdcookbook.grin.animator.SFAAEngine.  Further, note that the
video for this test doesn't have any "pre roll" time, and some
implementations take a fair amount of time to initialize the SFAA
subsystem.

If you want to test SFAA on a given player, follow these steps:

    1.  Insert disc into player
    
    2.  When it prompts you to enter the number of buffers, press "1"
        or "2".  Do not use the arrow keys to set a trim value before 
        pressing the number key.  Let the media play through, which 
        should take about 12 seconds.  You may see a fairly long delay 
        between when the video starts playing (that's the red bear 
        outline), and when SFAA starts presenting graphics (that's 
        the bear images that attempt to cover the red outline).  
        If there is a long delay, don't worry; this isn't part of 
        the test.
        
    3.  When it prompts you to press enter to run the test again, press enter.
        Ignore the results on the screen.
        
    4.  When it prompts you to enter the number of buffers, press "1".
        Do not use the arrow keys to set a trim value before pressing
        the number key.
    
    5.  Watch the registration of the video (red outline) and the SFAA
        graphics (bear picture).  It's OK if the graphics is behind the
        video by up to two frames.  During the initial diagonal movement
        of the bear, this equates to a registration difference between the
        two of 20 video pixels horizontally and 12 video pixels vertically
        (the video is encoded at full HD, 1920x1080).  It's also OK if
        the bear animation drops half of its frames, resulting in a
        real animation rate of 12fps - visually, this would be seen as
        slightly "jerky" bear movement with smoother outline movement.
        It may also intermittently fall behind by a couple of additional
        frames, perhaps once or twice during the movement (due to GC or
        other non-Java system activity).
        
        Within the parameters discussed above, the bear The picture shall
        remain registered with the bear outline throughout the video
        presentation.

    6.  When you see the results screen, it should say it skipped
        N frames out of a total of M frames.  M shall be between 260 and 300,
        and N shall be no greather than 55% of M.  Press enter.

    7.  When it prompts you to enter the number of buffers, press "4".
        Do not use the arrow keys to set a trim value before pressing
        the number key.

    8.  Repeat steps 5 and 6, noting the results.

    9.  Eject the disc


To build this xlet, you'll need to make a file called user.vars.properties
that overrides the setting of bdj.classes from vars.properties.  If you copy
the xlet source to a different directory in order to modify it, you'll also
need to override the setting for hdcookbook.dir.  You can override
debug.or.deploy if you want a debug build - this gets you debug output
(telnet to port 6000), and it lets you add an offset to the image registration.

