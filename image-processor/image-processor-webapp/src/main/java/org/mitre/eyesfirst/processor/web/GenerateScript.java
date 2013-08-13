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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

import org.mitre.eyesfirst.dicom.DicomID;

/**
 * Generate a shell script to move certain data files out of the image processor
 * data file. This was a utility that proved useful once and may prove useful
 * again in the future, but is currently <em>extremely</em> application specific.
 * @author dpotter
 *
 */
public class GenerateScript {

	public GenerateScript() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		// FIXME: A ton of stuff should be specified by the command line and not
		// directly.
		try {
			HashMap<DicomID, String> imageIDMap = new HashMap<DicomID, String>();
			BufferedReader in = new BufferedReader(new FileReader("/Users/dpotter/EyesFirst/eyesfirst_laptop_ids.csv"));
			String line;
			while ((line = in.readLine()) != null) {
				String[] fields = line.split(",");
				if (fields.length < 5) {
					System.err.println("Skipping line with invalid number of fields");
				} else {
					imageIDMap.put(new DicomID(fields[2], fields[3], fields[4]), fields[1]);
				}
			}
			in.close();
			for (int i = 0; i < args.length; i++) {
				in = new BufferedReader(new FileReader(args[i]));
				
				while ((line = in.readLine()) != null) {
					String[] fields = line.split(",");
					if (fields.length < 5) {
						System.err.println("Skipping line with invalid number of fields");
					} else {
						// See if we have this field already
						DicomID id = new DicomID(fields[2], fields[3], fields[4]);
						String imageID = imageIDMap.get(id);
						if (imageID != null) {
							System.out.println("mkdir -p ~/EyesFirst/clusterFeatures/" + fields[0]);
							System.out.println("# " + fields[2] + ", " + fields[3] + ", " + fields[4]);
							System.out.println("cp " +  RunProcessServlet.createHash(fields[3], fields[2], fields[4]) + "/storeClusterFeatures/original_cfar_clusterFeatures.mat ~/EyesFirst/clusterFeatures/" + fields[0] + "/" + imageID + "_original_cfar_clusterFeatures.mat");
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error");
			e.printStackTrace();
		}
	}
}
