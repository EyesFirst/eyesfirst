DICOM Uploader Applet
=====================

Signing the Applet
------------------

As the uploader system is a "child" of the uploader applet, it is no longer
signed as part of the default Maven build. This proved to be too obnoxious, as
not having set up the applet for signing would cause the build to fail. Instead,
the applet is now built via an Ant script.

Maven is still required to build the artifacts, the Ant script is merely used
to sign the applet.

Basically, first build the entire thing using `mvn install` at the root of the
project. Then, go into this directory and run `ant`.

This will fail at first because it won't be able to sign the applet because the
private key isn't checked into source control for what should be obvious
reasons. In order to sign the applet, you first will need to create a keystore:

    keytool -genkeypair -alias uploader-applet -keystore uploader-applet.jks -validity 365

With the key created, you can now run the Ant script:

    ant -Dkeystore.password=<password>

Note that you can create a `build.properties` file to customize the name of the
keystore and other information used to sign the file. See
`build.example.properties` for details.

If your OS saves command history, you might want to disable that prior to
running the ANT command. Under bash, this is done with `unset HISTFILE`.

Note that this creates a self-signed applet, which causes users to get a scary
security warning when running the applet. The only way to avoid that is to get
an actual code-signing certificate that's verified by a certificate provider
that Java itself trusts.

Deploying to DoRI
-----------------

Use `ant deploy` to automatically update the version of the applet located in
the DORI directory.

Using the Applet
----------------

To upload files:

For DCM and IMG, files must be ZIP files, with any number of DCM and IMG files inside. 
The software will match the files automatically, as long as the filenames are preserved.
Fundus photos must be in another ZIP file, with the following format:
  [LATERALITY]_[MM]-[DD]-[YYYY] e.g. "L_03-23-2011", "R_11-04-2010"
All files in a single upload are assumed to be from the same patient. The software will
associate fundus photos with images captured on the same day as the fundus photo is named.