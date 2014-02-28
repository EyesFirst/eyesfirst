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

class HardExudateCluster {
	double maxCfarValue;
	double normalScore;
	int layer;
	// center - no values so far, so skipped
	// radius - same as above
	int numVoxels;
	int boundingBoxX;
	int boundingBoxY;
	int boundingBoxZ;
	int boundingBoxWidth;
	int boundingBoxHeight;
	int boundingBoxDepth;
	double layerProportion;
	/*
	 * Don't care for now:
	int ellipseCenterX;
	int ellipseCenterY;
	int ellipseCenterZ;
	int boundingBoxMinCornerShiftX;
	int boundingBoxMinCornerShiftY;
	int boundingBoxMinCornerShiftZ;
	*/

	static constraints = {
	}

	static belongsTo = [ classifierResults: ClassifierResults ];
}
