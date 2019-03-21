#!/bin/sh
cd ../src
mkdir ../classes
mkdir ../classes/resources
javac -d ../classes hat/Main.java
cp -r resources/*.txt ../classes/resources/
cd ../classes
rm -rf ../bin/hat.jar
jar cmf ../src/MANIFEST.mf ../bin/hat.jar *
cp ../bin/hat.jar ~/bin

