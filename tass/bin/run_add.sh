#!/bin/sh


java -classpath classes:bcel-5.1/bcel-5.1.jar \
	com.sun.billf.tass.AddToTassDatabase test.tdb lib/packages.txt \
	test_prog/*.class
