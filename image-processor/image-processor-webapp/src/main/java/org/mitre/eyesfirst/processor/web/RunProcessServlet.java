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
package org.mitre.eyesfirst.processor.web;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.mitre.eyesfirst.processor.AbstractProcess;
import org.mitre.eyesfirst.processor.EyesFirstProcess;
import org.mitre.eyesfirst.processor.ProcessManager;
import org.slf4j.LoggerFactory;

/**
 * Servlet implementation class RunProcessServlet
 */
@WebServlet(
	description = "Handles executing processes.",
	urlPatterns = { "/process/*" }
)
public class RunProcessServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final ProcessManager processManager = new ProcessManager(2);
	private String wadoRoot;
	private JsonFactory jsonFactory = new JsonFactory();
	private File[] jarFiles;
	private String mcrPath;
	private String dicomURL;
	private String callbackURL;
	private File outputPath;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RunProcessServlet() {
		super();
	}

	@Override
	public void destroy() {
		processManager.destroy();
		super.destroy();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		InitialContext context;
		try {
			context = new InitialContext();
		} catch (NamingException e) {
			throw new ServletException("Unable to get JNDI initial context", e);
		}
		wadoRoot = lookupString(context, "eyesfirst/wado.url");
		processManager.setMaxProcesses(lookupInt(context, "eyesfirst/processes.max"));
		dicomURL = lookupString(context, "eyesfirst/dicom.url");
		callbackURL = lookupString(context, "eyesfirst/callback.url");
		String path = lookupString(context, "eyesfirst/output.path", null);
		if (path == null || path.isEmpty()) {
			log("No output path configured, using temp directory.");
			outputPath = null;
		} else {
			outputPath = new File(path);
			if (outputPath.exists()) {
				if (!outputPath.isDirectory()) {
					log("Output path " + outputPath + " exists and is not a directory: using a temp directory instead.");
					outputPath = null;
				}
			} else {
				if (!outputPath.mkdirs()) {
					log("Unable to create output path " + outputPath + ": using a temp directory instead.");
					outputPath = null;
				}
			}
		}
		if (outputPath == null) {
			try {
				outputPath = File.createTempFile("thickness-classifier-", "");
				if (!outputPath.delete()) {
					throw new IOException("Unable to delete path.");
				}
				if (!outputPath.mkdirs()) {
					throw new IOException("Unable to create directory in temp file's place.");
				}
			} catch (IOException e) {
				log("Unable to create temp path (" + outputPath + "), things are likely to go badly.", e);
			}
		}
		log("Using " + outputPath + " as output path.");
		path = lookupString(context, "eyesfirst/jar.path", null);
		File jarPath;
		if (path == null || path.isEmpty()) {
			jarPath = autoDiscoverWebInf();
		} else {
			jarPath = new File(path);
		}
		// Now that we (may) have that path, use it to create our JAR files
		if (jarPath != null) {
			log("Using " + jarPath + " as JAR path.");
			jarFiles = jarPath.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File f, String name) {
					if (name.length() > 4) {
						return name.substring(name.length()-4).equalsIgnoreCase(".jar");
					} else {
						return false;
					}
				}
			});
		}
		mcrPath = lookupString(context, "eyesfirst/mcr.path");
		log("Run process servlet started. Using the following configuration:");
		log("     Max processes: " + processManager.getMaxProcesses());
		log("          MCR Path: " + mcrPath);
		log("          JAR Path: " + jarPath.getAbsolutePath());
		log("         WADO root: " + wadoRoot);
		log("  DICOM upload URL: " + dicomURL);
		log("      Callback URL: " + callbackURL);
		log("  Output directory: " + outputPath.getAbsolutePath());
	}

	private static String lookupString(InitialContext context, String path) throws ServletException {
		try {
			Object o = context.lookup("java:comp/env/" + path);
			if (o instanceof String)
				return (String) o;
			throw new ServletException("Expected java.lang.String, got " + (o == null ? "null" : o.getClass().getName()));
		} catch (NamingException e) {
			throw new ServletException("Missing required environment entry \"" + path + "\"", e);
		}
	}

	private static String lookupString(InitialContext context, String path, String defaultValue) throws ServletException {
		try {
			Object o = context.lookup("java:comp/env/" + path);
			if (o instanceof String)
				return (String) o;
			throw new ServletException("Expected java.lang.String, got " + (o == null ? "null" : o.getClass().getName()));
		} catch (NameNotFoundException e) {
			// In this case, return the default value
			return defaultValue;
		} catch (NamingException e) {
			throw new ServletException("Missing required environment entry \"" + path + "\"", e);
		}
	}

	private static int lookupInt(InitialContext context, String path) throws ServletException {
		try {
			Object o = context.lookup("java:comp/env/" + path);
			if (o instanceof Integer)
				return ((Integer) o).intValue();
			throw new ServletException("Expected java.lang.String, got " + (o == null ? "null" : o.getClass().getName()));
		} catch (NamingException e) {
			throw new ServletException("Missing required environment entry \"" + path + "\"", e);
		}
	}

	private File autoDiscoverWebInf() {
		log("Attempting to auto-discover WEB-INF path...");
		// This is one of those things that really shouldn't work, but
		// probably will. Get the URL for the log4j.properties file.
		URL log4jProps = getClass().getResource("/log4j.properties");
		if (log4jProps.getProtocol().equals("file")) {
			// Sweet. Make it a file.
			try {
				File log4jPropFile = new File(log4jProps.toURI());
				// We want to go up a level (to classes) and then up a level
				// again.
				File path = log4jPropFile.getParentFile();
				if (path != null) {
					path = path.getParentFile();
				}
				if (path == null) {
					log("Unable to auto-discover location (unable to move from /WEB-INF/classes/log4j.properties to /WEB-INF/lib)");
					return null;
				}
				return new File(path, "processor-lib");
			} catch (URISyntaxException e) {
				// Well, that didn't work.
				log("Unable to auto-discover location (syntax exception).", e);
				return null;
			}
		} else {
			log("Can't use URL " + log4jProps + " to discover WEB-INF location, giving up.");
			return null;
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] path = parseRequestPath(request);
		if (path.length == 0) {
			// Show status
			AbstractProcess[] pe = processManager.getProcesses();
			JsonGenerator json = jsonFactory.createJsonGenerator(response.getOutputStream(), JsonEncoding.UTF8);
			json.writeStartObject();
			json.writeFieldName("processes");
			json.writeStartArray();
			for (AbstractProcess p : pe) {
				json.writeStartObject();
				json.writeStringField("id", ProcessManager.uidToString(p.getUID()));
				json.writeStringField("name", p.getName());
				json.writeStringField("status", p.getStatus().name());
				json.writeStringField("statusString", p.getStatusString());
				json.writeNumberField("worked", p.getUnitsWorked());
				json.writeNumberField("totalWork", p.getTotalWorkUnits());
				json.writeEndObject();
			}
			json.writeEndArray();
			json.writeEndObject();
			json.flush();
			return;
		} else if (path.length == 1) {
			if (path[0].equals("start")) {
				// Bad method
				response.setHeader("Allowed", "POST");
				response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				return;
			}
		}
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] path = parseRequestPath(request);
		if (path.length == 1 && path[0].equals("start")) {
			String studyUID = request.getParameter("studyUID");
			String seriesUID = request.getParameter("seriesUID");
			String objectUID = request.getParameter("objectUID");
			StringBuilder sb = new StringBuilder(wadoRoot);
			if (sb.indexOf("?") >= 0)
				sb.append('&');
			else
				sb.append('?');
			try {
				sb.append("requestType=WADO&contentType=application/dicom&studyUID=");
				sb.append(URLEncoder.encode(studyUID, "UTF-8"));
				sb.append("&seriesUID=");
				sb.append(URLEncoder.encode(seriesUID, "UTF-8"));
				sb.append("&objectUID=");
				sb.append(URLEncoder.encode(objectUID, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new Error("UTF-8 is unsupported?");
			}
			// Create the output path.
			File output = new File(outputPath, createHash(seriesUID, studyUID, objectUID));
			// Start a new process.
			EyesFirstProcess efp = new EyesFirstProcess(sb.toString(), dicomURL, callbackURL, output, mcrPath, jarFiles);
			long uid = processManager.queueProcess(efp);
			JsonGenerator json = jsonFactory.createJsonGenerator(response.getOutputStream());
			json.writeStartObject();
			json.writeFieldName("uid");
			json.writeString(ProcessManager.uidToString(uid));
			json.writeFieldName("status");
			json.writeString(efp.getStatusString());
			json.writeEndObject();
			json.flush();
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	public static String createHash(String seriesUID, String studyUID, String objectUID) {
		if (seriesUID == null || studyUID == null || objectUID == null)
			throw new NullPointerException();
		String message = seriesUID + "&" + studyUID + "&" + objectUID;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			byte[] result = digest.digest(message.getBytes("UTF-8"));
			return Hex.encodeHexString(result);
		} catch (NoSuchAlgorithmException e) {
			// That's not good.
			LoggerFactory.getLogger(RunProcessServlet.class).error("No SHA1 algorithm present in JVM, using java.lang.String.hashCode() instead!", e);
			return Integer.toHexString(message.hashCode());
		} catch (UnsupportedEncodingException e) {
			throw new Error("UTF-8 must be supported");
		}
	}

	/**
	 * Parses the request path, returning an array of strings that specify
	 * each individual path component from the request.
	 * @param request
	 * @return
	 */
	private String[] parseRequestPath(HttpServletRequest request) {
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
		return path.split("/");
	}
}
