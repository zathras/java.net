#!/bin/sh

cd ../../xlets/hdcookbook_discimage
ant -f build.xml build-menu-xlet
if [[ $? != 0 ]] ; then
    exit 1;
fi
