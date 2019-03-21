#!/bin/sh

function cvsstrip () {
    while [ $# != 0 ]; do
	find $1 -type f | grep -v "/CVS/"
	shift;
    done
}

cd ..
rm -f dist/hat_bin.zip
rm -f dist/hat_src.zip
mkdir dist
zip -r dist/hat_bin.zip `cvsstrip bin doc misc`
zip -r dist/hat_src.zip `cvsstrip bin src misc build doc`

echo 
echo "Made distribution files in dist.  To export hat, just"
echo "copy index.html, doc, dist and misc to the target directory."
echo 
