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
package org.mitre.openid.grails;

import groovy.util.ConfigObject;

import java.util.Collections;
import java.util.Set;

import org.codehaus.groovy.grails.commons.GrailsApplication;

public class OpenIDSecurityUtils {
	private static GrailsApplication application;

	private OpenIDSecurityUtils() {
	}

	public static GrailsApplication getApplication() {
		return application;
	}

	public static void setApplication(GrailsApplication application) {
		OpenIDSecurityUtils.application = application;
	}

	public static String getAppName() {
		return application.getMetadata().getApplicationName();
	}

	public static ConfigObject getOpenIDConfig() {
		if (application == null)
			return new ConfigObject();
		Object sec = application.getConfig().get("security");
		if (sec instanceof ConfigObject) {
			Object oidConf = ((ConfigObject)sec).get("openid");
			if (oidConf instanceof ConfigObject)
				return (ConfigObject) oidConf;
		}
		return new ConfigObject();
	}

	public static Set<String> getRedirectUris() {
		// Attempt to grab the redirect URL
		ConfigObject config = getOpenIDConfig();
		Object o = config.get("redirectURI");
		if (o != null) {
			// Use this value
			return Collections.singleton(o.toString());
		}
		// Otherwise, attempt to create it. Default to localhost:8080 since
		// apparently there literally is no way to look this information up.
		// (After all, a server COULD be listening on multiple ports. So
		// therefore there's no way to lookup ANY port instead of an API that
		// can return a list. Same with hostnames.)
		String prefix = "http://localhost:8080/" + getAppName();
		if (application != null) {
			Object grails = application.getConfig().get("grails");
			if (grails instanceof ConfigObject) {
				o = ((ConfigObject) grails).get("serverURL");
				if (o != null) {
					prefix = o.toString();
				}
			}
		}
		// The prefix shouldn't end with a "/", but go ahead and check and only
		// add one if it doesn't.
		if (!prefix.endsWith("/")) {
			prefix = prefix + "/";
		}
		return Collections.singleton(prefix + "openid_connect_login");
	}

	public static boolean isForceHTTPS() {
		ConfigObject conf = getOpenIDConfig();
		Object o = conf.get("forceHTTPS");
		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue();
		}
		return false;
	}
}
