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
package org.mitre.openid

import org.mitre.openid.connect.client.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

class OpenIDSecurityService {
	static transactional = false

	def grailsApplication

	Authentication getAuthentication() {
		return SecurityContextHolder.context?.authentication;
	}
	OIDCAuthenticationToken getOpenIDAuthentication() {
		def auth = getAuthentication();
		if (auth instanceof OIDCAuthenticationToken)
			return (OIDCAuthenticationToken) auth;
		else
			return null;
	}
	boolean isLoggedIn() {
		def auth = getAuthentication()
		// Auth can be anonymous
		return auth != null && !(auth instanceof AnonymousAuthenticationToken);
	}
	/**
	 * Gets the OpenID UserInfo for the current user if available.
	 */
	UserInfo getUserInfo() {
		return getOpenIDAuthentication()?.userInfo;
	}

	/**
	 * Gets a list of configured OpenID providers (configured in
	 * security.openid.defaultIssuers).
	 */
	def getDefaultIssuers() {
		def res = grailsApplication.config.security?.openid?.defaultIssuers
		// TODO: Check to make sure it exists, values are legal, whatever
		return res
	}
}
