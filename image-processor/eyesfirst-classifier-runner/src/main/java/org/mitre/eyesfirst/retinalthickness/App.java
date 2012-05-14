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
package org.mitre.eyesfirst.retinalthickness;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This provides a main method for running the Matlab code.
 */
public class App {
	private static Options createOptions() {
		Options options = new Options();
		Option out = new Option("o", "out", true, "the directory to create output in (default: create a temp directory)");
		out.setArgName("outdir");
		options.addOption(out);
		Option callback = new Option("c", "callback", true, "the URL to send a callback informing completion to");
		callback.setArgName("URL");
		options.addOption(callback);
		Option dicom = new Option("d", "dicom", true, "where to send the DICOM file to, in the form <aet>[@<host>[:<port>]]");
		dicom.setArgName("URL");
		options.addOption(dicom);
		return options;
	}

	private static void usage(Options options) {
		new HelpFormatter().printHelp("java " + App.class.getName() + " [options] <WADOURI>", options);
	}

	/**
	 * Run the application.
	 * <p>
	 * <strong>NOTE: Calls {@link System#exit(int)} in all code paths!</strong>
	 * You've been warned.
	 * @param args
	 */
	public static void main(String[] args) {
		// Special, "secret" option
		if (args.length == 1 && args[0].equals("--run-simple-test")) {
			System.out.println("Working!");
			System.exit(42);
		}
		// Create CLI  parser.
		Options options = createOptions();
		GnuParser parser = new GnuParser();
		CommandLine cl;
		try {
			cl = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Unable to parse arguments: " + e);
			usage(options);
			System.exit(1);
			return;
		}
		try {
			File destDir = null;
			String out = cl.getOptionValue("out");
			if (out == null) {
				destDir = File.createTempFile("retinalthickness", "");
				// This will create the file, but we want a directory, so:
				if (!destDir.delete()) {
					throw new IOException("Unable to delete temp file " + destDir);
				}
				if (!destDir.mkdirs()) {
					throw new IOException("Unable to create temp output directory " + destDir);
				}
			} else {
				destDir = new File(out);
			}
			String[] left = cl.getArgs();
			if (left.length < 1) {
				System.out.println("Missing required argument WADO URI.");
				usage(options);
				System.exit(1);
			}
			if (left.length > 1) {
				System.err.println("Ignoring extra URIs, only processing \"" + left[0] + "\".");
			}
			URI wadoURI;
			try {
				wadoURI = new URI(left[0]);
			} catch (URISyntaxException e) {
				System.err.println("Unable to parse WADO URI: " + e);
				System.exit(1);
				return;
			}
			String callback = cl.getOptionValue("callback");
			URI callbackURI;
			try {
				callbackURI = new URI(callback);
			} catch (URISyntaxException e) {
				System.err.println("Unable to parse callback URI: " + e);
				System.exit(1);
				return;
			}
			ThicknessClassifier classifier = new ThicknessClassifier(wadoURI, destDir, cl.getOptionValue("dicom"), callbackURI);
			classifier.runAll();
			// Matlab will spawn a bunch of windows. Kill them.
			System.exit(0);
		} catch (Exception e) {
			System.err.println("Unable to run thickness classifier:");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
