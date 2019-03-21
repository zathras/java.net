#!/bin/sh


java -classpath classes:bcel-5.1/bcel-5.1.jar \
	com.sun.billf.tass.MergeTassDatabases merged.tdb test.tdb test.tdb
