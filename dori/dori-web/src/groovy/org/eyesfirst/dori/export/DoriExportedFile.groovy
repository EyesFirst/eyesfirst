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

package org.eyesfirst.dori.export

import grails.converters.JSON

import java.text.SimpleDateFormat
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import org.dcm4che2.data.DicomObject
import org.dcm4che2.io.DicomOutputStream
import org.eyesfirst.dori.Diagnosis
import org.eyesfirst.dori.DicomImage
import org.eyesfirst.dori.Efid
import org.eyesfirst.dori.EfidIssuer
import org.eyesfirst.dori.Feedback
import org.eyesfirst.dori.ImageAnnotation
import org.eyesfirst.dori.User
import org.mitre.eyesfirst.dicom.DicomID;
import org.mitre.eyesfirst.dori.export.DoriImportException
import org.mitre.eyesfirst.util.NoCloseOutputStream
import org.mitre.eyesfirst.dicom.image.DicomAccessService

class DoriExportedFile {
	static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.S"
	static final def IMAGE_FORMATS = [ "image/jpeg": "jpg", "image/jpg": "jpg", "image/png": "png" ];
	static JsonFactory jsonFactory = new JsonFactory();
	DicomImage image;

	def DoriExportedFile(DicomImage image) {
		this.image = image;
	}

	private static String extensionForFormat(String format) {
		if (format in IMAGE_FORMATS) {
			return IMAGE_FORMATS[format];
		} else {
			return "dat";
		}
	}

	private static void createMimeTypeEntry(ZipOutputStream zip) {
		byte[] bytes = "application/x-dori-export; version=0".getBytes("UTF-8");
		ZipEntry entry = new ZipEntry("mimetype");
		entry.setMethod(ZipEntry.STORED);
		entry.setSize(bytes.length);
		CRC32 crc = new CRC32();
		crc.update(bytes);
		entry.setCrc(crc.getValue());
		zip.putNextEntry(entry);
		zip.write(bytes);
		zip.closeEntry();
	}

	def write(OutputStream out, DicomAccessService dicomService) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(out);
		createMimeTypeEntry(zip);
		zip.putNextEntry(new ZipEntry("DicomImage.json"));
		// Generate our JSON
		JsonGenerator json = jsonFactory.createJsonGenerator(zip);
		json.writeStartObject(); // {
		json.writeStringField("efid", image.efid.id);
		json.writeStringField("rawQueryString", image.rawQueryString);
		json.writeStringField("processedQueryString", image.processedQueryString);
		json.writeStringField("classifierDiagnoses", image.classifierDiagnoses);
		json.writeStringField("clinicalInterpretation", image.clinicalInterpretation);
		json.writeStringField("clinicalFeedback", image.clinicalFeedback);
		if (image.jointStatistic == null) {
			json.writeNullField("jointStatistic");
		} else {
			json.writeNumberField("jointStatistic", image.jointStatistic);
		}
		json.writeStringField("objectUID", image.objectUid);
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		json.writeStringField("dateCreated", dateFormat.format(image.dateCreated))
		json.writeArrayFieldStart("feedback") // [
		image.feedback.each { fb ->
			json.writeStartObject(); // {
			json.writeStringField("reviewer", fb.reviewer.username)
			use (JsonGeneratorCategory) {
				json.writeNullableBooleanField("affirmAbnormalRetinalThickness", fb.affirmAbnormalRetinalThickness)
				json.writeNullableBooleanField("affirmHardExudates", fb.affirmHardExudates)
				json.writeNullableStringField("diagnosis", fb.diagnosis)
				json.writeNullableStringField("plan", fb.plan)
				json.writeNullableStringField("notes", fb.processedNotes)
			}
			json.writeEndObject(); // }
		};
		json.writeEndArray() // ]
		json.writeArrayFieldStart("diagnoses") // [
		image.diagnoses.each { diag ->
			json.writeStartObject(); // {
			json.writeStringField("reviewer", diag.reviewer.username)
			use (JsonGeneratorCategory) {
				json.writeNullableBooleanField("abnormalRetinalThickness", diag.abnormalRetinalThickness)
				json.writeNullableBooleanField("hardExudates", diag.hardExudates)
				json.writeNullableBooleanField("microaneurysms", diag.microaneurysms)
				json.writeNullableBooleanField("neovascularization", diag.neovascularization)
				json.writeNullableStringField("diagnosis", diag.diagnosis)
				json.writeNullableStringField("plan", diag.plan)
				json.writeNullableStringField("notes", diag.notes)
			}
			json.writeEndObject(); // }
		};
		json.writeEndArray() // ]
		json.writeArrayFieldStart("annotations") // [
		image.annotations.each { annotation ->
			json.writeStartObject(); // {
			json.writeStringField("creator", annotation.creator.username)
			json.writeBooleanField("processed", annotation.processed)
			json.writeNumberField("x", annotation.x);
			json.writeNumberField("y", annotation.y);
			json.writeNumberField("slice", annotation.slice);
			json.writeNumberField("width", annotation.width);
			json.writeNumberField("height", annotation.height);
			json.writeNumberField("depth", annotation.depth);
			json.writeStringField("annotation", annotation.annotation);
			json.writeEndObject(); // }
		};
		json.writeEndArray() // ]
		json.writeEndObject(); // }
		json.flush();
		if (image.rawQueryString != null) {
			DicomID dicomID = DicomID.fromQueryString(image.rawQueryString);
			DicomObject dicom = dicomService.retrieveDicomObject(dicomID.studyUID, dicomID.seriesUID, dicomID.objectUID)?.dicomObject;
			if (dicom != null) {
				zip.putNextEntry(new ZipEntry("raw_image.dcm"));
				// writeDicomFile apparently closes the underlying stream.
				// Breaking things.
				DicomOutputStream dout = new DicomOutputStream(new NoCloseOutputStream(zip));
				dout.writeDicomFile(dicom);
				dout.finish();
			}
		}
		if (image.processedQueryString != null) {
			DicomID dicomID = DicomID.fromQueryString(image.processedQueryString);
			DicomObject dicom = dicomService.retrieveDicomObject(dicomID.studyUID, dicomID.seriesUID, dicomID.objectUID)?.dicomObject;
			if (dicom != null) {
				zip.putNextEntry(new ZipEntry("processed.dcm"));
				// writeDicomFile apparently closes the underlying stream.
				// Breaking things.
				DicomOutputStream dout = new DicomOutputStream(new NoCloseOutputStream(zip));
				dout.writeDicomFile(dicom);
				dout.finish();
			}
		}
		if (image.thicknessMap != null) {
			zip.putNextEntry(new ZipEntry("thicknessMap.png"));
			zip.write(image.thicknessMap);
		}
		if (image.fundusPhoto != null) {
			zip.putNextEntry(new ZipEntry("fundus_photo." + extensionForFormat(image.fundusPhoto.format)));
			zip.write(image.fundusPhoto.imageData);
		}
		if (image.synthesizedFundusPhoto != null) {
			zip.putNextEntry(new ZipEntry("synthesized_fundus_photo." + extensionForFormat(image.synthesizedFundusPhoto.format)));
			zip.write(image.synthesizedFundusPhoto.imageData);
		}
		zip.finish();
	}
}
