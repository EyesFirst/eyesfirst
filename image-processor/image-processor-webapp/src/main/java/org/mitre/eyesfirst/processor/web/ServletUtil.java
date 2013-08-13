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
package org.mitre.eyesfirst.processor.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

public class ServletUtil {
	/**
	 * Parses the request path, returning an array of strings that specify
	 * each individual path component from the request.
	 * @param request
	 * @return
	 */
	public static String[] parseRequestPath(HttpServletRequest request) {
		// The request URI is just the path part of the HTTP request (minus
		// the query string)
		String path = request.getRequestURI();
		// First, strip off the context path
		String cp = request.getContextPath();
		if (path.startsWith(cp)) {
			path = path.substring(cp.length());
		}
		// Next, string off the servlet path
		String sp = request.getServletPath();
		if (path.startsWith(sp)) {
			path = path.substring(sp.length());
		}
		if (path.length() == 0) {
			// Empty string is perfectly OK here, since we may have been invoked
			// directly.
			return new String[0];
		}
		// Finally, if there's a / at the start, remove that
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		String[] result = path.split("/");
		try {
			for (int i = 0; i < result.length; i++) {
					result[i] = URLDecoder.decode(result[i], "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			// This should never happen
			throw new RuntimeException("UTF-8 not supported? (This should never happen)", e);
		}
		return result;
	}

}
