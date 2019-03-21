The sample xlets in this directory demonstrate how credentials work.


What are these xlets?
=====================

Writer:
The Writer xlet writes to a local storage using FileOutputStream
in a file-named:

String filename = System.getProperty("dvb.persistent.root")
               + "/" + context.getXletProperty("dvb.org.id")
               + "/" + context.getXletProperty("dvb.app.id")
               + "/tmp.txt";

Which actually computes to:
${disk-root-writer}/4000/01/tmp.txt
        where 0x4000 is the org id and 0x01 is the application id. 

Reader:
The reader aquires file credentials and tries to read back the data
from the same location that the Writer wrote to.
The Reader xlet you may however think is trying to read
from a different storage location and not from the writer's storage. The
code that initializes the filename being read is same as that
of the Writer's:

String filename = System.getProperty("dvb.persistent.root")
               + "/" + context.getXletProperty("dvb.org.id")
               + "/" + context.getXletProperty("dvb.app.id")
               + "/tmp.txt";

Which computes to: ${disk-root-reader}/4001/02/tmp.txt

However, in the presence of file credentials, this file will map to
Writer's directory:
${disk-root-reader}/4001/02/tmp.txt ====> ${disk-root-writer}/4000/01/tmp.txt

If the credentials work correctly, the read should succeed and the
Reader xlet should display:

        "READER test passed, accessed filesystem without SecurityException"

If the credentials do not work, then the test will fail displaying:

        "Test Failed with IOException";

See the notes in the ${HDCOOKBOOK}/DiscCreationTools/security/README-CREDENTIALS.txt for
the status of the credential signer tool.

How to Build?
=============

1) Create the distribution directory where you would like to have the
disc image stored. Set the location of this directory in the
user.vars.properties (vars.properties) file to the value of
HDC_DISK_BDMV variable.

2) Download the Disk Image from the hdcookbook website.
  a) Go to https://hdcookbook.dev.java.net/
  b) On the left side bar, click on "Documents & files"
  c) You will see a list of files here. Download the file named:
    " storageXlet-disk-image.zip".
  d) Unzip the bundle. This contains the BDMV directory.
  The HDC_DISK_BDMV variable in the user.vars.properties file points
  to this directory.

3) Build the project using ant. This will populate the JAR, BDJO and
CERTIFICATE directories of the disk image.

Note: You will need to build two disk images separately. One for the
Writer and the other for the Reader.

Testing the Disk Images:
========================
You need to first run the writer xlet disk image and subsequenly
run the reader xlet disk image to check the results.

