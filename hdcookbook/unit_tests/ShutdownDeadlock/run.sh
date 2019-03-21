#!/bin/sh
#
#  A convenience shell script for running the PC version of this
#  test.  "ant test" does the same thing, after running a quick
#  GrinView test.

ant generate-binary-script

java -cp build/gensrc/grinview:../../bin/grinviewer.jar ShutdownDeadlock
if [ $? != 0 ] ; then
    echo ""
    echo "**** ERROR!  TEST FAILED!  ****"
    echo ""
    exit 1
else
    echo "Test passed."
    exit 0
fi

