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

class OpenidTagLib {
	static namespace = "openid"
	def openIDSecurityService
	/**
	 * Conditionally render the body if the user is logged in.
	 */
	def loggedIn = { attrs, body ->
		if (openIDSecurityService.isLoggedIn()) {
			out << body();
		}
	}
	/**
	 * Conditionally render the body if the user is logged out.
	 */
	def loggedOut = { attrs, body ->
		if (!openIDSecurityService.isLoggedIn()) {
			out << body();
		}
	}
	/**
	 * Render the user's user name (NOT their actual name).
	 */
	def userName = { attrs ->
		def userInfo = openIDSecurityService.userInfo;
		if (userInfo != null) {
			out << userInfo.preferredUsername;
		}
	}
	/**
	 * Renders the login form. This may include default logins.
	 */
	def loginForm = { attrs ->
		out << "<form action=\"${request.contextPath}/openid_connect_login\" method=\"get\">";
		def issuers = openIDSecurityService.defaultIssuers;
		if (issuers.empty) {
			out << 'Please enter the URL of your issuer: ';
		} else {
			out << 'Login using:<ul class="default-issuers-list">'
			issuers.each {
				if (it.value.name && it.value.url) {
					out << '<li><a href="';
					out << request.contextPath << "/openid_connect_login?identifier=" << it.value.url.encodeAsURL();
					out << '">' << it.value.name.encodeAsHTML() << '</a></li>';
				}
			};
			out << '</ul>Or enter the connection URL of your OpenID Connect server: ';
		}
		out << '<input type="text" name="identifier"><input type="submit" value="Login">';
		out << '</form>';
	}
}
