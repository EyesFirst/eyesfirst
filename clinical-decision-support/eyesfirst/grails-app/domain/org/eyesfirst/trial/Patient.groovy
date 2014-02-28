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

class Patient {
	/**
	 * The last name/family name, required.
	 */
	String lastName;
	/**
	 * The first name, optional. (Optional if the patient has no given name.)
	 */
	String firstName;
	/**
	 * Patient's MRN.
	 */
	String mrn;
	/**
	 * Patient's gender.
	 */
	String gender;
	/**
	 * The patient's birth day.
	 */
	Date birthday;
	/**
	 * The previous visit. (Should this be user filled-out or should it be the
	 * date of the most recent artifact? I dunno.)
	 */
	Date lastVisit;

	static constraints = {
		lastName blank: false
		firstName nullable: true
		birthday nullable: true
		lastVisit nullable: true
		mrn nullable: true
		gender size: 1..1, nullable: true
	}

	static hasMany = [ artifacts: Artifact ];
}
