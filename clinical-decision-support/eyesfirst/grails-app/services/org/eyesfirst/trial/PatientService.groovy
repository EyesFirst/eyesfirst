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

import org.springframework.security.access.annotation.Secured

class PatientService {
	@Secured(['ROLE_CLINICIAN'])
	def findPatient(String patientId) {
		if (patientId.isLong()) {
			return Patient.findById(patientId);
		}
		return null;
	}

	def createPatientMap(Patient patient) {
		
	}

	@Secured(['ROLE_CLINICIAN'])
	def createPatientArtifactList(Patient patient) {
		return patient.artifacts.collect {
			[
				id: it.id,
				name: it.name,
				type: it.type.systemName,
				timestamp: it.timestamp,
				laterality: it.laterality
			]
		};
	}
}
