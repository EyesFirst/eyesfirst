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
package org.mitre.eyesfirst.dicom;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;

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

	public DicomID(DicomObject dicom) throws IllegalArgumentException {
		SpecificCharacterSet sc = dicom.getSpecificCharacterSet();
		try {
			DicomElement e = dicom.get(Tag.StudyInstanceUID);
			if (e == null) {
				// No study ID? Give up
				throw new IllegalArgumentException("No StudyInstanceUID found");
			}
			this.studyUID = e.getString(sc, false);
			e = dicom.get(Tag.SeriesInstanceUID);
			if (e == null) {
				// No series ID? Give up
				throw new IllegalArgumentException("No SeriesInstanceUID found");
			}
			this.seriesUID = e.getString(sc, false);
			e = dicom.get(Tag.SOPInstanceUID);
			if (e == null) {
				// No object ID? Give up
				throw new IllegalArgumentException("No SOPInstanceUID found");
			}
			this.objectUID = e.getString(sc, false);
		} catch (UnsupportedOperationException e) {
			throw new IllegalArgumentException("Unable to extract DICOM WADO ID fields (bad DICOM file?)", e);
		}
	}

	public DicomID(URL url) {
		this(url.getQuery(), true);
	}

	public DicomID(URI uri) {
		this(uri.getQuery(), true);
	}

	/**
	 * Strips out the studyUID, seriesUID, and objectUID out of a WADO URL.
	 * @param url the URL to parse
	 * @throws URISyntaxException if the URI syntax is invalid
	 */
	public DicomID(String url) throws URISyntaxException {
		this(new URI(url));
	}

	/**
	 * Constructs a new DICOM ID based on just the query string.
	 * 
	 * @param queryString
	 * @return
	 * @throws IllegalArgumentException
	 *             if the query string is missing required fields
	 */
	public static DicomID fromQueryString(String queryString) {
		return new DicomID(queryString, true);
	}

	/**
	 * Implements parsing a URL based on just the query string.
	 * @param queryString just the query string
	 * @param overload entirely ignored
	 */
	private DicomID(String queryString, boolean overload) {
		String studyUID = null;
		String seriesUID = null;
		String objectUID = null;
		// This is an overly simplistic parser, but I'm pretty sure it will work
		// for the majority of actual WADO URLs, and allows me not to have to
		// bring large libraries to parse a URL.
		String[] values = queryString.split("&");
		try {
			for (String value : values) {
				int i = value.indexOf('=');
				if (i >= 0) {
					String key = value.substring(0, i);
					if (key.equals("studyUID")) {
						studyUID = URLDecoder.decode(value.substring(i+1), "UTF-8");
					} else if (key.equals("seriesUID")) {
						seriesUID = URLDecoder.decode(value.substring(i+1), "UTF-8");
					} else if (key.equals("objectUID")) {
						objectUID = URLDecoder.decode(value.substring(i+1), "UTF-8");
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new Error("UTF-8 must always be supported", e);
		}
		if (studyUID == null || seriesUID == null || objectUID == null) {
			StringBuilder m = new StringBuilder("Missing values for ");
			if (studyUID == null)
				m.append("studyUID ");
			if (seriesUID == null) {
				if (studyUID == null && objectUID != null)
					m.append("and ");
				m.append("seriesUID ");
			}
			if (objectUID == null) {
				if (seriesUID == null || studyUID == null)
					m.append("and ");
				m.append("objectUID ");
			}
			m.append(" in query string \"");
			m.append(queryString);
			m.append("\"");
			throw new IllegalArgumentException(m.toString());
		}
		this.studyUID = studyUID;
		this.seriesUID = seriesUID;
		this.objectUID = objectUID;
	}

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

	/**
	 * Returns the DICOM ID in the format expected for the rawQueryString field
	 * in DORI.
	 * @return
	 */
	public String toQueryString() {
		try {
			return "studyUID=" + URLEncoder.encode(studyUID, "UTF-8") +
					"&seriesUID=" + URLEncoder.encode(seriesUID, "UTF-8") +
					"&objectUID=" + URLEncoder.encode(objectUID, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error("UTF-8 must always be supported", e);
		}
	}

	@Override
	public String toString() {
		return "DicomID [studyUID=" + studyUID + ", seriesUID=" + seriesUID
				+ ", objectUID=" + objectUID + "]";
	}
}
