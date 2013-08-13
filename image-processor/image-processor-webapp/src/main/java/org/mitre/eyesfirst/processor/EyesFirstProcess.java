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
package org.mitre.eyesfirst.processor;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.mitre.eyesfirst.dicom.DicomID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EyesFirst image process.
 * @author dpotter
 */
public class EyesFirstProcess extends JavaProcess {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7222488527676729409L;
	private Logger log = LoggerFactory.getLogger(getClass());
	private String name = "EyesFirst";
	private String wadoURL;
	private String callbackURL;
	private String callbackKey;
	private String mcrPath;
	private String dicomURL;
	private File[] jarFiles = null;
	private File outputPath;
	private ConsoleInputHelper stdout = new ConsoleInputHelper();
	private ConsoleInputHelper stderr = new ConsoleInputHelper();
	private AtomicReference<String> statusString = new AtomicReference<String>();
	private JsonFactory jsonFactory = new JsonFactory();

	/**
	 * Creates a new EyesFirstProcess with the given parameters. The parameters
	 * as passed to the actual {@code org.mitre.eyesfirst.retinalthickness.App}
	 * app as command line arguments, so exactly what they mean is defined
	 * there.
	 * <p>
	 * There are two exceptions: {@code mcrPath} is used to determine how to
	 * launch MATLAB, and {@code jarFiles} is used to build the classpath used
	 * to launch the application.
	 * 
	 * @param wadoURL
	 *            the URL to download the DICOM object from
	 * @param dicomURL
	 *            the {@code dcmsnd} URL to send the resulting DICOM to
	 * @param callbackURL
	 *            the URL to send the processing result to
	 * @param callbackKey
	 *            a key used to inform the callback which process has completed
	 * @param outputPath
	 *            the path to store the output too
	 * @param mcrPath
	 *            the path to the MATLAB Component Runtime
	 * @param jarFiles
	 *            paths to include on the classpath, note that they're just
	 *            added to the classpath as-is
	 */
	public EyesFirstProcess(String wadoURL, String dicomURL, String callbackURL, String callbackKey, File outputPath, String mcrPath, File[] jarFiles) {
		if (wadoURL == null)
			throw new NullPointerException();
		setWadoURL(wadoURL);
		this.callbackURL = callbackURL;
		this.callbackKey = callbackKey;
		this.dicomURL = dicomURL;
		this.outputPath = outputPath;
		this.mcrPath = mcrPath;
		this.jarFiles = jarFiles;
	}

