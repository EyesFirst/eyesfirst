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
import org.mitre.openid.Issuer
import org.mitre.openid.Role
import org.mitre.openid.User
import org.mitre.openid.UserRole

// This class exists solely to set up the plugin test environment
class BootStrap {
	
	def init = { servletContext ->
		// Create users
		Issuer i = new Issuer(issuer: 'https://id.mitre.org/connect/')
		i.save()
		// Example of creating a new user
		User u = new User(issuer: i, subject: 'changeme')
		u.save()
		Role r = new Role(authority: 'ROLE_ADMIN')
		r.save()
		new UserRole(user: u, role: r).save()
	}
	def destroy = {
	}
}
