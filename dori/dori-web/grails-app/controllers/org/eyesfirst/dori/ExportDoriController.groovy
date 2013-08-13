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

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.eyesfirst.dori.export.DoriExportedFile;

class ExportDoriController {
	def dicomAccessService

	def index() { }

	def export() {
		DicomImage image = DicomImage.get(Long.parseLong(params.imageID))
		if (image == null) {
			System.out.println("No image with ID " + params.imageID)
			response.sendError(SC_NOT_FOUND)
			return
		}
		if (image.efid.id != params.efid) {
			log.warn("Image ID/EFID mismatch (wanted EFID " + params.efid + ", got image for EFID " + image.efid.id);
			response.sendError(SC_NOT_FOUND)
			return
		}
		response.contentType = "application/x-dori-export; version=0"
		response.setHeader("Content-Disposition", "attachment; filename=\"" + image.efid.id + "_" + image.id + ".efz\"")
		// Otherwise, generate the output ZIP
		// TODO (well, probably not): Figure out the file size and set
		// contentLength to that value. Unfortunately the only way to do that is
		// to create the ZIP file.
		new DoriExportedFile(image).write(response.outputStream, dicomAccessService);
	}
}
