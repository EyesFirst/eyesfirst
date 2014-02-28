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
package openid.connect

import org.springframework.security.access.prepost.PreAuthorize

class DebugController {
	def openIdConnectAuthenticationFilter
	def openIDSecurityService

	def index() {
		render (
			view:'debug', model: ["openIdConnectAuthenticationFilter":openIdConnectAuthenticationFilter]
		);
	}

	@PreAuthorize("hasRole('ROLE_USER')")
	def secured() {
		render (
			view:'secured', model: ["authentication":openIDSecurityService.authentication]
		);
	}
}
