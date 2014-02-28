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

/**
 * Artifact data: generally an opaque blob that either is the artifact data
 * directly or provides some mechanism for accessing it.
 * @author dpotter
 */
class ArtifactData {
	private static final Date NULL_DATE = new Date(0)
	/**
	 * The MIME type of the data. If set to non-null, then the artifact
	 * controller will serve the data directly. Otherwise, a custom type handler
	 * will be required to deal with the data.
	 */
	String mimeType
	/**
	 * The last modified date.
	 */
	Date lastModified = NULL_DATE
	/**
	 * The binary data itself (currently capped at 20MB in constraints)
	 */
	byte[] data

	static belongsTo = [ artifact: Artifact ];

	static constraints = {
		mimeType maxSize: 255
		data maxSize: 1024*1024*20
	}

	String dataAsString() {
		ByteArrayInputStream ba = new ByteArrayInputStream(data);
		DataInputStream dataStream = new DataInputStream(ba);
		return dataStream.readUTF();
	}

	void writeDataAsString(String str) {
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		DataOutputStream dataStream = new DataOutputStream(ba);
		dataStream.writeUTF(str);
		dataStream.flush()
		dataStream.close()
		data = ba.toByteArray()
	}

	def beforeInsert() {
		if (lastModified == NULL_DATE) {
			lastModified = new Date()
		}
	}
}
