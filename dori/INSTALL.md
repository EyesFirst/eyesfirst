EyesFirst DORI Web Installation Instructions
============================================

DORI Web is split into two projects: the DORI Browser Grails web application,
and the DICOM Uploader Applet, which is used to anonymize DICOM files prior to
them being uploaded to the DORI server. The SOLR directory contains information
on how to configure SOLR for use by the DORI Browser web application.

The DORI Web software is in the "dori-web" directory and is a Grails 1.3
project. The DICOM Uploader Applet is in the "dicom-uploader-applet" directory
and is a Maven 2.x project.

Required Software
-----------------

Before you can run DORI Web, you will need to install the following required
software:

 * [Apache Tomcat 7](#tomcat-7)
 * [Java Runtime Environment](#jre)
 * [MySQL 5.5](#mysql)
 * [DCM4CHEE](#dcm4chee)
   * JBoss 4.2.3.GA (required by DCM4CHEE)
 * [SOLR](#solr)

In order to build it, you will also need:

 * Grails 1.3.7
 * Java Development Kit 1.6

<h3 id="tomcat-7">Apache Tomcat 7</h3>

<http://tomcat.apache.org/>

If you are using Linux, there's a good chance your OS will provide Apache
Tomcat packages. These packages will also handle the Java Runtime Environment
dependency.

RedHat:

    sudo yum install tomcat

Debian (Ubuntu, Mint, etc.):

    sudo apt-get install tomcat7

For all other OSes, get it from the [Apache Tomcat downloads page](http://tomcat.apache.org/download-70.cgi).
You will also required a [Java Runtime Environment](#jre) to run Tomcat.

<h3 id="jre">Java Runtime Environment</h3>

If you are compiling the code, you will require the Java Development Kit (JDK).
It is [available from Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
You should use Java SE 6 or later.

If you are using Linux, your distribution most likely provides a version of the
OpenJDK. While the OpenJDK is not the Oracle JDK, it should work with DORI Web
(although this has not been extensively tested).

<h3 id="mysql">MySQL 5.5</h3>

<http://www.mysql.com/>

If you are using Linux, your OS almost certainly will provide MySQL 5 packages.
Exactly how to install them varies by OS, but generally speaking:

RedHat:

    sudo yum install mysql5-server

Debian (Ubuntu, Mint, etc.):

    sudo apt-get install mysql-server-5.5

For all other OSes, go to the [MySQL downloads page](http://www.mysql.com/downloads/)
and download the appropriate installer from there.

DCM4CHEE
--------

<http://www.dcm4che.org/>

DORI Web acts as a front end to DCM4CHEE and does not store DICOM images on its
own.

Follow the [installation instructions for DCM4CHEE](http://www.dcm4che.org/confluence/display/ee2/Installation).

Note that you'll need to download and install JBoss 4.2.3.GA in order to run
DCM4CHEE itself, which is also a servlet container and therefore may conflict
with Tomcat.

Grails 1.3.7
------------

<http://grails.org/>

DORI Web is currently built against Grails 1.3.7 and has not been tested with
the newer versions.

Download it from <http://grails.org/Download> and follow the installation
instructions.

SOLR
----

<http://lucene.apache.org/solr/>

The Solr configuration is in the "solr" directory. Essentially you should be
able to point your Solr home to be the "solr/solr" directory and it should work.

Building DORI Web
-----------------

At present, the latest version of the DORI web uploader applet is checked in
along with the rest of the code. If you have Grails installed, building the
application is fairly simple:

    grails upgrade
    grails war

The "upgrade" step is currently required to ensure that all the required
dependencies have been properly loaded. You only need to run it once.