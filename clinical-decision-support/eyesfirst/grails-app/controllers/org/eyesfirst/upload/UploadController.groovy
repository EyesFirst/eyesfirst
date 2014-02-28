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
package org.eyesfirst.upload

//import static groovyx.net.http.ContentType.*
//import static groovyx.net.http.Method.*
import static javax.servlet.http.HttpServletResponse.*
import groovyx.net.http.*

import javax.imageio.ImageIO
import javax.imageio.ImageReader

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream
import org.eyesfirst.trial.Artifact
import org.eyesfirst.trial.Patient
import org.mitre.eyesfirst.common.MimeTypes
import org.mitre.eyesfirst.dicom.DicomID
import org.mitre.eyesfirst.dicom.DicomMetadataUtil;
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

class UploadController {
	def dicomUploadService
	def octImageService
	def fundusImageService

	def index() {
	}

	def receive() {
		log.info("Receive called");

		if (request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request
			String efid = mpr.getParameter("efid")
			// FIXME: EFIDs are currently "just strings." As far as we're concerned,
			// it's the patient's "last name".
			if (efid == null) {
				response.status = SC_BAD_REQUEST
				render "No EFID given"
				return
			}
			// Check to see if we have a patient with the last name of the given EFID
			def patient = Patient.findByLastName(efid);
			boolean fillPatientDataFromDicom = false;
			if (patient == null) {
				// We do not. Create the patient.
				fillPatientDataFromDicom = true;
				patient = new Patient();
				patient.lastName = efid;
			}

			for (Map.Entry<String, MultipartFile> item : mpr.fileMap) {
				File uploadedFile = File.createTempFile("DCM", null);
				item.value.transferTo(uploadedFile);

				if (sendToPacsServer(uploadedFile)) {
					log.info("Successful upload to PACS server");
					log.info(item.key)
					// Grab the IDs out of the file
					DicomObject dicomObject = new DicomInputStream(new FileInputStream(uploadedFile)).readDicomObject();
					DicomID dcmId = new DicomID(dicomObject);
					if (fillPatientDataFromDicom) {
						// Fill in the missing DICOM data.
						patient.gender = DicomMetadataUtil.getGender(dicomObject);
						patient.birthday = DicomMetadataUtil.getBirthday(dicomObject);
						patient.save(failOnError: true);
						fillPatientDataFromDicom = false;
					}
					log.info("Received DICOM file " + dcmId.studyUID + ", " + dcmId.seriesUID + ", " + dcmId.objectUID);

					// Create the DICOM artifact
					octImageService.importOCT(patient, dcmId);

					def url = grailsApplication.config.eyesfirst.imageProcessorService
					log.info("Notifying processor via " + url)
					notifyProcessor(new URL(url), patient.id as String, dcmId.studyUID, dcmId.seriesUID, dcmId.objectUID)
				} else {
					log.error("PACS upload unsuccessful");
				}
				uploadedFile.delete();
			}
			render(contentType: "text/json") { success = true; }
		} else {
			response.status = SC_BAD_REQUEST
			render "Not a multipart request"
		}
	}

	def fundus() {
		// Does nothing (this is for a UI view)
	}

	def uploadDCM() {
		// Also does nothing (upload form)
	}

