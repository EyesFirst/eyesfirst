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
 * Very basic type system that describes the types of artifacts and how they're
 * loaded.
 * @author dpotter
 */
class ArtifactType {
	String systemName;
	String name;

	static hasMany = [ artifacts: Artifact ];

	static constraints = {
		systemName size: 1..32
		name size: 1..255
	}

	static ArtifactType getOrCreate(String systemName, String name) {
		ArtifactType rv = ArtifactType.findBySystemName(systemName);
		if (rv == null) {
			rv = new ArtifactType(systemName: systemName, name: name);
			rv.save(flush: true, failOnError: true);
		}
		return rv;
	}
}