	public EyesFirstProcess(String wadoURL) {
		this(wadoURL, null, null, null, null, null, null);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Gets the WADO URL - the URL that will be used to download the DICOM file
	 * for processing.
	 * 
	 * @return the WADO URL
	 */
	public String getWadoURL() {
		return wadoURL;
	}

	/**
	 * Sets the WADO URL - the URL where the DICOM object to process is.
	 * 
	 * @param wadoURL
	 *            the URL where the DICOM object to process is
	 */
	public void setWadoURL(String wadoURL) {
		if (wadoURL == null)
			throw new NullPointerException();
		this.wadoURL = wadoURL;
		// Set the name to the "bad" version immediately
		name = "EyesFirst (bad WADO URL)";
		// Generate the name from the WADO URL. Really this would involve
		// parsing the WADO URL to be "correct", but, for simplicity's sake:
		try {
			URI uri = new URI(wadoURL);
			String query = uri.getRawQuery();
			if (query != null) {
				DicomID id = DicomID.fromQueryString(query);
				name = "EyesFirst: StudyUID = " + id.getStudyUID() + "; SeriesUID = " + id.getSeriesUID() + "; ObjectUID = " + id.getObjectUID();
			}
		} catch (IllegalArgumentException e) {
			// Ignore (handled above by setting the name to the "bad" value)
		} catch (URISyntaxException e) {
			// Ignore (handled above by setting the name to the "bad" value)
		}
	}

	public String getCallbackURL() {
		return callbackURL;
	}

	/**
	 * Sets the callback URL, which is used to notify the completion of
	 * processing. If {@code null}, no URL is invoked to notify of completion.
	 * 
	 * @param callbackURL
	 *            the callback URL.
	 */
	public void setCallbackURL(String callbackURL) {
		this.callbackURL = callbackURL;
	}

	public String getCallbackKey() {
		return callbackKey;
	}

	public void setCallbackKey(String callbackKey) {
		this.callbackKey = callbackKey;
	}

	public String getMcrPath() {
		return mcrPath;
	}

	public void setMcrPath(String mcrPath) {
		this.mcrPath = mcrPath;
	}

	public String getDicomURL() {
		return dicomURL;
	}

	/**
	 * Sets the DICOM URL - the URL used to store the created DICOM file.
	 * @param dicomURL
	 */
	public void setDicomURL(String dicomURL) {
		this.dicomURL = dicomURL;
	}

	public File[] getJarFiles() {
		return jarFiles;
	}

	/**
	 * Sets the JAR files. <strong>The array will be used as-is and will not be
	 * copied!</strong> If set to {@code null}, then no additional files will
	 * be added to the classpath.
	 * @param jarFiles the files to add to the classpath
	 */
	public void setJarFiles(File[] jarFiles) {
		this.jarFiles = jarFiles;
	}

	public File getOutputPath() {
		return outputPath;
	}

	/**
	 * Sets the output path - the path where the processed files will be stored.
	 * If set to {@code null}, the processed files will be stored in a temp
	 * directory.
	 * 
	 * @param outputPath
	 *            the path to store processed files
	 */
	public void setOutputPath(File outputPath) {
		this.outputPath = outputPath;
	}

	@Override
	protected void createJavaArguments(List<String> arguments) {
		if (outputPath != null) {
			arguments.add("-o");
			arguments.add(outputPath.getAbsolutePath());
			if (dicomURL != null) {
				arguments.add("-d");
				arguments.add(dicomURL);
			}
			if (callbackURL != null) {
				arguments.add("-c");
				arguments.add(callbackURL);
				if (callbackKey != null) {
					arguments.add("-k");
					arguments.add(callbackKey);
				}
			}
		}
		arguments.add(wadoURL);
	}

	@Override
	protected String getClassName() {
		return "org.mitre.eyesfirst.classifier.App";
	}

	@Override
	protected List<File> createClassPath() {
		List<File> res = super.createClassPath();
		if (jarFiles != null) {
			for (File f : jarFiles) {
				res.add(f);
			}
		}
		return res;
	}

	@Override
	protected void runProcess() throws Exception {
		statusString.set("Starting thickness classifier...");
		// Change to our process-specific logger
		log = LoggerFactory.getLogger(getClass().getName() + "." + ProcessManager.uidToString(getUID()));
		try {
			super.runProcess();
			statusString.set("Completed successfully.");
		} catch (Exception e) {
			statusString.set(null);
			log.error("Error running model", e);
			throw e;
		} finally {
			// Clean up.
			stdout.finish();
			String line;
			while ((line = stdout.readLine()) != null) {
				log.info(line);
			}
			stderr.finish();
			while ((line = stderr.readLine()) != null) {
				log.warn(line);
			}
		}
	}

	/**
	 * Attempts to configure the child environment appropriately to allow
	 * MATLAB to run. <strong>This code is platform dependent!</strong>
	 */
	@Override
	protected void configureProcess(ProcessBuilder processBuilder) {
		String platform = System.getProperty("os.name");
		Map<String, String> env = processBuilder.environment();
		String mcrroot = mcrPath;
		if (mcrroot == null || mcrroot.length() == 0)
			mcrroot = findMCRRoot(env);
		if (platform.startsWith("Windows")) {
			log.debug("Running under Windows");
			// TODO: Whatever we need to do for Windows
		} else if (platform.equals("Linux")) {
			log.debug("Running under Linux");
			configureUnix(mcrroot, env, "LD_LIBRARY_PATH", "glnx86", null);
		} else if (platform.equals("Mac OS X")) {
			// I think this also applies to BSD, but whatever
			log.debug("Running under Mac OS X");
			configureUnix(mcrroot, env, "DYLD_LIBRARY_PATH", "maci32", null);
		} else {
			log.warn("Unsupported platform {} - not setting MCR environment variables.", platform);
		}
	}

	private void configureUnix(String mcrroot, Map<String, String> env, String libraryPathName, String defaultArch, String arch) {
		if (mcrroot == null) {
			if (!env.containsKey(libraryPathName)) {
				log.warn("MCR Root not found and {} not set. MATLAB may break.", libraryPathName);
			}
			// If it's set, just assume it's right
			return;
		}
		if (arch == null) {
			// We can guess the arch, it's the only directory in mcrroot/runtime/,
			// usually.
			File f = new File(mcrroot);
			if (!f.exists()) {
				log.warn("Using MCR root at {}, but it doesn't exist...", mcrroot);
			} else {
				File runtime = new File(f, "runtime");
				String[] archs = runtime.list();
				if (archs == null) {
					log.warn("Configured MCR root {} does not contain a \"runtime\" directory.", mcrroot);
				} else {
					for (String a : archs) {
						if (a.length() > 0 && a.charAt(0) != '.') {
							arch = a;
							break;
						}
					}
					log.debug("Auto-detected arch as {}", arch);
				}
			}
			if (arch == null) {
				log.warn("Unable to auto-detect platform architecture, defaulting to {}", defaultArch);
				arch = defaultArch;
			}
		}
		String libraryPath = mcrroot + "/runtime/" + arch + ":" + 
				mcrroot + "/bin/" + arch + ":" +
				mcrroot + "/sys/os/" + arch;
		// GNU/Linux has some threading libraries we also need:
		if (arch.equals("glnx86")) {
			libraryPath = libraryPath + ":" +
					mcrroot + "/sys/java/jre/glnx86/jre/lib/i386/native_threads:" +
					mcrroot + "/sys/java/jre/glnx86/jre/lib/i386/server:" +
					mcrroot + "/sys/java/jre/glnx86/jre/lib/i386";
		} else if (arch.equals("glnxa64")) {
			// 64 bit has very similar but slightly different paths:
			libraryPath = libraryPath + ":" +
					mcrroot + "/sys/java/jre/glnxa64/jre/lib/amd64/native_threads:" +
					mcrroot + "/sys/java/jre/glnxa64/jre/lib/amd64/server:" +
					mcrroot + "/sys/java/jre/glnxa64/jre/lib/amd64";
		}
		env.put(libraryPathName, libraryPath);

		env.put("XAPPLRESDIR", mcrroot + "/X11/app-defaults");
		log.debug("{}={}", libraryPathName, libraryPath);
		log.debug("XAPPLRESDIR={}", env.get("XAPPLRESDIR"));
	}

	private final static String[] MCR_ROOT_ENV_VARS = { "MATLAB_HOME", "MCRROOT", "MATLABROOT" };
	public static String findMCRRoot(Map<String, String> env) {
		for (String s : MCR_ROOT_ENV_VARS) {
			String r = env.get(s);
			if (r != null)
				return r;
		}
		// TODO: Use other methods to locate the MCR
		return null;
	}

	@Override
	protected void receiveStdOut(byte[] b, int length) {
		stdout.write(b, 0, length);
		String line;
		while ((line = stdout.readLine()) != null) {
			log.info(line);
			if (line.startsWith("STATUS:")) {
				// Status message. Rest of line is a JSON object describing
				// the status.
				try {
					JsonParser parser = jsonFactory.createJsonParser(line.substring(7));
					if (parser.nextToken() == JsonToken.START_OBJECT) {
						// Read the fields. We only care about one value.
						while (parser.nextValue() != JsonToken.END_OBJECT) {
							if (parser.getCurrentName().equals("message")) {
								statusString.set(parser.getText());
							}
						}
					}
				} catch (Exception e) {
					// Dope.
					log.warn("Exception reading status message", e);
				}
			}
		}
	}

	@Override
	protected void receiveStdErr(byte[] b, int length) {
		stderr.write(b, 0, length);
		String line;
		while ((line = stderr.readLine()) != null) {
			log.warn(line);
		}
	}

	@Override
	public String getStatusString() {
		String status = statusString.get();
		return status == null ? super.getStatusString() : status;
	}
	
}
