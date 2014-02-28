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

import org.apache.commons.logging.LogFactory
import org.springframework.security.access.prepost.PreAuthorize

class ArtifactController {
	private static final log = LogFactory.getLog(this)
	private static final DEBUG_GO_SLOW = false
	def index() { }

	@PreAuthorize("hasRole('ROLE_CLINICIAN')")
	def list() {
		def patient = Patient.findById(params["patient"], [fetch:[classifierResults:"eager"]]);
		if (patient == null) {
			response.sendError(SC_NOT_FOUND);
			return;
		}
		render patient.artifacts.collect {
			[
				id: it.id,
				name: it.name,
				type: it.type.systemName,
				timestamp: it.timestamp,
				laterality: it.laterality,
				hardExudates: it.classifier?.hardExudateCount
			]
		} as JSON
	}

	@PreAuthorize("hasRole('ROLE_CLINICIAN')")
	def thumbnail() {
		def artifact = Artifact.findById(params["id"], [fetch:[thumbnail:"eager"]]);
		ArtifactThumbnail thumbnail = artifact?.thumbnail;
		if (thumbnail == null || thumbnail.image == null || thumbnail.image.length == 0) {
			response.sendError(SC_NOT_FOUND);
			return;
		}
		response.contentType = "image/jpeg";
		OutputStream stream = response.outputStream;
		stream.write(thumbnail.image);
		stream.flush()
	}

	@PreAuthorize("hasRole('ROLE_CLINICIAN')")
	def fetch() {
		def artifact = Artifact.findById(params["id"], [fetch:[data:"eager"]]);
		ArtifactData data = artifact?.data;
		if (data == null || data.data == null || data.mimeType == null) {
			response.sendError(SC_NOT_FOUND);
			return;
		}
		response.setDateHeader("Last-Modified", data.lastModified.time);
		response.contentType = data.mimeType;
		OutputStream stream = response.outputStream;
		if (DEBUG_GO_SLOW) {
			// DEBUG VERSION: Write the data out s...l...o...w...l...y... for
			// debugging purposes with onload handlers.
			byte[] bytes = data.data;
			for (int o = 0; o < bytes.length; o += 1024) {
				stream.write(bytes, o, Math.min(bytes.length - o, 1024));
				stream.flush();
				Thread.sleep(100L);
			}
		} else {
			stream.write(data.data);
		}
		stream.flush()
	}
}
