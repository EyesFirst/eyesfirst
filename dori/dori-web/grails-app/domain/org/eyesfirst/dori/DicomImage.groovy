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
package org.eyesfirst.dori

class DicomImage {

	String rawQueryString			//String used to access the image over WADO
	String processedQueryString		//String used to access the processed image
	String classifierDiagnoses		//Automated diagnosis results
	String clinicalInterpretation	//Clinician's assessment of the image
	String clinicalFeedback
	Double jointStatistic
	String objectUid				//SOP Instance UID of raw DICOM object
	byte[] thicknessMap				//Thickness map, a PNG image
	FundusPhoto fundusPhoto			//Fundus Photo, a PNG image
	FundusPhoto synthesizedFundusPhoto
	Date dateCreated
	Date lastUpdated

	static belongsTo = [efid : Efid]
	static hasMany = [feedback : Feedback, diagnoses: Diagnosis]

	static mapping = {
		version false
		rawQueryString index:'raw_query_idx'
		processedQueryString index:'processed_query_idx'
	}

	static constraints = {
		rawQueryString blank: false
		processedQueryString nullable: true
		classifierDiagnoses nullable: true
		classifierDiagnoses maxSize: 100000
		clinicalInterpretation nullable: true
		clinicalFeedback nullable: true
		clinicalFeedback maxSize: 100000
		jointStatistic nullable: true
		clinicalInterpretation maxSize: 100000
		thicknessMap nullable: true
		thicknessMap maxSize: 1000000000
		fundusPhoto nullable: true
		synthesizedFundusPhoto nullable: true
	}
}
