

This test probes an area of the specification that may be undefined.
What happens if you have two discs, A and B, where the two discs have
the same org ID but different root certificates, and you use a BUDA
credential to grant read/write access from disc B to disc A?  This test
can be performed by using "middle" as disc a, and this disc ("root") as
disc B.

Thus, this disc has a credential to grant it write access to the file
<middle's hash>/7fff0002/output_1.txt.  This physical file name gets
mapped to <root's hash>/7fff0002/output_1.txt.  However, this xlet's
org ID is 7fff0002, so it already has read-write access to the actual
physical file <root's hash>/7fff0002/output_1.txt.  So, when you do the
write, where does it go?  And, if <middle's hash>/7fff0002/output_1.txt
was already there, do we see it?

The first quesiton in answered by running this disc first, then middle.
The second is answered by running creator (thus creating the file under
middle's hash), then this disc.

Possible outcomes:

        The credential wins, that is, output_1 gets written under middle's 
        hash

        The credential loses, that is, output_1 gets written under root's
        hash

        Partial credential loss:  output_1 gets written under root's hash if
        it wasn't there, but if it was there under middle's hash, that mapped
        name applies

        Disc doesn't launch because player fails to cope with this totally
        unanticipated scenario

        Something else
