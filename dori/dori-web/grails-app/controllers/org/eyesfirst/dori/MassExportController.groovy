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

import org.eyesfirst.dori.export.DoriExportedFile

class MassExportController {
	File exportDir = new File("/tmp/EyesFirst/export");
	def dicomAccessService

	def index() {
		// Try and create the export directory
		exportDir.mkdirs();
		PrintWriter out = response.writer;
		out.println("<!DOCTYPE html><html><head><title>Mass Export...</title></head><body><h1>Mass Export</h1><p>Exporting to " + exportDir + "...</p><pre>");
		// Unfortunately, we can't possibly know the actual image ID through
		// here, so just skip that part and create a map of EFIDs
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (DicomImage i : DicomImage.list()) {
			String efid = i.efid.id;
			Integer e = map.get(efid);
			int id = 1;
			if (e == null) {
				map.put(efid, id);
			} else {
				id = e + 1;
				map.put(efid, id);
			}
			String filename = efid + "_" + id + ".efz";
			System.out.println("Exporting " + filename + "...");
			out.println("Exporting " + filename + "...");
			out.flush();
			new DoriExportedFile(i).write(new FileOutputStream(new File(exportDir, filename)), dicomAccessService);
		}
		out.println("</pre><h1>Done!</h1></body></html>");
		out.flush();
	}
}
