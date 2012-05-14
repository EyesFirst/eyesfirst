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

import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import groovyx.net.http.*

import groovyx.net.http.HTTPBuilder

import java.io.IOException
import java.io.OutputStreamWriter

import static javax.servlet.http.HttpServletResponse.*

import org.dcm4che2.tool.dcmsnd.DcmSnd
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest


class UploadController {

	static Random = new Random();

	def springSecurityService

	def index = { log.info("Serving upload applet") }

	def receive = {
		log.info("Receive called");

		if(request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request
			String efidString = mpr.getParameter("efid")
			String timestampString = mpr.getParameter("timestamp")

			for (Map.Entry<String, MultipartFile> item : mpr.fileMap) {
				File uploadedFile = File.createTempFile("DCM", null);
				item.value.transferTo(uploadedFile);

				String[] wadoParams = item.key.split("&")
				Map<String, String> uidMap = new HashMap<String, String>()
				for(String s : wadoParams) {
					log.info(s)
					String[] pair = s.split("=")
					uidMap.put(pair[0], pair[1])
				}

				if (sendToPacsServer(uploadedFile)) {
					log.info("Successful upload to PACS server");
					log.info(item.key)

					log.info(uidMap.get("objectUID"))
					def dicomImage = new DicomImage(rawQueryString: item.key, objectUid: uidMap.get("objectUID"))
					def efid = Efid.findById(efidString)
					efid.addToImages(dicomImage)
					efid.save(failOnError: true, flush: true)

					def url = grails.util.GrailsConfig['eyesfirst.imageProcessorUrl']
					log.info("Notifying processor via " + url)
					notifyProcessor(new URL(url), uidMap.get("studyUID"), uidMap.get("seriesUID"), uidMap.get("objectUID"))
				} else {
					log.error("PACS upload unsuccessful");
				}
				uploadedFile.delete();
			}
			updateSolr()
			render(contentType: "text/json") { success = true; }
		} else {
			response.status = SC_BAD_REQUEST
			render "Not a multipart request"
		}
	}

	def receiveFundus = {
		if(request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request
			for (Map.Entry<String, MultipartFile> item : mpr.fileMap) {
				FundusPhoto photo = new FundusPhoto(imageData: item.value.getBytes(), format: item.value.getContentType()).save()

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

	def processed = {
		if(request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request
			DicomImage imageEntry = DicomImage.findByRawQueryString(mpr.getParameter("rawQueryString"))

			FundusPhoto synth = new FundusPhoto(format: "image/png", imageData: mpr.getFile("fundusPhoto").getBytes())
			synth.save()

			imageEntry.setClassifierDiagnoses(mpr.getParameter("classifierDiagnoses"))
			imageEntry.setProcessedQueryString(mpr.getParameter("processedQueryString"))
			imageEntry.setThicknessMap(mpr.getFile("thicknessMap").getBytes())
			imageEntry.setSynthesizedFundusPhoto(synth)
			imageEntry.save(flush: true)

			updateSolr()
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

	def boolean sendToPacsServer (File f) {

		def REMOTE_AE = "DCM4CHEE"

		DcmSnd dcmsnd = new DcmSnd("DCMSND");
		dcmsnd.setCalledAET(REMOTE_AE);
		dcmsnd.setRemoteHost(grails.util.GrailsConfig['eyesfirst.dcm4cheeHost']);
		dcmsnd.setRemotePort(grails.util.GrailsConfig['eyesfirst.dcm4cheePort']);

		dcmsnd.addFile(f);

		dcmsnd.setOfferDefaultTransferSyntaxInSeparatePresentationContext(false);
		dcmsnd.setSendFileRef(false);
		dcmsnd.setStorageCommitment(false);
		dcmsnd.setPackPDV(true);
		dcmsnd.setTcpNoDelay(true);

		dcmsnd.configureTransferCapability();
		try {
			dcmsnd.start();
		} catch (Exception e) {
			System.err.println("ERROR: Failed to start server for receiving");
			return false;
		}

		try {
			long t1 = System.currentTimeMillis();
			dcmsnd.open();
			long t2 = System.currentTimeMillis();
			System.out.println("Connected to " + REMOTE_AE + " in "
					+ ((t2 - t1) / 1000F)
					+ "s");

			dcmsnd.send();
			dcmsnd.close();
			System.out.println("Released connection to " + REMOTE_AE);
		} catch (IOException e) {
			System.err.println("ERROR: Failed to establish association:"
					+ e.getMessage());
			return false;
		} catch (InterruptedException e) {
			System.err.println("ERROR: Failed to establish association:"
					+ e.getMessage());
			return false;
		} finally {
			dcmsnd.stop();
		}

		return true;
	}

	def String updateSolr() {
		def http = new HTTPBuilder(grails.util.GrailsConfig['eyesfirst.solrUpdate'])
		http.request(POST, XML) {
			response.success = {resp, xml -> return resp.statusLine }
		}
	}
}
