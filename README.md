EyesFirst
=========

For more information about EyesFirst, please check out our website at
http://eyesfirst.github.io/ .

EyesFirst is currently split into four separate parts:

 * DORI
 * The new Clinical Decision Support User Interface
 * The EyesFirst retinal image classifier
 * The EyesFirst image processor

The four parts relate to one another, although conceptually DORI, the new
clinical decision support user interface, and the EyesFirst classifier can each
be used on their own.

The EyesFirst classifier is a MATLAB program that runs through a number of
statistical analysis to determine various features of a retinal image. It is
given a DICOM image, and processes it on its own.

The image processor is a web-based front end to the retinal image classifier,
and handles launching image processing on submitted DICOM images.

DORI is used to warehouse and search through retinal images, as well as view
them in an internal viewer. It by default submits images to the image processor
once they are uploaded, and can view the classifier results.

The new clinical support UI is a new UI intended to replace DORI that provides a
built-in method of viewing the information from the classifier.

Build Instructions
------------------

Before you can build the complete system, there are a few external libraries
that are required. You will need to build and install the DCM4CHE2 toolkit
version 2.0.27.

Download DCM4CHE2 from: http://www.dcm4che.org/confluence/display/d2/dcm4che2+DICOM+Toolkit

Use `mvn install` to build and install the DCM4CHE2 libraries.

To build the image processor, you must have MATLAB 2012a or later installed.
In order to make the MATLAB Java APIs available to the build script, you can
run a small ANT script to install your local `javabuilder.jar`.