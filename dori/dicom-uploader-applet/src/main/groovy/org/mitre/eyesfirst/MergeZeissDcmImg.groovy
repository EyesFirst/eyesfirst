/*
 * Copyright 2012 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.eyesfirst

import org.dcm4che2.data.DicomObject
import org.dcm4che2.data.Tag
import org.dcm4che2.data.TransferSyntax
import org.dcm4che2.io.DicomInputStream
import org.mitre.eyesfirst.dori.Zeiss2Dcm

class MergeZeissDcmImg {
	def static convert(File dcmDir, File imgDir, File outDir, xferSyntaxUid, efid, ProgressMonitor progressMonitor, List<DicomObject> dicomList) {
		//--get arguments and validate them

		assert dcmDir.isDirectory()
		assert imgDir.isDirectory()
		assert outDir.isDirectory()
		assert !outDir.list() //empty
		def xferSyntax = TransferSyntax.valueOf("$xferSyntaxUid") //e.g. JPEGLSLossless

		assert xferSyntax

		progressMonitor.subTask("Gathering DICOM files...")
		def dcmFiles = [];//File
		dcmDir.eachFileRecurse { File f ->
			if (f.name.toUpperCase() ==~ /.*\.DCM/)
				dcmFiles << f;
		}
		assert dcmFiles;

		progressMonitor.subTask("Gathering IMG files...")
		def imgFiles = [];//File
		imgDir.eachFileRecurse { File f ->
			if (f.name.toUpperCase() ==~ /.*\.IMG/) {
				imgFiles << f;
			}
		}
		assert imgFiles;
		assert imgFiles.collect {it.name}.unique().size() == imgFiles.size() //ensure names are unique

		progressMonitor.startTask(dcmFiles.size(), "Finding suitable DICOM files...")
		for (dcmFile in dcmFiles) {
			DicomObject dco = null;
			progressMonitor.subTask("Checking " + dcmFile.name + "...");
			new DicomInputStream(dcmFile).withStream {dicomInputSteam ->
				// read the object from input stream
				dco = dicomInputSteam.readDicomObject()
			}
			if (dco.getString(Tag.MediaStorageSOPClassUID) != "1.2.840.10008.5.1.4.1.1.12.77") {
				progressMonitor.worked(1)
				continue;
			}
			//states macular cube
			if (!dco.getString([
				Tag.PerformedProtocolCodeSequence,
				0,
				Tag.CodeMeaning]
			as int[]).startsWith("Macular Cube")) {
				progressMonitor.worked(1)
				continue;
			}
			def privateTag = dco.getString(0x00570001)
			//println dcmFile.path+" "+privateTag
			if (!(privateTag == "1.2.276.0.75.2.2.40.6"/*corresponds to software version v4.5*/ ||
			privateTag == "1.2.826.0.1.3680043.2.139.3.6.6"/*" v3.0*/)) {
				progressMonitor.worked(1)
				continue;
			}
			progressMonitor.subTask("Processing " + dcmFile.name + "...")
			def partialName = Zeiss2Dcm.getPartialImgNameFromDcm(dco);
			def imgFile = imgFiles.find {it.name.endsWith("_cube_raw.img") && it.name.contains(partialName)};
			if (!imgFile) {
				println " Error: couldn't find matching .img file; expected partial name $partialName";
				progressMonitor.worked(1)
				continue;
			}
			println "       and img: " + imgFile
			Zeiss2Dcm dcm2dcm = new Zeiss2Dcm();
			dcm2dcm.imgFile = imgFile;
			dcm2dcm.transferSyntax = xferSyntax.uid();
			try {
				dicomList << dcm2dcm.convert(dcmFile, outDir, efid)
			} catch (Exception e) {
				e.printStackTrace()
			}
			progressMonitor.worked(1)
		}
	}
}