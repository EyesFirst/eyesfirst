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
package org.eyesfirst.dori

import static javax.servlet.http.HttpServletResponse.*

import grails.converters.JSON

class ImageAnnotationController {
	def springSecurityService

	static allowedMethods = [update:'POST',
							 create:'POST',
							 remove:'POST']

	static String createURL(studyUID, seriesUID, objectUID) {
		return "studyUID=" + studyUID + "&seriesUID=" + seriesUID + "&objectUID=" + objectUID
	}

	def findImageByURL(studyUID, seriesUID, objectUID) {
		def url = createURL(studyUID, seriesUID, objectUID)
		log.info("Looking up " + url)
		// Look up the object
		def processed = false
		DicomImage dicom = DicomImage.findByRawQueryString(url)
		if (dicom == null) {
			log.info("Unprocessed not found, trying processed")
			dicom = DicomImage.findByProcessedQueryString(url)
			if (dicom == null) {
				log.info("Nothing found")
				return [ null, null ]
			}
			processed = true
		}
		return [ dicom, processed ]
	}

	private static ImageAnnotation findAnnotationById(studyUID, seriesUID, objectUID, annotationID) {
		ImageAnnotation annotation = ImageAnnotation.get(annotationID)
		if (annotation == null) {
			return null
		}
		// Next, go ahead and make sure the given annotation belongs to the
		// requested object. Not sure this is really necessary, but why not.
		def url = createURL(studyUID, seriesUID, objectUID)
		def wantedURL = annotation.processed ? annotation.image.processedQueryString : annotation.image.rawQueryString
		if (wantedURL != url) {
			return null
		}
		return annotation
	}

	def index = {
		log.info("Looking up annotations...")
		def (dicom, processed) = findImageByURL(params.studyUID, params.seriesUID, params.objectUID)
		if (dicom == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		// Fetch the annotations
		def annotations = ImageAnnotation.findAllByImageAndProcessed(dicom, processed)
		def result = [annotations: annotations]
		render result as JSON
	}

	def create = {
		def (dicom, processed) = findImageByURL(params.studyUID, params.seriesUID, params.objectUID)
		if (dicom == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		// TODO: Make sure we have all the required parameters
		ImageAnnotation annotation = new ImageAnnotation(params)
		// Reset the parameters that the user is NOT allowed to set:
		annotation.creator = springSecurityService.getCurrentUser()
		annotation.processed = processed
		dicom.addToAnnotations(annotation)
		dicom.save(failOnError: true, flush: true)
		log.info("Create annotation $annotation for ${annotation.image}")
		def result = [ "result": [ "success": true ], "annotation": annotation ]
		render result as JSON
	}

	def update = {
		// See if we can find the specific annotation
		ImageAnnotation annotation = findAnnotationById(params.studyUID, params.seriesUID, params.objectUID, params.annotationID)
		if (annotation == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		// And update the remaining fields
		if (params.containsKey('x'))
			annotation.x = Integer.parseInt(params.x)
		if (params.containsKey('y'))
			annotation.y = Integer.parseInt(params.y)
		if (params.containsKey('slice'))
			annotation.slice = Integer.parseInt(params.slice)
		if (params.containsKey('width'))
			annotation.width = Integer.parseInt(params.width)
		if (params.containsKey('height'))
			annotation.height = Integer.parseInt(params.height)
		if (params.containsKey('depth'))
			annotation.depth = Integer.parseInt(params.depth)
		if (params.containsKey('annotation'))
			annotation.annotation = params.annotation
		annotation.save()
		// Mirror out the annotation on the off-chance we wind up doing
		// processing on any of the values. (Specifically I'm thinking of
		// something like allowing rich text/Wiki text in the annotation.)
		def result = [ "result": [ "success": true ], "annotation": annotation ]
		render result as JSON
	}

	def remove = {
		ImageAnnotation annotation = findAnnotationById(params.studyUID, params.seriesUID, params.objectUID, params.annotationID)
		if (annotation == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}
		annotation.delete()
		def result = [ "result": [ "success": true ]]
		render result as JSON
	}
}
