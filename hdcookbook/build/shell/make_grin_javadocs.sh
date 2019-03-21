#!/bin/sh

source vars.sh
DEST=$HDC_BUILD_DIR/grin_javadoc

cd $HDC_REPOSITORY
ant javadoc-deploy
if [[ $? != 0 ]] ; then
    exit 1;
fi


echo ""
echo "Built javadocs for all source files in the repository:"
echo "../../www/javadocs/grin"

