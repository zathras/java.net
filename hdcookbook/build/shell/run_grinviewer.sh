#!/bin/sh -x
#
#  This shell script runs grinview on various show file
#

TOP=`dirname $0`/../..
GRIN_BASE=$TOP/AuthoringTools/grin
HDCDISC_BASE=$TOP/xlets/hdcookbook_discimage
CLASSES=$TOP/bin/grinviewer.jar
MENU_CLASSES=$HDCDISC_BASE/build/xlets_tools/menuxlet
MENU_GENERATED=$HDCDISC_BASE/build/xlets/menu_generated/grinview
EXTENSION_PARSER=com.hdcookbook.bookmenu.menu.MenuExtensionParser

case $1 in
    menu)
	    HD_SRC=$HDCDISC_BASE/bookmenu/src/com/hdcookbook/bookmenu
	    ASSETS=$HD_SRC/assets
	    BG_IMG=$HD_SRC/menu/test_assets/MenuScreenBG_gray.png
	    java -cp $CLASSES:$MENU_CLASSES:$MENU_GENERATED \
	    	    com.hdcookbook.grin.test.bigjdk.GrinView \
		    -asset_dir $ASSETS \
		    -extension_parser $EXTENSION_PARSER \
		    -background $BG_IMG \
		    -fps 24 menu.txt
	    ;;

    menu-bin)
    	    cd $HDCDISC_BASE
	    ant -f run_jdktools.xml run-grin-viewer-binary
	    ;;

    test)
	    ASSETS=$HDCDISC_BASE/../grin_samples/Scripts/DrawingOptimization
	    java -cp $CLASSES com.hdcookbook.grin.test.bigjdk.GrinView \
		    -asset_dir $ASSETS show.txt
	    ;;

    ryan)
	    ASSETS=$GRIN_BASE/jdktools/grinviewer/src/com/hdcookbook/grin/test/assets
	    java -cp $CLASSES com.hdcookbook.grin.test.bigjdk.GrinView \
		    -extension_parser com.hdcookbook.grin.test.RyanExtensionParser \
		    -screensize pal -scale 1 \
		    -asset_dir $ASSETS ryan_show.txt
	    ;;
    *)
    	echo ""
    	echo "Usage:  $0 [ menu | menu-bin | test ]"
    	echo ""
	exit 1
	;;
esac

