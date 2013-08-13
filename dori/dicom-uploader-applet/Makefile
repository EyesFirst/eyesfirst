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
# This makefile is provided as an example of how to generate and sign the
# applet JAR file. Note that is will copy the applet JAR into the dori-web
# project, assuming both projects were checked out as-is.
#

all: copy

ARTIFACT_NAME=dcm-uploader
ARTIFACT_VERSION=0.0.1-SNAPSHOT
ARTIFACT_JAR=$(ARTIFACT_NAME)-$(ARTIFACT_VERSION)
DORI_PATH=../../dori-web/trunk
DORI_APPLET_PATH=$(DORI_PATH)/web-app/upload/applet/
STOREPASS=password
KEYPASS=password

clean:
	mvn clean
	rm -f "$(DORI_APPLET_PATH)/*.jar"

rebuild: clean all

target/applet/$(ARTIFACT_JAR):
	mvn -Djarsigner.keypass=$(KEYPASS) -Djarsigner.storepass=$(STOREPASS) package

copy: target/applet/$(ARTIFACT_JAR)
	mkdir -p "$(DORI_APPLET_PATH)"
	cp -v target/applet/*.jar "$(DORI_APPLET_PATH)"

.PHONY: all copy clean rebuild
