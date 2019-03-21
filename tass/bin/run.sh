#!/bin/sh

java -classpath classes:bcel-5.1/bcel-5.1.jar com.sun.billf.tass.Test \
	lib/classes.zip lib/packages.txt 47.0 \
	test_prog/*.class
