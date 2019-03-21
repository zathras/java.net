

If the xlet is unsigned, we get a SecurityException when
opening the http: URL.  Instead of not having the bio feature,
we just fake reading from a network by opening these files,
with a one-second delay.


