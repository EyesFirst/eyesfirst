EyesFirst
=========

For more information about EyesFirst, please check out our website at
http://eyesfirst.github.io/ .

EyesFirst is currently split into three separate parts:

 * DORI
 * The EyesFirst retinal image classifier
 * The EyesFirst image processor

The three parts relate to one another, although conceptually DORI and the
EyesFirst classifier can be used on their own.

The EyesFirst classifier is a MATLAB program that runs through a number of
statistical analysis to determine various features of a retinal image. It is
given a DICOM image, and processes it on its own.

The image processor is a web-based front end to the retinal image classifier,
and handles launching image processing on submitted DICOM images.

DORI is used to warehouse and search through retinal images, as well as view
them in an internal viewer. It by default submits images to the image processor
once they are uploaded, and can view the classifier results.
