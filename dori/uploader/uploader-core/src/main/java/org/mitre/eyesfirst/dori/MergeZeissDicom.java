/*
 * Copyright 2013 The MITRE Corporation
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

package org.mitre.eyesfirst.dori;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.io.DicomInputStream;
import org.mitre.eyesfirst.FileCollection;
import org.mitre.eyesfirst.Uploader;
import org.mitre.eyesfirst.ui.ProgressMonitor;

/**
 * Merge Zeiss DICOM files with their unencrpyted imagery.
 * 
 * @author dsmiley
 * @author dpotter
 */
public class MergeZeissDicom {
	public MergeZeissDicom() {
	}

	/**
	 * Convert the given collection of DICOM and IMG files into DICOM files that
	 * can be used by the main system.
	 * 
	 * @param uploader
	 *            the uploader that's uploading these files, used to maintain
	 *            certain state
	 *            FIXME: Remove this parameter!
	 * @param files
	 *            the collection of DICOM and IMG files
	 * @param outDir
	 *            the output directory where generated DICOM files are stored
	 * @param xferSyntaxUid
	 *            the transfer syntax to use in the generated DICOM (for
	 *            example, JPEGLSLossless, {@code "1.2.840.10008.1.2.4.90"})
	 * @param efid
	 *            the EFID to use for the anonymized DICOM files
	 * @param progressMonitor
	 *            a progress monitor that is informed of progress
	 * @param dicomList
	 *            an input list that generated DicomObjects are added to
	 *            FIXME: why isn't this a return value?
	 * @throws IllegalArgumentException
	 *             if the transfer syntax is an invalid value
	 * @throws IOException
	 *             if an I/O error occurs during processing
	 */
	public static void convert(Uploader uploader, FileCollection files,
			File outDir, String xferSyntaxUid, String efid,
			ProgressMonitor progressMonitor, List<DicomObject> dicomList)
			throws IOException {
		TransferSyntax xferSyntax = TransferSyntax.valueOf(xferSyntaxUid);
		if (xferSyntax == null)
			throw new IllegalArgumentException("Bad transfer syntax " + xferSyntaxUid);

		progressMonitor.subTask("Gathering files...");
		List<String> dcmFiles = new ArrayList<String>();
		List<String> imgFiles = new ArrayList<String>();
		for (String name : files.getFileNames()) {
			if (name.toUpperCase(Locale.US).endsWith(".DCM")) {
				dcmFiles.add(name);
			} else if (name.toUpperCase(Locale.US).endsWith(".IMG")) {
				imgFiles.add(name);
			}
		}
		if (dcmFiles.size() == 0)
			return;
		if (imgFiles.size() == 0)
			return;
		// TODO:
		//assert imgFiles.collect {it.name}.unique().size() == imgFiles.size() //ensure names are unique

		progressMonitor.startTask(dcmFiles.size(), "Finding suitable DICOM files...");
		for (String dcmFile : dcmFiles) {
			DicomObject dco = null;
			System.out.print("Checking " + dcmFile + "... ");
			progressMonitor.subTask("Checking " + dcmFile + "...");
			// The DicomInputStream documentation doesn't tell you and doesn't
			// do it for you, but it does require the input stream be buffered
			DicomInputStream dicomInputStream = new DicomInputStream(
					new BufferedInputStream(files.openFileAsStream(dcmFile)));
			dco = dicomInputStream.readDicomObject();
			if (!dco.getString(Tag.MediaStorageSOPClassUID).equals("1.2.840.10008.5.1.4.1.1.12.77")) {
				System.out.println("skipped, wrong MediaStorageSOPClassUID");
				// Ignore this file
				progressMonitor.worked(1);
				continue;
			}
			dicomInputStream.close();
			//states macular cube
			String s = dco.getString(new int[] { Tag.PerformedProtocolCodeSequence,
				0,
				Tag.CodeMeaning});
			if (s == null || !s.startsWith("Macular Cube")) {
				progressMonitor.worked(1);
				System.out.println("skipped, wrong PerformedProtocolCodeSequence");
				continue;
			}
			// Check to see what the size is.
			int cols = dco.getInt(Tag.Columns);
			int rows = dco.getInt(Tag.Rows);
			int frames = dco.getInt(Tag.NumberOfFrames);
			if (cols != 1024 || rows != 512 || frames != 128) {
				progressMonitor.worked(1);
				System.out.println("skipped, wrong size (is " + cols + "x" + rows + "x" + frames + ")");
				continue;
			}
			System.out.print(" (input sized " + cols + "x" + rows + "x" + frames + ")");
			String privateTag = dco.getString(0x00570001);
			//System.out.println(dcmFile.path+" "+privateTag);
			if (!(privateTag.equals("1.2.276.0.75.2.2.40.6")/*corresponds to software version v4.5*/ ||
			privateTag.equals("1.2.826.0.1.3680043.2.139.3.6.6")/*" v3.0*/)) {
				System.out.println("warning: unknown software version detected (version is " + privateTag + ")");
			}
			progressMonitor.subTask("Processing " + dcmFile + "...");
			String partialName = Zeiss2Dcm.getPartialImgNameFromDcm(dco);
			String imgFile = null;
			for (String it : imgFiles) {
				if (it.endsWith("_cube_raw.img")  && it.contains(partialName)) {
					imgFile = it;
					break;
				}
			}
			if (imgFile == null) {
				System.out.println("FAILED, no matching imagery!");
				System.err.println("Error: couldn't find matching .img file; expected partial name " + partialName);
				progressMonitor.worked(1);
				continue;
			}
			Zeiss2Dcm dcm2dcm = new Zeiss2Dcm();
			// These do actually need to be files
			File dicomFileAsFile = files.openFile(dcmFile);
			File imageFileAsFile = files.openFile(imgFile);
			dcm2dcm.setImgFile(imageFileAsFile);
			dcm2dcm.setTransferSyntax(xferSyntax.uid());
			try {
				dicomList.add(dcm2dcm.convert(uploader, dicomFileAsFile, outDir, efid));
				System.out.println("OK");
			} catch (Exception e) {
				System.out.println("FAILED, " + e);
				e.printStackTrace();
			}
			progressMonitor.worked(1);
		}
	}
}
