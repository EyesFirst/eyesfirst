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

import javax.servlet.http.HttpServletResponse;

import grails.converters.JSON


class FeedbackController {

	def springSecurityService

	def index = {
		def processedQueryString = request.getParameter("processedQueryString")
		DicomImage image = DicomImage.findByProcessedQueryString(processedQueryString)
		if(image == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND)
		} else {
			if (request.getParameter("json") != null) {
				render image.feedback as JSON
			} else {
				[ "feedback": image.feedback ]
			}
		}
	}

	def save = {
		def image = DicomImage.findByProcessedQueryString(request.getParameter("processedQueryString"))

		// Fail immediately if not found
		if (image == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND)
			return
		}

		def feedback = getFeedback(request.getParameter("processedQueryString"))
		if (feedback == null) feedback = new Feedback()
		feedback.setReviewer(springSecurityService.getCurrentUser())
		feedback.setAffirmHardExudates(DiagnosisController.parseBoolean(request.getParameter("affirmHardExudates")))
		feedback.setAffirmAbnormalRetinalThickness(DiagnosisController.parseBoolean(request.getParameter("affirmAbnormalRetinalThickness")))
		feedback.setDiagnosis(request.getParameter("diagnosis"))
		feedback.setPlan(request.getParameter("plan"))
		feedback.setProcessedNotes(request.getParameter("processedNotes"))

		image.addToFeedback(feedback)
		image.save()
		render(contentType: "text/json") { success = true; }
	}

	def show = {
		def feedback = getFeedback(request.getParameter("processedQueryString"))
		if (feedback != null) {
			render feedback as JSON
		}
		response.sendError(HttpServletResponse.SC_NOT_FOUND)
	}

	Feedback getFeedback(String processedQueryString) {
		def image = DicomImage.findByProcessedQueryString(processedQueryString)
		if (image == null) {
			return null
		}
		def feedback = image.feedback
		for(Feedback f : feedback) {
			if(f.reviewer.equals(springSecurityService.getCurrentUser()))
				return f
		}
		return null
	}
}