	def receiveFundus() {
		// Currently does nothing. Exists solely as an end point to receive an
		// empty message from the applet.
		render "Currently disabled"
	}
/*
 * Disabled for now. Integrating the fundus upload with the rest of the process
 * is kind of complicated.
	def receiveFundus() {
		if (request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request
			for (Map.Entry<String, MultipartFile> item : mpr.fileMap) {
				// Fun new discovery: Spring's MultipartFile will merrily return
				// "multipart/form-data" for the MIME type. Grr.
				MultipartFile mpFile = item.value
				String mime = mpFile.contentType
				byte[] data = item.value.bytes
				if (mime == null || (!mime.startsWith("image/"))) {
					// Just ignore this, it's wrong.
					// Step 1: try and guess the expected mime type from the
					// file name, if we have that
					String name = mpFile.originalFilename
					if (name != null) {
						mime = MimeTypes.getMimeType(name);
					}
					if (mime == null || (!mime.startsWith("image/"))) {
						log.warn("Failed to find a MIME type for file \"" + name + "\" and no suitable one was provided by the browser, falling back on ImageIO")
						// Step 2: THAT didn't work, try again using ImageIO
						mime = null
						try {
							Iterator<ImageReader> readers = ImageIO.getImageReaders(ImageIO.createImageInputStream(new ByteArrayInputStream(data)))
							while (mime == null && readers.hasNext()) {
								ImageReader reader = readers.next()
								String[] mimes = reader.originatingProvider.MIMETypes
								if (mimes != null && mimes.length > 0) {
									// Just use the first, we have no way of knowing what the "best" is
									mime = mimes[0]
									break
								}
							}
							if (mime == null) {
								log.warn("No ImageIO plugin found that provided a MIME type, using " + MimeTypes.BINARY_MIME_TYPE)
								mime = MimeTypes.BINARY_MIME_TYPE
							}
						} catch (Exception e) {
							log.warn("Unable to find a MIME type using standard lookups, fell back to ImageIO, which raised an exception", e)
						}
					}
				}
				log.info("Uploading fundus photo...")
				FundusPhoto photo = new FundusPhoto(imageData: data, format: mime).save()

				for(String s : item.key.split(",")) {
					log.info("query string: " + s)
					def image = DicomImage.findByRawQueryString(s)
					image.fundusPhoto = photo
					image.save()
				}
			}
			render "Success!"
		}
	}
*/
	static void notifyProcessor(URL url, String key, String studyUID, String seriesUID, String objectUID) {
		//System.out.println("POST to " + url);
		HttpURLConnection urlConnection = url.openConnection()
		urlConnection.setRequestMethod("POST")
		urlConnection.setRequestProperty("Content-Type","application/x-www-form-urlencoded")
		urlConnection.setDoOutput(true)
		ByteArrayOutputStream bytes = new ByteArrayOutputStream()
		Writer formWriter = new OutputStreamWriter(bytes, "UTF-8")
		formWriter.write("key=" + URLEncoder.encode(key, "UTF-8"))
		formWriter.write("&studyUID=" + URLEncoder.encode(studyUID, "UTF-8"))
		formWriter.write("&seriesUID=" + URLEncoder.encode(seriesUID, "UTF-8"))
		formWriter.write("&objectUID=" + URLEncoder.encode(objectUID, "UTF-8"))
		formWriter.flush()
		byte[] buf = bytes.toByteArray()
		urlConnection.setRequestProperty("Content-Length", Integer.toString(buf.length))
		OutputStream urlOut = urlConnection.getOutputStream()
		urlOut.write(buf)
		urlOut.flush()
		//System.out.println("Form data: ");
		//System.out.write(buf);
		//System.out.println();
		//System.out.println("Connect");
		urlConnection.connect()
		InputStream res = urlConnection.getInputStream()
		buf = new byte[1024]
		//System.out.println("Result: ");
		while (true) {
			int r = res.read(buf)
			if (r < 0)
				break
			//System.out.write(buf, 0, r)
		}
		//System.out.println();
		//System.out.println("Done.");
	}

	def processed() {
		if(request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request;
			// We no longer tie the result back to the original image. So we
			// create an entirely new artifact based on the DICOM id coming in.
			// However, we do need to tie it back to the patient, and we do that
			// using the value in the "key" field.
			def patientKey = mpr.getParameter("key");
			if (patientKey == null) {
				response.status = SC_BAD_REQUEST;
				render "Missing key value (required to tie results back to the original record)";
				return;
			}
			Patient patient = Patient.get(patientKey);
			if (patient == null) {
				// If there is no patient, we can't store this artifact.
				response.status = SC_NOT_FOUND;
				render "No patient found for patient key \"" + patientKey + "\"";
				return;
			}
			// We have to receive a DICOM ID to do anything.
			def v = mpr.getParameter("processedDicomId")
			if (v == null) {
				response.status = SC_BAD_REQUEST;
				render "Missing dicom ID";
				return;
			}
			// Generate the artifact
			Artifact artifact = octImageService.importOCT(patient, v);
			// See if we have a fundus photo.
			MultipartFile fundus = mpr.getFile("fundusPhoto");
			if (fundus != null) {
				octImageService.importSynthesizedFundusPhoto(artifact, new Date(), fundus.contentType, fundus.bytes);
			}

			v = mpr.getParameter("classifierDiagnoses")
			if (v != null) {
				octImageService.importClassifierResults(artifact, new StringReader(v));
			}
			MultipartFile thicknessMap = mpr.getFile("thicknessMap")
			if (thicknessMap != null) {
				octImageService.importThicknessMap(artifact, new Date(), thicknessMap.contentType, thicknessMap.bytes);
			}
		}
	}

	private boolean sendToPacsServer(File f) {
		log.info("Sending file to PACS server...")
		try {
			dicomUploadService.uploadDicomFile(f)
			return true
		} catch (Exception e) {
			log.error("Failed to upload to PACS", e)
			return false
		}
	}
}
