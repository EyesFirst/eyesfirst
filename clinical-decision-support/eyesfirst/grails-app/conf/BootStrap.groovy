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
import grails.util.GrailsUtil

import java.text.SimpleDateFormat

import org.apache.commons.logging.LogFactory
import org.eyesfirst.trial.Artifact
import org.eyesfirst.trial.ArtifactType
import org.eyesfirst.trial.Patient
import org.eyesfirst.trial.util.MockDicomAccessService
import org.mitre.eyesfirst.dicom.BasicDicomImage
import org.eyesfirst.User
import org.eyesfirst.Role
import org.eyesfirst.UserRole

class BootStrap {
	private static final log = LogFactory.getLog(this)
	/**
	 * Minimum test data birthday (Jan 1, 1950)
	 */
	private static long MIN_BIRTHDAY = -631134000000
	/**
	 * Maximum test data birthday (Jan 1, 1990)
	 */
	private static long MAX_BIRTHDAY = 631170000000
	def grailsApplication
	def octImageService
	def fundusImageService
	def actualDicomAccessService

	def init = { servletContext ->
		// Create the built-in types, if necessary
		if (ArtifactType.list().size == 0) {
			octImageService.getOCTType()
			fundusImageService.getFundusType()
		}
		if (grailsApplication.config.eyesfirst?.loadSampleData) {
			createSampleArtifacts(servletContext);
		}
		// At present, *always* create sample users, as there won't be any otherwise
		def adminRole = new Role(authority: 'ROLE_ADMIN').save(flush: true)
		def clinicianRole = new Role(authority: 'ROLE_CLINICIAN').save(flush: true)
		def admin = new User(username: 'admin', enabled: true, password: 'password').save(flush: true)
		UserRole.create admin, adminRole, true
		UserRole.create admin, clinicianRole, true
		def clinician = new User(username: 'clinician', enabled: true, password: 'password').save(flush: true)
		UserRole.create clinician, clinicianRole, true
	}
	def destroy = {
	}

	def createSampleArtifacts(def servletContext) {
		MockDicomAccessService dicomAccessService = null;
		if (actualDicomAccessService instanceof MockDicomAccessService)
			dicomAccessService = (MockDicomAccessService) actualDicomAccessService;
		// Load the sample patients
		InputStream s = servletContext.getResourceAsStream("/WEB-INF/testData/patients.csv");
		if (s == null) {
			log.warn("No sample patients present, skipping entire test data load!");
			return;
		}
		Reader reader = new InputStreamReader(s, "UTF-8");
		// Skip the first line
		reader.readLine()
		SimpleDateFormat birthdayFormat = new SimpleDateFormat("yyyy-MM-dd");
		reader.splitEachLine(","){line->
			// Last Name,First Name,Birthdate (yyyy-MM-dd),Gender,MRN
			Date birthday
			if (line[2]) {
				try {
					birthday = birthdayFormat.parse(line[2]);
				} catch (Exception e) {
					log.warn("Unable to parse " + line[2] + " as a date", e);
				}
			}
			if (birthday == null) {
				// Randomly generate a birthday
				birthday = new Date(((long)(Math.random() * (MAX_BIRTHDAY - MIN_BIRTHDAY))) + MIN_BIRTHDAY);
			}
			new Patient(lastName: line[0], firstName: line[1], birthday: birthday, gender: line[3], mrn: line[4]).save()
		}
		reader.close();
		s = servletContext.getResourceAsStream("/WEB-INF/testData/artifacts.csv");
		if (s == null) {
			log.warn("No sample artifacts present, not loading anything!");
			return;
		}
		reader = new InputStreamReader(s, "UTF-8");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		// Skip the first line
		reader.readLine()
		reader.splitEachLine(","){line->
			// Field order:
			String lastName = line[0];
			String firstName = line[1];
			String laterality = line[2];
			String fundusFile = line[3];
			//String visitDate = line[4];
			String octDicomFile = line[5];
			String thicknessMapFile = line[6];
			String synthesizedFundusFile = line[7];
			String classifierResultsFile = line[8];
			String thickness = line[9];
			Date visitDate;
			if (line[4]) {
				visitDate = dateFormat.parse(line[4]);
			} else {
				visitDate = new Date();
			}
			// Try and find the patient
			Patient p = Patient.findByLastNameAndFirstName(lastName, firstName);
			if (p == null) {
				p = new Patient(lastName: lastName, firstName: firstName);
				p.save();
			}
			if (fundusFile) {
				try {
					fundusImageService.importFundus(p, fundusFile, visitDate, laterality, servletContext.getResourceAsStream("/WEB-INF/testData/" + fundusFile).getBytes());
					log.info("Added fundus to " + p.firstName + " " + p.lastName);
				} catch (Exception e) {
					log.warn("Unable to load fundus for " + p.firstName + " " + p.lastName, e);
				}
			}
			if (octDicomFile) {
				try {
					String dicomID = octDicomFile;
					if (dicomAccessService != null) {
						// Try and load the image
						InputStream stream = servletContext.getResourceAsStream("/WEB-INF/testData/" + octDicomFile);
						if (stream != null) {
							BasicDicomImage image = new BasicDicomImage(stream.getBytes());
							dicomAccessService.addDicomImage(image);
							dicomID = image.getDicomID().toQueryString();
						}
					}
					Artifact artifact = octImageService.importOCT(p, dicomID);
					// Change the date in the artifact to match the CSV file,
					// if it was given
					if (line[4])
						artifact.timestamp = visitDate;
					String type = "OCT"
					if (thicknessMapFile) {
						try {
							octImageService.importThicknessMap(artifact, new Date(), "image/png", servletContext.getResourceAsStream("/WEB-INF/testData/" + thicknessMapFile).getBytes());
							type = type + " and thickness map"
						} catch (Exception e) {
							log.warn("Unable to import thickness map for " + p.firstName + " " + p.lastName, e);
						}
					}
					if (synthesizedFundusFile) {
						try {
							octImageService.importSynthesizedFundusPhoto(artifact, new Date(), "image/png", servletContext.getResourceAsStream("/WEB-INF/testData/" + synthesizedFundusFile).getBytes());
							type = type + " with synthesized fundus photo"
						} catch (Exception e) {
							log.warn("Unable to import synthesized fundus photo for " + p.firstName + " " + p.lastName, e);
						}
					}
					log.info("Added " + type + " to " + p.firstName + " " + p.lastName);
					if (classifierResultsFile) {
						try {
							octImageService.importClassifierResults(artifact, servletContext.getResourceAsStream("/WEB-INF/testData/" + classifierResultsFile), thickness.isDouble() ? thickness.toDouble() : 0);
							log.info("Added classifier results for " + p.firstName + " " + p.lastName);
						} catch (Exception e) {
							log.warn("Unable to import classifier results for " + p.firstName + " " + p.lastName, e);
						}
					}
				} catch (Exception e) {
					log.warn("Unable to import OCT for " + p.firstName + " " + p.lastName, e);
				}
			}
		}
		reader.close();
	}
}
