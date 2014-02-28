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

import org.apache.commons.logging.LogFactory
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import org.eyesfirst.trial.oct.ClassifierResults
import org.eyesfirst.trial.oct.HardExudateCluster
import org.eyesfirst.trial.oct.SynthesizedFundusPhoto
import org.eyesfirst.trial.oct.ThicknessMap
import org.mitre.eyesfirst.dicom.DicomID
import org.mitre.eyesfirst.dicom.DicomImage
import org.mitre.eyesfirst.dicom.DicomMetadataUtil
import org.springframework.transaction.annotation.Transactional

class OctImageService {
	static final log = LogFactory.getLog(this)
	static final JsonFactory jsonFactory = new JsonFactory();
	def dicomAccessService

	@Transactional
	def getOCTType() {
		return ArtifactType.getOrCreate("oct", "OCT scan");
	}

	@Transactional(readOnly = true)
	DicomImage loadDicomImage(String artifactId) {
		if (!artifactId.isInteger()) {
			return null;
		}
		Artifact artifact = Artifact.findById(artifactId, [fetch:[data:"eager"]]);
		return loadDicomImage(artifact);
	}

	@Transactional(readOnly = true)
	DicomImage loadDicomImage(Artifact artifact) {
		if (artifact.type.systemName != 'oct')
			return null;
		DicomID dId = DicomID.fromQueryString(artifact.data.dataAsString())
		return dicomAccessService.retrieveDicomObject(dId.studyUID, dId.seriesUID,
			dId.objectUID);
	}

	@Transactional
	void createThumbnail(Artifact artifact) {
		if (artifact.type.systemName != 'oct') {
			throw new IllegalArgumentException("Can only create thumbnails for OCT images")
		}
		DicomImage image = loadDicomImage(artifact);
		if (image != null) {
			artifact.createThumbnail(image.getSlice((int)(image.sliceCount / 2)), image.aspectRatio);
		}
	}

	@Transactional
	Artifact importOCT(Patient patient, String dicomID) {
		importOCT(patient, DicomID.fromQueryString(dicomID));
	}

	@Transactional
	Artifact importOCT(Patient patient, DicomID dId) {
		String dicomID = dId.toQueryString();
		// Try and load the DICOM image to grab some metadata out of it
		DicomImage image = dicomAccessService.retrieveDicomObject(dId.studyUID, dId.seriesUID,
			dId.objectUID);
		if (image == null)
			throw new IllegalArgumentException("Cannot import DICOM with ID " + dicomID + " - it was not found");
		Date timestamp = new Date(image.lastModifiedTime);
		// Grab the laterality out of the image
		String laterality = DicomMetadataUtil.getLaterality(image.dicomObject);
		// The laterality coming out of DICOM will be L or R, convert to OS or OD
		if (laterality == "L")
			laterality = "OS";
		else if (laterality == "R")
			laterality = "OD";
		Artifact artifact = new Artifact(name: dicomID, timestamp: timestamp, laterality: laterality);
		artifact.data = new ArtifactData(mimeType: "text/plain");
		artifact.data.writeDataAsString(dicomID);
		artifact.type = getOCTType();
		patient.addToArtifacts(artifact);
		artifact.save(failOnError: true);
		try {
			// We now need to load the DICOM image to pull out some additional
			// metadata.
			createThumbnail(artifact);
		} catch (Exception e) {
			log.warn("Unable to create thumbnail for OCT scan", e);
		}
		patient.save(failOnError: true);
		return artifact;
	}

	@Transactional
	ThicknessMap importThicknessMap(Artifact artifact, Date timestamp, String mimeType, byte[] data) {
		// Add a thickness map to an artifact
		if (artifact.type.systemName != "oct") {
			throw new IllegalArgumentException("Not adding a thickness map to artifact type " + artifact.type.systemName)
		}
		ThicknessMap tm = new ThicknessMap(lastModified: timestamp, mimeType: mimeType, data: data);
		artifact.thicknessMap = tm;
		artifact.save(flush: true, failOnError: true)
		return tm;
	}

	@Transactional
	SynthesizedFundusPhoto importSynthesizedFundusPhoto(Artifact artifact, Date timestamp, String mimeType, byte[] data) {
		// Add a thickness map to an artifact
		if (artifact.type.systemName != "oct") {
			throw new IllegalArgumentException("Not adding a synthesized fundus photo to artifact type " + artifact.type.systemName)
		}
		SynthesizedFundusPhoto photo = new SynthesizedFundusPhoto(lastModified: timestamp, mimeType: mimeType, data: data);
		artifact.synthesizedFundusPhoto = photo;
		artifact.save(flush: true, failOnError: true)
		return photo;
	}

	@Transactional
	ClassifierResults importClassifierResults(Artifact artifact, Reader resultsJson, double thickness=0) {
		return importClassifierResultsImpl(artifact, jsonFactory.createJsonParser(resultsJson), thickness);
	}

	@Transactional
	ClassifierResults importClassifierResults(Artifact artifact, InputStream resultsJson, double thickness=0) {
		return importClassifierResultsImpl(artifact, jsonFactory.createJsonParser(resultsJson), thickness);
	}

