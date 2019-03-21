#!/bin/sh

java -classpath classes:bcel-5.1/bcel-5.1.jar \
	com.sun.billf.tass.dbview.TdbViewer test.tdb lib/classes.zip
