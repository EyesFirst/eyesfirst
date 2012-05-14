To create the Applet file, use the Maven build script:

	mvn package

This will fail because it won't be able to sign the applet. In order to sign the
applet, you first will need to create a keystore:

	keytool -genkeypair -alias DicomUploader -keystore DcmUploader.keystore -validity 365

Once that's done, you can build the applet with the following command:

	mvn -Djarsigner.storepass=<password> -Djarsigner.keypass=<password> package

This will create and sign the JAR files in the "target" directory.

******************************

To upload files:

For DCM and IMG, files must be ZIP files, with any number of DCM and IMG files inside. 
The software will match the files automatically, as long as the filenames are preserved.
Fundus photos must be in another ZIP file, with the following format:
  [LATERALITY]_[MM]-[DD]-[YYYY] e.g. "L_03-23-2011", "R_11-04-2010"
All files in a single upload are assumed to be from the same patient. The software will
associate fundus photos with images captured on the same day as the fundus photo is named.
