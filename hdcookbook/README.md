# HDCOOKBOOK java.net project, version 1.2

This directory is an archive of the old hdcookbook open-source project
from java.net.  It has been updated to run on JDK 1.8, and was tested
and known to work in January 2022.  See README.pdf for a description
of the contents.

The following worked on Ubuntu Linux 20.x, with the standard JDK 1.8
installation selected:
```
billf@Zathras:~/github/java.net/hdcookbook$ ant
Buildfile: /home/billf/github/java.net/hdcookbook/build.xml

   ...  many lines of build output  ...

BUILD SUCCESSFUL
Total time: 17 seconds
```
```
billf@Zathras:~/github/java.net/hdcookbook$ java -version
openjdk version "1.8.0_312"
OpenJDK Runtime Environment (build 1.8.0_312-8u312-b07-0ubuntu1~20.04-b07)
OpenJDK 64-Bit Server VM (build 25.312-b07, mixed mode)
```
```
billf@Zathras:~/github/java.net/hdcookbook$ ant -version
Apache Ant(TM) version 1.10.12 compiled on October 13 2021
```
And, similarly on MacOS 11.6.2 (Big Sur, M1 silicon):
```
billf@londo:~/github/java.net/hdcookbook$ ant
Buildfile: /Users/billf/github/java.net/hdcookbook/build.xml

   ...  many lines of build output  ...

build-hdcookbook-xlets:

BUILD SUCCESSFUL
Total time: 8 seconds
```
```
billf@londo:~/github/java.net/hdcookbook$ java -version
openjdk version "1.8.0_302"
OpenJDK Runtime Environment (Zulu 8.56.0.23-CA-macos-aarch64) (build 1.8.0_302-b08)
OpenJDK 64-Bit Server VM (Zulu 8.56.0.23-CA-macos-aarch64) (build 25.302-b08, mixed mode)
```
```
billf@londo:~/github/java.net/hdcookbook$ ant -version
Apache Ant(TM) version 1.10.12 compiled on October 13 2021
```
## -XDignore.symbol.file

Note that `DiscCreationTools/security/make/build.xml` passes
`-XDignore.symbol.file` to `javac`.  This is necessary; it appears
some of the tools used for signing are hidden in JDK 1.8, unless
this option is passed.
