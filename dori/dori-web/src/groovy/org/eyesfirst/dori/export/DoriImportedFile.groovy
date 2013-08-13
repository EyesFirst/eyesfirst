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

import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import org.apache.commons.logging.LogFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import org.eyesfirst.dori.Diagnosis
import org.eyesfirst.dori.DicomImage
import org.eyesfirst.dori.Efid
import org.eyesfirst.dori.EfidIssuer
import org.eyesfirst.dori.Feedback
import org.eyesfirst.dori.FundusPhoto
import org.eyesfirst.dori.ImageAnnotation
import org.eyesfirst.dori.User
import org.mitre.eyesfirst.dori.export.DoriImportException

class DoriImportedFile {
	private static final log = LogFactory.getLog(this)
	private ZipFile zipFile
	private DicomImage dicomImage
	private List<String> warnings
	static final def IMAGE_FORMATS = [ "jpg": "image/jpeg", "png": "image/png" ];

	def DoriImportedFile(File file, User importingUser) throws DoriImportException {
		zipFile = new ZipFile(file)
		ZipEntry entry = zipFile.getEntry("DicomImage.json")
		if (entry == null)
			throw new DoriImportException("Missing DicomImage.json")
		dicomImage = parseDicomJson(zipFile.getInputStream(entry), importingUser);
		// Now for the thickness map and other items...
		entry = zipFile.getEntry("thicknessMap.png");
		if (entry != null) {
			dicomImage.thicknessMap = importDataFile("thickness map", entry);
		}
		// Importing the fundus/synthesized fundus is somewhat more difficult,
		// because it could be in any of the supported export formats. So go
		// through the entries and find the first one
		zipFile.entries().each { e ->
			if (e.name.startsWith("fundus_photo.")) {
				if (dicomImage.fundusPhoto == null) {
					dicomImage.fundusPhoto = importFundusPhoto("fundus photo", e);
				} else {
					warn("Multiple fundus photos found in the exported data, ignoring duplicates!");
				}
			} else if (e.name.startsWith("synthesized_fundus_photo.")) {
				if (dicomImage.synthesizedFundusPhoto == null) {
					dicomImage.synthesizedFundusPhoto = importFundusPhoto("synthesized fundus photo", e);
				} else {
					warn("Multiple synthesized fundus photos found in the exported data, ignoring duplicates!");
				}
			}
		};
	}

	private void warn(String message) {
		if (warnings == null)
			warnings = new ArrayList<String>();
		warnings.add(message);
		log.warn(message);
	}

	public String[] getWarnings() {
		if (warnings == null) {
			return null;
		} else {
			return warnings.toArray(new String[warnings.size()]);
		}
	}

	private byte[] importDataFile(String name, ZipEntry entry) {
		byte[] data = new byte[entry.size];
		InputStream is = zipFile.getInputStream(entry);
		for (int o = 0; o < data.length; ) {
			int r = is.read(data, o, data.length - o);
			if (r < 0)
				throw new DoriImportException("Unable to import " + name + ": expected " + data.length + " bytes, only got " + o);
			o += r;
		}
		return data;
	}

	private FundusPhoto importFundusPhoto(String name, ZipEntry entry) {
		FundusPhoto fundus = new FundusPhoto();
		int i = entry.name.lastIndexOf('.');
		String extension = entry.name.substring(i+1).toLowerCase();
		if (extension in IMAGE_FORMATS) {
			fundus.format = IMAGE_FORMATS[extension];
		} else {
			fundus.format = "application/octet-stream";
		}
		fundus.imageData = importDataFile(name, entry);
		return fundus;
	}

	public void close() {
		zipFile.close();
	}

	public DicomImage getDicomImage() {
		return dicomImage;
	}

	/**
	 * Creates a temporary file containing the extracted raw dicom.
	 * @return
	 */
	public File extractRawDicom() {
		return extractFile("raw_image.dcm", "raw", "dcm")
	}

	/**
	 * Creates a temporary file containing the extracted raw dicom.
	 * @return
	 */
	public File extractProcessedDicom() {
		return extractFile("processed.dcm", "processed", "dcm")
	}

