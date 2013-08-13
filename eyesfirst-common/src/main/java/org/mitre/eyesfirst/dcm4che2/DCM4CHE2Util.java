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
package org.mitre.eyesfirst.dcm4che2;

/**
 * Should-be-useless class to workaround some DCM4CHE2 defaults that don't
 * necessarily work on all machines.
 * @author dpotter
 *
 */
public class DCM4CHE2Util {
	private DCM4CHE2Util() {
	}

	/**
	 * Sets system properties that tell DCM4CHE2 to use the Java-based JPEG
	 * encoders and NOT the JNI ones that don't exist under all Java
	 * implementations.
	 */
	public static void setImageIOSettings() {
		try {
			System.setProperty("org.dcm4che2.imageio.ImageReaderFactory",
					"org/mitre/eyesfirst/dcm4che2/ImageReaderFactory.properties");
			System.setProperty("org.dcm4che2.imageio.ImageWriterFactory",
					"org/mitre/eyesfirst/dcm4che2/ImageWriterFactory.properties");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
