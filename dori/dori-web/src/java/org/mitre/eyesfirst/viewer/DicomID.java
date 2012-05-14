/*
 * Copyright 2012 The MITRE Corporation
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
package org.mitre.eyesfirst.viewer;

/**
 * A DICOM id: a three-part UI, consisting of a study UID, a series UID,
 * and an object UID.
 *
 * @author dpotter
 */
public class DicomID {
	private final String studyUID;
	private final String seriesUID;
	private final String objectUID;

	/**
	 * Creates a new DICOM ID with the given IDs.
	 * 
	 * @param studyUID
	 *            the study UID
	 * @param seriesUID
	 *            the series UID
	 * @param objectUID
	 *            the object UID
	 * @throws NullPointerException
	 *             if any of the three IDs are {@code null}
	 */
	public DicomID(String studyUID, String seriesUID, String objectUID) {
		if (studyUID == null || seriesUID == null || objectUID == null)
			throw new NullPointerException();
		this.studyUID = studyUID;
		this.seriesUID = seriesUID;
		this.objectUID = objectUID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((objectUID == null) ? 0 : objectUID.hashCode());
		result = prime * result
				+ ((seriesUID == null) ? 0 : seriesUID.hashCode());
		result = prime * result
				+ ((studyUID == null) ? 0 : studyUID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DicomID other = (DicomID) obj;
		if (objectUID == null) {
			if (other.objectUID != null)
				return false;
		} else if (!objectUID.equals(other.objectUID))
			return false;
		if (seriesUID == null) {
			if (other.seriesUID != null)
				return false;
		} else if (!seriesUID.equals(other.seriesUID))
			return false;
		if (studyUID == null) {
			if (other.studyUID != null)
				return false;
		} else if (!studyUID.equals(other.studyUID))
			return false;
		return true;
	}

	/**
	 * Gets the study UID.
	 * @return the study UID
	 */
	public String getStudyUID() {
		return studyUID;
	}

	public String getSeriesUID() {
		return seriesUID;
	}

	public String getObjectUID() {
		return objectUID;
	}

	@Override
	public String toString() {
		return "DicomID [studyUID=" + studyUID + ", seriesUID=" + seriesUID
				+ ", objectUID=" + objectUID + "]";
	}
}
