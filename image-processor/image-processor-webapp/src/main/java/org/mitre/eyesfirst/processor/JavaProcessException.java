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
package org.mitre.eyesfirst.processor;

/**
 * Exception raised when an error occurs trying to spawn a new Java process.
 * @author dpotter
 *
 */
public class JavaProcessException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4671424580425490562L;

	public JavaProcessException() {
		super();
	}

	public JavaProcessException(String message, Throwable cause) {
		super(message, cause);
	}

	public JavaProcessException(String message) {
		super(message);
	}

	public JavaProcessException(Throwable cause) {
		super(cause);
	}

}