	private File extractFile(String name, String prefix, String suffix) {
		ZipEntry entry = zipFile.getEntry(name);
		if (entry == null)
			return null;
		File res = File.createTempFile(prefix, suffix);
		FileOutputStream out = new FileOutputStream(res);
		byte[] buf = new byte[8096];
		InputStream is = zipFile.getInputStream(entry);
		while (true) {
			int r = is.read(buf);
			if (r < 0)
				break;
			out.write(buf, 0, r);
		}
		is.close();
		out.close();
		return res;
	}

	private DicomImage parseDicomJson(InputStream jsonIn, User importingUser) {
		use (JsonParserCategory) {
			JsonParser parser = DoriExportedFile.jsonFactory.createJsonParser(jsonIn);
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				throw new DoriImportException("Expected start of object");
			}
			JsonToken token;
			DicomImage result = new DicomImage();
			while ((token = parser.nextValue()) != null) {
				String field = parser.getCurrentName();
				if (field == "efid") {
					String efidStr = parser.getText();
					Efid efid = Efid.get(efidStr);
					if (efid == null) {
						// Automatically issue this EFID to the current user?
						if (importingUser == null) {
							throw new DoriImportException("The imported file uses a new EFID, but there is no user available to use to import the EFID");
						} else {
							EfidIssuer issuer = importingUser.efidIssuer;
							if (issuer.efids >= issuer.maxEfids) {
								throw new DoriImportException("Importing this file would require issuing a new EFID, but the issuer is out of EFIDs.");
							}
							efid = new Efid();
							efid.id = efidStr;
							issuer.efids++;
							issuer.addToEfidList(efid);
							// I'm not clear on why I have to save both, but I do:
							issuer.save(failOnError:true);
							efid.save(failOnError: true);
						}
					}
					result.efid = efid;
				} else if (field == "rawQueryString") {
					result.rawQueryString = parser.getText();
				} else if (field == "processedQueryString") {
					result.processedQueryString = parser.getNullableString();
				} else if (field == "classifierDiagnoses") {
					result.classifierDiagnoses = parser.getNullableString();
				} else if (field == "clinicalInterpretation") {
					result.clinicalInterpretation = parser.getNullableString();
				} else if (field == "clinicalFeedback") {
					result.clinicalFeedback = parser.getNullableString();
				} else if (field == "jointStatistic") {
					if (token == JsonToken.VALUE_NUMBER_FLOAT) {
						result.jointStatistic = parser.getDoubleValue();
					}
				} else if (field == "objectUID") {
					result.objectUid = parser.getText();
				} else if (field == "dateCreated") {
					SimpleDateFormat dateFormat = new SimpleDateFormat(DoriExportedFile.DATE_FORMAT);
					result.dateCreated = dateFormat.parse(parser.getText());
				} else if (field == "feedback") {
					parseFeedbackJson(parser, result);
				} else if (field == "diagnoses") {
					parseDiagnosesJson(parser, result);
				} else if (field == "annotations") {
					parseAnnotationsJson(parser, result);
				} else {
					if (token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) {
						// Ignore this
						parser.skipChildren();
					}
				}
			}
			return result;
		}
	}

	private void parseFeedbackJson(JsonParser parser, DicomImage image) {
		use (JsonParserCategory) {
			if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
				throw new DoriImportException("Expected feedback array");
			}
			JsonToken token;
			while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
				if (token != JsonToken.START_OBJECT) {
					throw new DoriImportException("Expected object");
				}
				Feedback fb = new Feedback();
				String reviewer = null;
				while ((token = parser.nextValue()) != JsonToken.END_OBJECT) {
					String field = parser.getCurrentName();
					if (field == "reviewer") {
						reviewer = parser.getText();
						fb.reviewer = findUser(reviewer);
					} else if (field == "affirmAbnormalRetinalThickness") {
						fb.affirmAbnormalRetinalThickness = parser.readNullableBooleanField();
					} else if (field == "affirmHardExudates") {
						fb.affirmHardExudates = parser.readNullableBooleanField();
					} else if (field == "diagnosis") {
						fb.diagnosis = parser.getNullableString();
					} else if (field =="plan") {
						fb.plan = parser.getNullableString();
					} else if (field == "notes") {
						fb.processedNotes = parser.getText();
					} else {
						throw new DoriImportException("Unexpected field " + field);
					}
				}
				if (fb.reviewer == null) {
					if (reviewer == null) {
						warn("No reviewer given in imported feedback data - dropping feedback.");
					} else {
						warn("No user \"" + reviewer + "\" found - dropping their feedback (they will not appear in the imported version).")
					}
				} else {
					image.addToFeedback(fb);
				}
			}
		}
	}

	private void parseDiagnosesJson(JsonParser parser, DicomImage image) {
		if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
			throw new DoriImportException("Expected diagnoses array");
		}
		use (JsonParserCategory) {
			JsonToken token;
			while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
				if (token != JsonToken.START_OBJECT) {
					throw new DoriImportException("Expected object");
				}
				Diagnosis diagnosis = new Diagnosis();
				String reviewer = null;
				while ((token = parser.nextValue()) != JsonToken.END_OBJECT) {
					String field = parser.getCurrentName();
					if (field == "reviewer") {
						reviewer = parser.getText();
						diagnosis.reviewer = findUser(reviewer);
					} else if (field == "abnormalRetinalThickness") {
						diagnosis.abnormalRetinalThickness = parser.readNullableBooleanField();
					} else if (field == "hardExudates") {
						diagnosis.hardExudates = parser.readNullableBooleanField();
					} else if (field == "microaneurysms") {
						diagnosis.microaneurysms = parser.readNullableBooleanField();
					} else if (field == "neovascularization") {
						diagnosis.neovascularization = parser.readNullableBooleanField();
					} else if (field == "diagnosis") {
						diagnosis.diagnosis = parser.getNullableString();
					} else if (field == "plan") {
						diagnosis.plan = parser.getNullableString();
					} else if (field == "notes") {
						diagnosis.notes = parser.getNullableString();
					} else {
						throw new DoriImportException("Unexpected field " + field);
					}
				}
				if (diagnosis.reviewer == null) {
					if (reviewer == null) {
						warn("No reviewer given in imported diagnosis data - dropping a diagnosis.");
					} else {
						warn("No user \"" + reviewer + "\" found - dropping their diagnoses (they will not appear in the imported version).")
					}
				} else {
					image.addToDiagnoses(diagnosis);
				}
			}
		}
	}

	private void parseAnnotationsJson(JsonParser parser, DicomImage image) {
		if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
			throw new DoriImportException("Expected annotations array");
		}
		JsonToken token;
		while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
			if (token != JsonToken.START_OBJECT) {
				throw new DoriImportException("Expected object");
			}
			ImageAnnotation annotation = new ImageAnnotation();
			String creator = null;
			while ((token = parser.nextValue()) != JsonToken.END_OBJECT) {
				String field = parser.getCurrentName();
				if (field == "creator") {
					creator = parser.getText();
					annotation.creator = findUser(creator);
				} else if (field == "processed") {
					annotation.processed = parser.getBooleanValue();
				} else if (field == "x") {
					annotation.x = parser.getIntValue();
				} else if (field == "y") {
					annotation.y = parser.getIntValue();
				} else if (field == "slice") {
					annotation.slice = parser.getIntValue();
				} else if (field == "width") {
					annotation.width = parser.getIntValue();
				} else if (field == "height") {
					annotation.height = parser.getIntValue();
				} else if (field == "depth") {
					annotation.depth = parser.getIntValue();
				} else if (field == "annotation") {
					annotation.annotation = parser.getText();
				} else {
					throw new DoriImportException("Unexpected field " + field);
				}
			}
			if (annotation.creator == null) {
				if (creator == null) {
					warn("No creator given in imported data - dropping annotation.");
				} else {
					warn("No user \"" + creator + "\" found - dropping their annotation (it will not appear in the imported version).")
				}
			} else {
				image.addToAnnotations(annotation);
			}
		}
	}

	private static User findUser(String name) {
		User reviewer = User.findByUsername(name);
		if (reviewer == null) {
			System.out.println("Warning: No local user with the name " + name);
		}
		return reviewer;
	}
}
