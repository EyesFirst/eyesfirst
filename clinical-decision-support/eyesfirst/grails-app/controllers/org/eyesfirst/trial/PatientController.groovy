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

import org.springframework.security.access.annotation.Secured

class PatientController {
	def index() { }

	@Secured(['ROLE_CLINICIAN'])
	def list() {
		// Generate a list of patients, optionally sorted
		def page = params.int("p")
		if (page == null)
			page = 1
		def pageSize = params.int("ps")
		// FIXME: Make the default page size configurable
		if (pageSize == null)
			pageSize = 20
		/*
		 * TODO: Implement sorting.
		def sortOn = params["s"]
		if (sortOn == null)
			sortOn = "name"
		*/
		// The sort list is a comma separated list of fields by default, so
		def c = Patient.createCriteria();
		def results = c.list {
			firstResult ((page - 1) * pageSize)
			maxResults pageSize
			and {
				order("lastName", "asc")
				order("firstName", "asc")
			}
		};
		render results.collect {
			[
				id: it.id,
				lastName: it.lastName,
				firstName: it.firstName,
				artifactCount: it.artifacts.size(),
				lastVisit: it.lastVisit
			]
		} as JSON
	}

	@Secured(['ROLE_CLINICIAN'])
	def info() {
		def patient = Patient.findById(params["id"]);
		if (patient == null) {
			response.sendError(SC_NOT_FOUND);
			return;
		}
		def map = [
			id: patient.id,
			firstName: patient.firstName,
			lastName: patient.lastName,
			birthday: patient.birthday,
			lastVisit: patient.lastVisit,
			gender: patient.gender,
			mrn: patient.mrn
		];
		// Also grab the artifact list for the patient.
		// FIXME: This should be using the same code that the ArtifactController does!
		map["artifacts"] = patient.artifacts.collect {
			[
				id: it.id,
				name: it.name,
				type: it.type.systemName,
				timestamp: it.timestamp,
				laterality: it.laterality,
				hardExudates: it.classifierResults?.hardExudateCount,
				thickness: it.classifierResults?.thickness
			]
		};
		render map as JSON
	}
}
