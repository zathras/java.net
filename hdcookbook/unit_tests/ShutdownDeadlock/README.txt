

Test for a deadlock on show shutdown. 

This contains both an xlet and a Java SE program.  Both do basically
the same thing:  They start with a show that loads a bunch of different
images.  They measure how long it takes this show to run, and then they
run it and destroy it with different time delays, running from 0ms to 150%
of the maximum image load time observed.

This is done to try to flush out deadlock bugs related to destroying a
show that's in the setup phase.

Strictly speaking, this isn't a "unit" test.  Oh well.

See ManagedFullImage's state model and a discussion of flush() to 
see the issue that this helped fix.

