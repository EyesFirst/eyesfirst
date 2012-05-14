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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND

import grails.converters.JSON

class DiagnosisController {

	def springSecurityService

	def index = {
		def rawQueryString = request.getParameter("rawQueryString")
		DicomImage image = DicomImage.findByRawQueryString(rawQueryString)
		if (image == null) {
			response.sendError(SC_NOT_FOUND)
		} else {
			if (request.getParameter("json") != null) {
				render image.diagnoses as JSON
			} else {
				[ "diagnoses": image.diagnoses ]
			}
		}
	}

	def save = {
		def rawQueryString = request.getParameter("rawQueryString")
		def image = DicomImage.findByRawQueryString(rawQueryString)

		if (image == null) {
			// Quit immediately
			response.sendError(SC_NOT_FOUND)
			return
		}

		def diagnosis = getDiagnosis(rawQueryString)
		if (diagnosis == null) diagnosis = new Diagnosis()
		diagnosis.setReviewer(springSecurityService.getCurrentUser())
		diagnosis.setHardExudates(parseBoolean(request.getParameter("hardExudates")))
		diagnosis.setAbnormalRetinalThickness(parseBoolean(request.getParameter("abnormalRetinalThickness")))
		diagnosis.setMicroaneurysms(parseBoolean(request.getParameter("microaneurysms")))
		diagnosis.setNeovascularization(parseBoolean(request.getParameter("neovascularization")))
		diagnosis.setNotes(request.getParameter("notes"))

		image.addToDiagnoses(diagnosis)
		image.save(failOnError:true)
		render(contentType: "text/json") { success = true; }
	}

	def show = {
		def rawQueryString = request.getParameter("rawQueryString")
		def diagnosis = getDiagnosis(rawQueryString)

		if (diagnosis == null) {
			response.sendError(SC_NOT_FOUND)
			return
		}

		render diagnosis as JSON
	}

	/**
	 * Parse a string value into a boolean for the database. Specifically
	 * handles "true" and "yes" for true, "false" and "no" for false, and
	 * anything else becomes null, effectively handling the tri-state.
	 * @param s
	 * @return
	 */
	public static Boolean parseBoolean(String s) {
		if (s == "true" || s == "yes")
			return true
		if (s == "false" || s == "no")
			return false
		return null
	}

	Diagnosis getDiagnosis(String rawQueryString) {
		DicomImage image = DicomImage.findByRawQueryString(rawQueryString)
		if (image == null)
			return null
		def diagnoses = image.diagnoses
		if (diagnoses == null) {
			// I think this can't actually happen, but pretend it can anyway
			return null
		}
		for (Diagnosis d : diagnoses) {
			if (d.reviewer.equals(springSecurityService.getCurrentUser()))
				return d
		}
		return null
	}
}
