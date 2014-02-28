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
package org.eyesfirst.trial

import java.awt.image.BufferedImage

import javax.imageio.ImageIO

class FundusImageService {

	def getFundusType() {
		return ArtifactType.getOrCreate("fundus", "Fundus photo");
	}

	def importFundus(Patient patient, String name, Date timestamp, String laterality, byte[] fundusPhoto) {
		Artifact artifact = new Artifact(name: name, timestamp: timestamp, laterality: laterality);
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(fundusPhoto));
		artifact.createThumbnail(image);
		artifact.data = new ArtifactData(mimeType: "image/jpeg", data: fundusPhoto);
		artifact.type = fundusType;
		patient.addToArtifacts(artifact);
		artifact.save();
		patient.save();
	}
}
