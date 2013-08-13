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
package org.eyesfirst.dori

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import static javax.servlet.http.HttpServletResponse.*
import groovyx.net.http.*

import javax.imageio.ImageIO
import javax.imageio.ImageReader

import org.dcm4che2.io.DicomInputStream
import org.mitre.eyesfirst.common.MimeTypes
import org.mitre.eyesfirst.dicom.DicomID
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

class UploadController {
	static Random = new Random();

	def springSecurityService
	def solrService
	def dicomUploadService

	def index() { log.info("Serving upload applet") }

	def receive() {
		log.info("Receive called");

		if(request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request
			String efidString = mpr.getParameter("efid")
			// FIXME: What was this for?
			//String timestampString = mpr.getParameter("timestamp")
			def efid = Efid.findById(efidString)
			if (efid == null) {
				response.status = SC_BAD_REQUEST
				render "No EFID given or EFID does not exist"
				return
			}

			for (Map.Entry<String, MultipartFile> item : mpr.fileMap) {
				File uploadedFile = File.createTempFile("DCM", null);
				item.value.transferTo(uploadedFile);

				if (sendToPacsServer(uploadedFile)) {
					log.info("Successful upload to PACS server");
					log.info(item.key)
					// Grab the IDs out of the file
					DicomID dcmId = new DicomID(new DicomInputStream(new FileInputStream(uploadedFile)).readDicomObject())
					log.info("Received DICOM file " + dcmId.studyUID + ", " + dcmId.seriesUID + ", " + dcmId.objectUID);

					def dicomImage = new DicomImage(rawQueryString: dcmId.toQueryString(), objectUid: dcmId.objectUID)
					efid.addToImages(dicomImage)
					efid.save(failOnError: true, flush: true)

					def url = grailsApplication.config.eyesfirst.imageProcessorUrl
					log.info("Notifying processor via " + url)
					notifyProcessor(new URL(url), dcmId.studyUID, dcmId.seriesUID, dcmId.objectUID)
				} else {
					log.error("PACS upload unsuccessful");
				}
				uploadedFile.delete();
			}
			solrService.updateSolr()
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

	static void notifyProcessor(URL url, String studyUID, String seriesUID, String objectUID) {
		//System.out.println("POST to " + url);
		HttpURLConnection urlConnection = url.openConnection()
		urlConnection.setRequestMethod("POST")
		urlConnection.setRequestProperty("Content-Type","application/x-www-form-urlencoded")
		urlConnection.setDoOutput(true)
		ByteArrayOutputStream bytes = new ByteArrayOutputStream()
		Writer formWriter = new OutputStreamWriter(bytes, "UTF-8")
		formWriter.write("studyUID=" + studyUID)
		formWriter.write("&seriesUID=" + seriesUID)
		formWriter.write("&objectUID=" + objectUID)
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
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request
			DicomImage imageEntry = DicomImage.findByRawQueryString(mpr.getParameter("rawQueryString"))

			if (imageEntry == null) {
				log.warn("Received processed response for non-existant image " + mpr.getParameter("rawQueryString") + "!")
				response.sendError(SC_NOT_FOUND)
				return
			}

			// The processor may call with incomplete results. Only update what
			// we actually have.
			MultipartFile fundus = mpr.getFile("fundusPhoto")
			if (fundus != null) {
				FundusPhoto synth = new FundusPhoto(format: mpr.contentType, imageData: mpr.getFile("fundusPhoto").bytes)
				synth.save()
				imageEntry.synthesizedFundusPhoto = synth
			}

			MultipartFile bloodVesselFundus = mpr.getFile("bloodVesselFundus")
			if (bloodVesselFundus != null) {
				FundusPhoto bvFundus = new FundusPhoto(format: bloodVesselFundus.contentType, imageData: bloodVesselFundus.bytes)
				bvFundus.save()
				imageEntry.bloodVesselFundus = bvFundus
			}

			String v = mpr.getParameter("classifierDiagnoses")
			if (v != null)
				imageEntry.classifierDiagnoses = v
			v = mpr.getParameter("processedQueryString")
			if (v != null)
				imageEntry.processedQueryString = v
			MultipartFile thicknessMap = mpr.getFile("thicknessMap")
			if (thicknessMap != null)
				imageEntry.thicknessMap = thicknessMap.bytes
			imageEntry.save(flush: true)

			solrService.updateSolr()
		}
	}

	def clinicalInterpretation = {
		DicomImage imageEntry = DicomImage.findByRawQueryString(request.getParameter("rawQueryString"))
		if (imageEntry == null) {
			response.sendError(SC_NOT_FOUND)
		} else {
			if (request.getParameter("method") == "PUT") {
				imageEntry.setClinicalInterpretation(request.getParameter("clinicalInterpretation"))
				imageEntry.save()
			} else if (request.getParameter("method") == "GET") {
				response.setHeader("Content-Type", "application/json")
				def output = response.outputStream
				output.write(imageEntry.getClinicalInterpretation())
				output.close()
			}
		}
	}

	def clinicalFeedback = {
		DicomImage imageEntry = DicomImage.findByRawQueryString(request.getParameter("processedQueryString"))
		if (imageEntry == null) {
			response.sendError(SC_NOT_FOUND)
		} else {
			if (request.getParameter("method") == "PUT") {
				imageEntry.setClinicalFeedback(request.getParameter("clinicalFeedback"))
				imageEntry.save()
			} else if (request.getParameter("method") == "GET") {
				response.setHeader("Content-Type", "application/json")
				def output = response.outputStream
				output.write(imageEntry.getClinicalFeedback())
				output.close()
			}
		}
	}

	def classifierDiagnoses = {
		DicomImage imageEntry = DicomImage.findByProcessedQueryString(request.getParameter("processedQueryString"))
		if (imageEntry == null) {
			response.sendError(SC_NOT_FOUND)
		} else {
			response.setHeader("Content-Type", "application/json")
			response.writer.print(imageEntry.getClassifierDiagnoses())
			response.writer.close()
		}
	}

	def thicknessMap = {
		DicomImage imageEntry = DicomImage.findByProcessedQueryString(request.getParameter("processedQueryString"))
		if (imageEntry == null || imageEntry.getThicknessMap() == null) {
			response.sendError(SC_NOT_FOUND)
		} else {
			response.setHeader("Content-Type", "image/png")
			def output = response.outputStream
			output.write(imageEntry.getThicknessMap())
			output.close()
		}
	}

	def fundusPhoto = {
		DicomImage imageEntry
		if(request.getParameter("rawQueryString") != null) {
			imageEntry = DicomImage.findByRawQueryString(request.getParameter("rawQueryString"))
		} else if (request.getParameter("processedQueryString") != null) {
			imageEntry = DicomImage.findByProcessedQueryString(request.getParameter("processedQueryString"))
		}
		if (imageEntry == null || imageEntry.fundusPhoto == null) {
			response.sendError(SC_NOT_FOUND)
		} else {
			response.setHeader("Content-Type", imageEntry.fundusPhoto.format)
			def output = response.outputStream
			output.write(imageEntry.fundusPhoto.getImageData())
			output.close()
		}
	}

	def synthesizedFundusPhoto = {
		DicomImage imageEntry = DicomImage.findByProcessedQueryString(request.getParameter("processedQueryString"))
		if (imageEntry == null || imageEntry.synthesizedFundusPhoto == null) {
			response.sendError(SC_NOT_FOUND)
		} else {
			response.setHeader("Content-Type", imageEntry.synthesizedFundusPhoto.format)
			def output = response.outputStream
			output.write(imageEntry.synthesizedFundusPhoto.getImageData())
			output.close()
		}
	}

	private boolean sendToPacsServer (File f) {
		try {
			dicomUploadService.uploadDicomFile(f)
			return true
		} catch (Exception e) {
			log.warn("Failed to upload to PACS", e)
			return false
		}
	}
}
