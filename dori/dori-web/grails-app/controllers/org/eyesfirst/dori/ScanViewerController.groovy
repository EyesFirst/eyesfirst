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

import static javax.servlet.http.HttpServletResponse.*

import java.awt.image.BufferedImage

import javax.imageio.ImageIO
import javax.servlet.http.HttpServletResponse

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonGenerator;
import org.mitre.eyesfirst.dicom.DicomImage
import org.mitre.eyesfirst.dicom.DicomJSONConverter
import org.mitre.eyesfirst.dicom.image.CachingDicomAccessService

/**
 * Controller for retrieving DICOM metadata and slices.
 * @author dpotter
 */
class ScanViewerController {
	private static JsonFactory jsonFactory = new JsonFactory()
	def dicomAccessService

	def index() {
	}

	def debug() {
		// Provide basic information about the current JVM
		response.contentType = "text/plain";
		render "Supported image formats:\n";
		for (String format : ImageIO.getReaderFormatNames()) {
			render format;
			render '\n';
		}
	}

	def clearCache() {
		if (dicomService instanceof CachingDicomAccessService) {
			((CachingDicomAccessService)dicomService).clearCache();
			render "Cleared";
		} else {
			render "Not using a cache";
		}
	}

	def slices() {
		def studyUID = params.studyUID
		def seriesUID = params.seriesUID
		def objectUID = params.objectUID
		def sliceType = params.sliceType
		int sliceNumber = params.sliceNumber as int
		// Grab the series we want a slice from
		log.debug "Fetching slice $studyUID/$seriesUID/$objectUID, slice $sliceNumber"
		DicomImage dicom = dicomAccessService.retrieveDicomObject(studyUID, seriesUID, objectUID)
		// Finally, grab the slice. Right now the slice type is ignored.
		if (dicom == null) {
			// Not found
			log.debug "$studyUID/$seriesUID/$objectUID not found"
			response.sendError(SC_NOT_FOUND)
		} else {
			// Grab the last modified time and if-modified-since now so we
			// don't have to regenerate the content
			long lastModified = dicom.lastModifiedTime
			long time = request.getDateHeader("If-Modified-Since")
			if (lastModified > Long.MIN_VALUE && time >= lastModified) {
				// Not changed, use cached and don't bother reconverting
				log.debug "$studyUID/$seriesUID/$objectUID not changed, returning not modified"
				response.setDateHeader("Last-Modified", lastModified);
				response.status = SC_NOT_MODIFIED
				// Otherwise Grails looks for the view and helpfully 404s us
				render "Not modified"
				return
			}
			BufferedImage image = dicom.getSlice(sliceNumber-1)
			if (image != null) {
				if (lastModified != Long.MIN_VALUE)
					response.setDateHeader("Last-Modified", lastModified)
				response.contentType = "image/jpeg"
				ByteArrayOutputStream buf = new ByteArrayOutputStream(10*1024)
				ImageIO.write(image, "jpeg", buf)
				image.flush()
				byte[] bytes = buf.toByteArray()
				response.contentLength = bytes.length
				response.outputStream.write(bytes)
			} else {
				response.sendError(SC_INTERNAL_SERVER_ERROR)
				log.error "Unable to export JPEG for image $studyUID/$seriesUID/$objectUID, slice $sliceNumber"
			}
		}
	}

	def info() {
		def studyUID = params.studyUID
		def seriesUID = params.seriesUID
		def objectUID = params.objectUID
		log.info "Checking $studyUID/$seriesUID/$objectUID"
		// Grab the dicom object
		DicomImage dicom = dicomAccessService.retrieveDicomObject(studyUID, seriesUID, objectUID)
		if (dicom == null) {
			response.sendError(SC_NOT_FOUND)
		} else {
			// I suppose I did this to myself by not checking the names already
			// used when building the DICOM image retrieval service:
			def doriImage = org.eyesfirst.dori.DicomImage.findByDicomID(studyUID, seriesUID, objectUID)
			StringWriter str = new StringWriter()
			JsonGenerator generator = jsonFactory.createJsonGenerator(str)
			generator.writeStartObject() // {
			generator.writeStringField("efid", doriImage?.efid?.id)
			generator.writeFieldName("metadata")
			DicomJSONConverter.convertToViewerJSON(dicom.getDicomObject(), generator)
			generator.writeEndObject() // }
			generator.flush()
			render str.toString()
		}
	}
}