	@Transactional
	private ClassifierResults importClassifierResultsImpl(Artifact artifact, JsonParser parser, double thickness) {
		// There are two types of classifier results we can receive, either
		// the JSON from an export, or the JSON from the classifier.
		// In any case, the first token must be:
		if (parser.nextToken() != JsonToken.START_OBJECT) {
			// FIXME: Should be a specific exception, I think
			throw new RuntimeException("Expected start of object");
		}
		JsonToken token;
		ClassifierResults results = new ClassifierResults();
		results.thickness = thickness;
		while ((token = parser.nextValue()) != null) {
			String field = parser.getCurrentName();
			switch (field) {
			case "classifierDiagnoses":
				// Recurse!
				if (token == JsonToken.VALUE_STRING) {
					importClassifierResultsImpl(artifact, jsonFactory.createJsonParser(parser.getText()), thickness);
					return;
				}
			case "abnormalThickness":
				importClassifierAbnormalThickness(results, parser);
				break;
			case "hardExudates":
				importClassifierHardExudates(results, parser);
				break;
			default:
				if (token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) {
					// Ignore this
					parser.skipChildren();
				}
				break;
			}
		}
		artifact.classifierResults = results;
		artifact.save(flush: true, failOnError: true);
		return results;
	}

	private void importClassifierAbnormalThickness(ClassifierResults results, JsonParser parser) {
		// We should be starting at an object
		if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new RuntimeException("Expected an object for abnormal thickness, found " + parser.getCurrentToken());
		}
		JsonToken token;
		while ((token = parser.nextValue()) != null) {
			if (token == JsonToken.END_OBJECT)
				return;
			String field = parser.getCurrentName();
			switch (field) {
			case "pfa":
				results.abnormalThicknessPFA = parser.getDoubleValue();
				break;
			case "pd":
				results.abnormalThicknessPD = parser.getDoubleValue();
				break;
			case "specificity":
				results.abnormalThicknessSpecificity = parser.getDoubleValue();
				break;
			case "sensitivity":
				results.abnormalThicknessSensitivity = parser.getDoubleValue();
				break;
			case "jointAnomStat":
				results.abnormalThicknessJointAnomStat = parser.getDoubleValue();
				break;
			default:
				if (token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) {
					// Ignore this
					parser.skipChildren();
				}
				break;
			}
		}
	}

	private void importClassifierHardExudates(ClassifierResults results, JsonParser parser) {
		// We should be starting at an object
		if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new RuntimeException("Expected an object for hard exudates, found " + parser.getCurrentToken());
		}
		JsonToken token;
		while ((token = parser.nextValue()) != null) {
			if (token == JsonToken.END_OBJECT)
				break;
			String field = parser.getCurrentName();
			switch (field) {
			case "pfa":
				results.hardExudatesPFA = parser.getDoubleValue();
				break;
			case "pd":
				results.hardExudatesPD = parser.getDoubleValue();
				break;
			case "hardExudates":
				// Create hard exudates for each entry in the array
				if (token != JsonToken.START_ARRAY) {
					throw new RuntimeException("Expected an array, found " + token);
				}
				while ((token = parser.nextValue()) != null) {
					if (token == JsonToken.END_ARRAY)
						break;
					if (token == JsonToken.START_OBJECT) {
						results.addToHardExudates(importClassifierSingleHardExudate(parser));
					} 
				}
				break;
			default:
				if (token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) {
					// Ignore this
					parser.skipChildren();
				}
				break;
			}
		}
	}
	private HardExudateCluster importClassifierSingleHardExudate(JsonParser parser) {
		HardExudateCluster cluster = new HardExudateCluster();
		// We should be starting at an object
		if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new RuntimeException("Expected an object for hard exudates, found " + parser.getCurrentToken());
		}
		JsonToken token;
		while ((token = parser.nextValue()) != null) {
			if (token == JsonToken.END_OBJECT)
				break;
			String field = parser.getCurrentName();
			switch (field) {
			case "maxCfarValue":
				cluster.maxCfarValue = parser.getDoubleValue();
				break;
			case "normalScore":
				cluster.normalScore = parser.getDoubleValue();
				break;
			case "layer":
				cluster.layer = parser.getIntValue();
				break;
			//case "center": Not now
			//case "radius": Also not now
			case "numVoxels":
				cluster.numVoxels = parser.getIntValue();
				break;
			case "boundingBoxMinCorner":
				// Coordinates are in order columns, layers, rows
				// (or fast time, slow time, axial)
				// (or x, z, y)
				int[] coords = parseCoordinates(parser);
				cluster.boundingBoxX = coords[0];
				cluster.boundingBoxY = coords[2];
				cluster.boundingBoxZ = coords[1];
				break;
			case "boundingBoxWidth":
				// See above comment
				int[] coords = parseCoordinates(parser);
				cluster.boundingBoxWidth = coords[0];
				cluster.boundingBoxHeight = coords[2];
				cluster.boundingBoxDepth = coords[1];
				break;
			case "layerProportion":
				cluster.layerProportion = parser.getDoubleValue();
				break;
			//case "ellipseCenter":
				//[324, 55, 401]
				//break;
			//case "boundingBoxMinCornerShift":
				//[330, 53, 390]
				//break;
			default:
				if (token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) {
					// Ignore this
					parser.skipChildren();
				}
				break;
			}
		}
		return cluster;
	}

	private int[] parseCoordinates(JsonParser parser) {
		if (parser.getCurrentToken() != JsonToken.START_ARRAY)
			throw new RuntimeException("Expected array start");
		int[] rv = new int[3];
		for (int i = 0; i < 3; i++) {
			if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
				throw new RuntimeException("Expected int value");
			}
			rv[i] = parser.getIntValue();
		}
		if (parser.nextToken() != JsonToken.END_ARRAY) {
			throw new RuntimeException("Too many values inside coordinates");
		}
		return rv;
	}
}
