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
import org.eyesfirst.dori.*

class BootStrap {

	def springSecurityService

	def init = {servletContext ->
		def adminRole = Role.findByAuthority('ROLE_ADMIN') ?: new Role(authority: 'ROLE_ADMIN').save(failOnError: true)
		def createRole = Role.findByAuthority('ROLE_CREATE') ?: new Role(authority: 'ROLE_CREATE').save(failOnError: true)
		def browseRole = Role.findByAuthority('ROLE_BROWSE') ?: new Role(authority: 'ROLE_BROWSE').save(failOnError: true)

		def adminUser = User.findByUsername('admin') ?: new User(
				username: 'admin', email: 'jsutherland@mitre.org',
				password: 'admin',
				enabled: true).save(failOnError: true)

		if (!adminUser.authorities.contains(adminRole)) {
			UserRole.create adminUser, adminRole
		}
		if (!adminUser.authorities.contains(createRole)) {
			UserRole.create adminUser, createRole
		}
		if (!adminUser.authorities.contains(browseRole)) {
			UserRole.create adminUser, browseRole
		}

		if (adminUser.efidIssuer == null) {
			adminUser.efidIssuer = new EfidIssuer([name: "admin"]).save()
		}
	}

	def destroy = {
	}
}
