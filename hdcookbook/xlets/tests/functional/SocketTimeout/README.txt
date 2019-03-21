

This little sample was put together to validate some code that Sun
was submitting to the BDA for the guidelines document.  Personal Basis
Profile 1.0 has no way to programmatically set a socket timeout.  Before
this was discovered and addressed in the guidelines document, some players
might have had a very long timeout for socket connections.

The code in this xlet, in SocketDemo.java, helps to work around this by
setting the socket timeout on players that support Personal Basis 1.1.
Note that it's intentionally hard for a BD-J app to access an API that's
present in PBP 1.1, and not PBP 1.0 - a direct reference to such an API is
out of the question, due to eager linking issues (see HD cookbook page
16-5, "Dealing With Optionally Present APIs).  For this reason, a verifier
woult be within its rights to reject a disc that contains an xlet with
such a reference.  It is only in cases of extreme need where it's justified
to use reflection to access such a feature.  This is what we have done here.

When you build this, be sure to make a user.vars.properites file to
override the settings in vars.properties for cookbook.dir and
bdj.classes.
