#!/bin/sh -x

# Recreates a hdcookbook disc image from a release bundle, by adding JAR, 
# CERTIFICATE and BDJO to the disc image.
# Adjust "nojar_image" to the root of the hdcookbook disc image downloaded
# from http://hdcookbook.dev.java.net/servlets/ProjectDocumentList.

nojar_image_name=2008_03_hdcookbook_disc_image_no_jar_bdjo

bundle=HDCookbook-DiscImage.zip

#CHANGE THIS TO SUIT YOUR NEED
nojar_image=C:/"$nojar_image_name".zip

dist_dir=HDCookbook-DiscImage-complete

if [ ! -f $bundle ] ; then
	echo "File not found: $bundle ";
        exit 1;
fi

if [ ! -f $nojar_image ]  ; then
	echo "Disc image not found. $nojar_image ";
	echo "Image can be downloaded from hdcookbook.dev.java.net/servlets/ProjectDocumentList"
        exit 1;
fi

mkdir -p $dist_dir
cd $dist_dir
jar xvf $nojar_image
cd $nojar_image_name
jar xvf ../../$bundle

echo "Created a complete disc image at $dist_dir."
