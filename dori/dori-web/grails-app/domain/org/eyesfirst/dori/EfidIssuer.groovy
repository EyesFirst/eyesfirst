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
package org.eyesfirst.dori

class EfidIssuer {

	String name // usually an org name

	Integer efids = 0
	Integer maxEfids = 100

	static hasMany = [efidList: Efid]

	static constraints = {
		name(blank: false, unique: true)
		efids(min: 0, nullable: false, validator: {val, obj -> val <= obj.maxEfids })
		maxEfids(min: 0, nullable: false)
	}

	static mapping = {
		version true //the default, allows optimistic locking and ensures we can increment efids
		autoTimestamp false
	}

	Set<User> getUsers() {
		User.findAllByEfidIssuer(this) as Set
	}

	public String toString() {
		return "$name ($efids / $maxEfids)"
	}

}
