#!/bin/sh
#
# Copyright 2012 The MITRE Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# This provides an example of running the runner straight from the command line.
#
# The current script is designed to be run under Mac OS X and will require
# modification to work under Linux.
#

#
# Basic configuration:
#
MATLAB_HOME=/Applications/MATLAB_R2011b.app

DATA_DIR=$TEMP/eyesfirst
WADO_URL='http://localhost:8888/wado?requestType=WADO&contentType=application/dicom&studyUID=1.2.826.0.1.3680043.2.139.3.6.33166195690.200803141441141090&seriesUID=1.2.826.0.1.3680043.2.139.3.6.33166195690.200803141441141400&objectUID=1.2.826.0.1.3680043.8.1302.20080314144114.1671730634998957899'
DORIWEB_URL="http://localhost:8080/doriweb/upload/processed"
DCM4CHEE_URL="DCM4CHEE@localhost"

export DYLD_LIBRARY_PATH=$MATLAB_HOME/runtime/maci64:$MATLAB_HOME/bin/maci64:$MATLAB_HOME/sys/os/maci64
export XAPPLRESDIR=$MATLAB_HOME/X11/app-defaults

java -jar target/eyesfirst-classifier-runner-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
-o "$DATA_DIR" -c "$DORIWEB_URL" -d "$DCM4CHEE_URL" "$WADO_URL"
