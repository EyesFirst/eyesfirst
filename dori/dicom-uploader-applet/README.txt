As the uploader system is a "child" of the uploader applet, it is no longer
signed as part of the default Maven build. This proved to be too obnoxious, as
not having set up the applet for signing would cause the build to fail. Instead,
the applet is now built via an Ant script.

Maven is still required to build the artifacts, the Ant script is merely used
to sign the applet.

Basically, first build the entire thing using `mvn package` at the root of the
project. Then, go into this directory and run `ant`.

This will fail at first because it won't be able to sign the applet because the
private key isn't checked into source control for what should be obvious
reasons. In order to sign the applet, you first will need to create a keystore:

    keytool -genkeypair -alias DicomUploader -keystore DcmUploader.keystore -validity 365

Once that's done, you can build the applet with the following command:

    mvn -Djarsigner.storepass=<password> -Djarsigner.keypass=<password> package

This will create and sign the JAR files in the "target" directory.

Note that this creates a self-signed applet, which causes users to get a scary
security warning when running the applet. The only way to avoid that is to get
an actual code-signing certificate that's verified by a certificate provider
that Java itself trusts.

--------------------------------------------------------------------------------

To upload files:

For DCM and IMG, files must be ZIP files, with any number of DCM and IMG files inside. 
The software will match the files automatically, as long as the filenames are preserved.
Fundus photos must be in another ZIP file, with the following format:
  [LATERALITY]_[MM]-[DD]-[YYYY] e.g. "L_03-23-2011", "R_11-04-2010"
All files in a single upload are assumed to be from the same patient. The software will
associate fundus photos with images captured on the same day as the fundus photo is named.