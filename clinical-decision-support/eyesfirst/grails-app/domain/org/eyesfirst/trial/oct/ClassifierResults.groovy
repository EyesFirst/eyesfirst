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
package org.eyesfirst.trial.oct

import org.eyesfirst.trial.Artifact;

class ClassifierResults {
	Artifact artifact;
	double abnormalThicknessPFA;
	double abnormalThicknessPD;
	double abnormalThicknessSpecificity;
	double abnormalThicknessSensitivity;
	double abnormalThicknessJointAnomStat;
	double hardExudatesPFA;
	double hardExudatesPD;
	double thickness;
	int hardExudateCount;

	static hasMany = [ hardExudates: HardExudateCluster ];

	static constraints = {
	}

	def beforeUpdate() {
		hardExudateCount = hardExudates.size();
	}

	def beforeInsert() {
		hardExudateCount = hardExudates.size();
	}
}
