Test Data
=========

Sample patient names are stored in patients.csv and that is used to populate
the initial patients list.

Artifacts are stored in artifacts.csv. See artifacts_example.csv for an example
of the contents of that file. Paths for files are relative to the testData
directory.

The "visit date" for the given artifact applies only to the fundus
photo entry and is ignored for the OCT scan. (Instead, the date stored in the
DICOM file is used for the OCT scan.)

Note that the parser for these CSV file is extremely simplistic and does not
support quotes around strings!