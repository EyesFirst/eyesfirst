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

import org.eyesfirst.dori.export.DoriImportedFile
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

class ImportDoriController {
	def solrService;
	def springSecurityService;
	def dicomUploadService;

	def index() { }
	
	def upload() {
		String importedEFID = null;
		String solrError = null;
		if (request instanceof MultipartHttpServletRequest) {
			for (Map.Entry<String, MultipartFile> item : request.fileMap) {
				if (item.key == "importedFile") {
					// TODO: Handle import errors
					log.debug("Receiving imported DORI entry...")
					// Since it's a ZIP, we have to create a temp file for it.
					File importedFile = File.createTempFile("import_", ".zip");
					item.value.transferTo(importedFile);
					// And import it
					log.debug("Parsing JSON data...")
					DoriImportedFile importData = new DoriImportedFile(importedFile, springSecurityService.currentUser);
					String[] warnings = importData.getWarnings();
					if (warnings != null && request.getParameter("ignoreWarnings") != "yes") {
						// FIXME: We force the user to reupload their file after
						// aborting due to warnings. Really it should be stuffed
						// into the session with a random key and THAT should
						// be used.
						render(view:"warnings", model: [ warnings: warnings ]);
						return;
					}
					// Now to upload the existing dicom files...
					File dcm = importData.extractRawDicom();
					log.debug("Sending raw DICOM to DCM4CHE...")
					dicomUploadService.uploadDicomFile(dcm);
					dcm.delete();
					dcm = importData.extractProcessedDicom();
					if (dcm != null) {
					log.debug("Sending processed DICOM to DCM4CHE...")
						dicomUploadService.uploadDicomFile(dcm);
						dcm.delete();
					}
					DicomImage dicomImage = importData.dicomImage;
					if (dicomImage.fundusPhoto != null) {
						dicomImage.fundusPhoto.save(failOnError:true);
					}
					if (dicomImage.synthesizedFundusPhoto != null) {
						dicomImage.synthesizedFundusPhoto.save(failOnError:true);
					}
					dicomImage.save(failOnError:true);
					importData.close();
					importedFile.delete();
					log.info("Imported image for EFID " + dicomImage.efid.id + ".")
					importedEFID = dicomImage.efid.id
					try {
						solrService.updateSolr();
					} catch (Exception e) {
						solrError = "Unable to update SOLR: " + e;
					}
				}
			}
		}
		render(view:"index", model:[importedEFID:importedEFID, solrError: solrError])
	}
}
