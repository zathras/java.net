#!/bin/sh

hm=~
if [ "$windir" != "" ] ; then
    hm=c:
fi
if [ "$HDC_NOSETVARS" != "yes" ] ; then
    HDC_REPOSITORY=$hm/java.net/hdcookbook
    HDC_BUILD_DIR=$hm/java.net/hdcookbook/AuthoringTools/grin/build
    HDC_BDJ_PLATFORM_CLASSES=$hm/bd-j/local/bdj_stubs/classes/interactive/classes.zip
fi


