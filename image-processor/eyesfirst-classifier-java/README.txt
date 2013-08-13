This is the EyesFirst classifier Java runner - the runner provides glue code
that allows the Image Processor Webapp to run the classifier as a separate
process. It handles downloading the DICOM image from a WADO instance, running
the EyesFirst classifier, then (assuming it completed successfully), uploading
the various artifacts to DCM4CHEE and back to DORI Web.