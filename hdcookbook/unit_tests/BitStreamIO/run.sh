#!/bin/sh
rm -rf classes
mkdir classes
javac -g -d classes -sourcepath .:../../AuthoringTools/grin/library/src *.java 
java -cp classes Test
# jdb -classpath classes -sourcepath .:../../AuthoringTools/grin/library/src Test
rm -rf classes
