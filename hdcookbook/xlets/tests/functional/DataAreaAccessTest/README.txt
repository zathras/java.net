                    ORG ID TEST XLET
                    ================


This is a small test xlet to investigate player behavior
with regards to the organization_id value.  A writeup of
the issue it's investigating can be found at
http://wiki.java.net/bin/view/Mobileandembedded/BDJOrgIDDiscVsXlet .

This test tries reading and writing to the ADA and the BUDA in three 
different ways:

    *  On a disc where the xlet org ID is the same as the disc org ID

    *  On a disc where the xlet org ID is different from the disc org ID

       o  In a directory named after the xlet org ID

       o  In a directory named after the disc org ID

It displays its results on the screen, for easy logging into a
compatibility spreadsheet.  Please see the wiki article at
http://wiki.java.net/bin/view/Mobileandembedded/BDJOrgIDDiscVsXlet
for an authoring guideline related to this test.

DISCLAIMER:  This test was written to test various players to test 
compatibility.  It ran on enough players to answer the questions 
that needed to be answered, but please be aware that it did fail 
to run on some players.  Once the needed results were obtained, 
we didn't investigate the problems on the other players.

