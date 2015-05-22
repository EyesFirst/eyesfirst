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
package org.eyesfirst.trial

import static javax.servlet.http.HttpServletResponse.*
import grails.converters.JSON

import java.awt.image.BufferedImage

import javax.imageio.ImageIO

import org.codehaus.jackson.JsonFactory
import org.eyesfirst.trial.oct.SynthesizedFundusPhoto
import org.eyesfirst.trial.oct.ThicknessMap
import org.mitre.eyesfirst.dicom.DicomImage
import org.mitre.eyesfirst.dicom.DicomJSONConverter
import org.springframework.security.access.annotation.Secured

/**
 * Controller that handles loading slices.
 * @author dpotter
 *
 */
class OctController {
	def octImageService
	static JsonFactory jsonFactory = new JsonFactory();

	/**
	 * Does nothing.
	 * @return
	 */
	def index() { }

	/**
	 * Get information about an OCT scan, required to fetch the individual
	 * slices.
	 * @return
	 */
	@Secured(['ROLE_CLINICIAN'])
	def info() {
		DicomImage di = octImageService.loadDicomImage(params["id"])
		if (di == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		response.contentType = "text/plain"
		DicomJSONConverter.convertToViewerJSON(di.dicomObject, response.writer)
	}

	@Secured(['ROLE_CLINICIAN'])
	def classifierResults() {
		Artifact a = Artifact.findById(params["id"], [fetch:[hardExudates:"eager"]])
		if (a == null || a.classifierResults == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		// I will never understand why Grails doesn't bother making it easy to
		// exclude partial fields, but we can't just the default JSON converter
		// because we need to include all the hard exudates but not the artifact
		// itself, and we can't say "deep except for the artifact". Despite this
		// being an incredibly common use case.
		// So......
		def classifier = a.classifierResults;
		def result = [
			abnormalThicknessPFA: classifier.abnormalThicknessPFA,
			abnormalThicknessPD: classifier.abnormalThicknessPD,
			abnormalThicknessSpecificity: classifier.abnormalThicknessSpecificity,
			abnormalThicknessSensitivity: classifier.abnormalThicknessSensitivity,
			abnormalThicknessJointAnomStat: classifier.abnormalThicknessJointAnomStat,
			hardExudatesPFA: classifier.hardExudatesPFA,
			hardExudatesPD: classifier.hardExudatesPD
		];
		// Now generate the hard exudate entries...
		result["hardExudates"] = classifier.hardExudates.collect {
			[
				maxCfarValue: it.maxCfarValue,
				normalScore: it.normalScore,
				layer: it.layer,
				numVoxels: it.numVoxels,
				layerProportion: it.layerProportion,
				boundingBox: [
					x: it.boundingBoxX,
					y: it.boundingBoxY,
					z: it.boundingBoxZ,
					width: it.boundingBoxWidth,
					height: it.boundingBoxHeight,
					depth: it.boundingBoxDepth
				]
			]
		}
		render result as JSON
	}

	/**
	 * Get a single slice from a scan.
	 * @return
	 */
	@Secured(['ROLE_CLINICIAN'])
	def slice() {
		// The data in this case is a string that is the DICOM ID to send to the
		// WADO service
		DicomImage dicom = octImageService.loadDicomImage(params["id"])
		if (dicom == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		int slice = params.int("slice")
		// TODO: Cache slices
		// Grab the last modified time and if-modified-since now so we
		// don't have to regenerate the content
		long lastModified = dicom.lastModifiedTime
		long time = request.getDateHeader("If-Modified-Since")
		if (lastModified > Long.MIN_VALUE && time >= lastModified) {
			// Not changed, use cached and don't bother reconverting
			//log.debug "$studyUID/$seriesUID/$objectUID not changed, returning not modified"
			response.setDateHeader("Last-Modified", lastModified);
			response.status = SC_NOT_MODIFIED
			// Otherwise Grails looks for the view and helpfully 404s us
			render "Not modified"
			return
		}
		BufferedImage image = dicom.getSlice(slice-1)
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
			def id = params["id"]
			log.error "Unable to export JPEG for artifact $id, slice $slice"
		}
	}

	@Secured(['ROLE_CLINICIAN'])
	def thicknessMap() {
		Artifact a = Artifact.findById(params["id"])
		if (a == null || a.thicknessMap == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		ThicknessMap tm = a.thicknessMap
		response.contentType = tm.mimeType
		response.setDateHeader("Last-Modified", tm.lastModified.time)
		response.outputStream.write(tm.data)
	}

	@Secured(['ROLE_CLINICIAN'])
	def synthesizedFundus() {
		Artifact a = Artifact.findById(params["id"])
		if (a == null || a.synthesizedFundusPhoto == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		SynthesizedFundusPhoto photo = a.synthesizedFundusPhoto
		response.contentType = photo.mimeType
		response.setDateHeader("Last-Modified", photo.lastModified.time)
		response.outputStream.write(photo.data)
	}
}
