

This directory contains projects to build disc images
that are used to exercise BUDA credentials.  Each is signed
with a different root certificate, and uses a different org ID.
These disc images are:

Creator:  This image uses a BUDA credential to create a file in the
directory owned by "Middle".

Middle:  This image doesn't use a credential, and checks for the
file created by Creator.  It then creates another file.

Destroyer:  This image uses a BUDA credential containing a wildcard.
It checks for the two files that should be there after Middle has
successfully run.  It then does some read and write tests, and then
deletes the various files out of Middle's directory.

Root:  This image is outside of the Creator->Middle->Destroyer test
sequence.  It has the same org ID as "middle", but uses a different
root certificate (whereas the other disc images all have different root
certificates and different org IDs).  See root/README.txt for more information.

Wildcard:  This image is outside of the Creator->Middle->Destroyer test
sequence.  It is meant to be run first, and it exercises wildcarded
credentials, both the "/*" and "/-" variants.  Like "creator," it results
in the file "output_1.txt" being left in the place where "middle" expects
it to be, which allows easy verification that a new file is created in the
directory determined by the root cert of the grantor, even with a wildcard
certificate.

creatorWithMultiOrgs and destroyerForMultiOrgs:

These two disc images demonstrate a practical scenario for BUDA Credential's
use. The creator(CreatorWithMultiOrgs) disc image uses a root certificate that
is issued by a Studio with a specific OrgID. The vendor then creates an application
certificate with Studio cert as its issuer with its own OrgID.

The destroyer needs to access the files written out by the creator using
credentials.
The BD-J spec requires that, the credential signer's certificate issuer's OrgID
must match OrgID of the credential signer itself. But the certificate issuer
in this case is the Studio that has a different OrgID than the credential signer (
the application vendor).

In order to make this work, the application vendor creates another certificate
using its OrgID, issued by its own certificate that inturn was issued by the Studio.
The new certificate issued this way is used for signing the credentials. 
This way OrgID of the certificate that is used to sign the credentials is same
as OrgID of the issuer of the credential signer certificate. Since the root does not
change, BUDA area is accessible for any certificate in the chain that has same
OrgID as the original one that wrote to BUDA.
