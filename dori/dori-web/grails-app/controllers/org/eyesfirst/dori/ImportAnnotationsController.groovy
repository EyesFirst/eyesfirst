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

import java.nio.charset.Charset

import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

import com.csvreader.CsvReader


/**
 * Controller for loading in annotations from a CSV file. The CSV file can be
 * generated currently only via directly accessing the database and running
 * the following query:
 * <p>
 * <code>SELECT di.efid_id, inst.pk, di.raw_query_string,
 * IF(d.abnormal_retinal_thickness IS NULL, 'Unsure',
 * IF(d.abnormal_retinal_thickness = 0, 'No', 'Yes')) AS abnormal_thickness,
 * IF(d.hard_exudates IS NULL, 'Unsure',
 * IF(d.hard_exudates = 0, 'No', 'Yes')) AS hard_exudates,
 * IF(d.neovascularization IS NULL, 'Unsure',
 * IF(d.neovascularization = 0, 'No', 'Yes')) AS neovascularization,
 * IF(d.microaneurysms IS NULL, 'Unsure',
 * IF(d.microaneurysms = 0, 'No', 'Yes')) AS microaneurysms,
 * d.notes, d.diagnosis, d.plan FROM pacsdb.instance AS inst
 * INNER JOIN doriweb.dicom_image AS di ON inst.sop_iuid = di.object_uid
 * INNER JOIN diagnosis AS d ON d.image_id = di.id
 * INTO OUTFILE '/tmp/eyesfirst.csv' FIELDS TERMINATED BY ','
 * OPTIONALLY ENCLOSED BY '"' ESCAPED BY '\\' LINES TERMINATED BY '\n';</code>
 * @author dpotter
 */
class ImportAnnotationsController {
	def springSecurityService

	def index() { }

	def upload() {
		if(request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request
			MultipartFile file = mpr.getFile("annotationCSV");
			request.setAttribute("results", readFile(file.getInputStream(), Charset.forName("UTF-8"), springSecurityService.getCurrentUser()));
		} else {
			response.status = SC_BAD_REQUEST
			render "Not a multipart request"
		}
	}

	private static List<String> readFile(InputStream file, Charset charset, User forUser) {
		CsvReader reader = new CsvReader(file, Charset.defaultCharset());
		List<String> result = new ArrayList<String>();
		while (reader.readRecord()) {
			// Handle the record
			String efid = reader.get(0);
			String pk = reader.get(1);
			String dicomId = reader.get(2);
			System.out.println("EFID = " + efid + ", pk=" + pk + ", dicomID=" + dicomId);
			Boolean abnormalThickness = readBoolean(reader.get(3));
			Boolean hardExudates = readBoolean(reader.get(4));
			Boolean neovascularization = readBoolean(reader.get(5));
			Boolean microaneurysms = readBoolean(reader.get(6));
			System.out.println("  AT = " + abnormalThickness + ", HE = " + hardExudates + ", NV = " + neovascularization + ", MA = " + microaneurysms);
			String notes = reader.get(7);
			String diagnosis = reader.get(8);
			String plan = reader.get(9);
			System.out.println("  Notes = " + notes + ", diagnosis = " + diagnosis + ", plan = " + plan);
			// Once we've collected the data, see if we can import it
			DicomImage di = DicomImage.findByRawQueryString(dicomId);
			String msg = "<tr><td>" + efid + "</td><td>" + pk + "</td><td>";
			if (di == null) {
				System.out.println("  Error: Cannot import annotations for " + dicomId + ", it does not exist on this system!");
				result.add(msg + "<b>Error:</b> could not import for " + dicomId + "<br>It was not found in this system.</td></tr>");
			} else {
				// Create the new annotations object for this instance and add
				// it.
				Diagnosis d = di.findDiagnosisForUser(forUser);
				if (d == null) {
					d = new Diagnosis();
					d.reviewer = forUser;
					di.addToDiagnoses(d);
				}
				d.setHardExudates(hardExudates)
				d.setAbnormalRetinalThickness(abnormalThickness)
				d.setMicroaneurysms(microaneurysms)
				d.setNeovascularization(neovascularization)
				d.setDiagnosis(diagnosis)
				d.setPlan(plan)
				d.setNotes(notes)
				di.save(failOnError:true)
				result.add(msg + "OK</td></tr>");
			}
		}
		return result;
	}

	static Boolean readBoolean(String field) {
		if (field.equals("Yes"))
			return Boolean.TRUE;
		else if (field.equals("No"))
			return Boolean.FALSE;
		return null;
	}
}
