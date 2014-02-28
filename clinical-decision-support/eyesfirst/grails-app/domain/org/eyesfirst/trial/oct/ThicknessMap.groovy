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
package org.eyesfirst.trial.oct

import java.util.Date;
import org.eyesfirst.trial.Artifact;

class ThicknessMap {
	/**
	 * The artifact that contains this thickness map
	 */
	Artifact artifact
	/**
	 * The MIME type of the image.
	 */
	String mimeType
	/**
	 * The last modified date.
	 */
	Date lastModified
	/**
	 * The binary data itself (currently capped at 2MB in constraints)
	 */
	byte[] data

	static constraints = {
		mimeType maxSize: 255
		data maxSize: 1024*1024*2
	}
}
