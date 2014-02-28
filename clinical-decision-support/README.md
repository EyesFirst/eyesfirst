# Clinical Decision Support User Interface

These packages are part of the new Clinical Decision Support User Interface.

## Installation Instructions

Create the following databases and users:

    CREATE DATABASE efid;
    CREATE DATABASE eyesfirst;
    GRANT ALL PRIVILEGES ON efid.* TO eyesfirst@localhost IDENTIFIED BY 'eyesfirst';
    GRANT ALL PRIVILEGES ON eyesfirst.* TO eyesfirst@localhost;

## Development Configuration

The current development configuration places the following services on the
following URLs:

Service                 | URL
------------------------|--------------------------------------------------
EFID service            | http://localhost:8180/efid/
Image processor service | http://localhost:8280/image-processor-webapp/
DCM4CHEE                | localhost:11112
DCM4CHEE WADO           | http://localhost:8888/wado

## Compiling and Using the OpenID Plugin

Run `grails maven-install` inside the `openid-connect` directory to locally
install the plugin, allowing Grails to locate it when running inside EyesFirst.

When I tried this, for whatever reason, it failed the first time and worked the
second. It looks like it downloaded the dependencies the first time, failed to
locate them for whatever reason, and then located the dependencies it downloaded
the first time on the second run. So keep on trying `grails maven-install` until
it either works or you get a loop of errors.

## The uploader applet

At present the uploader applet is not contained within source control. You will
need to build it and then manually copy it over to
`eyesfirst/web-app/upload/applet`.