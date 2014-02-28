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

import java.awt.Image
import java.awt.image.BufferedImage

import org.eyesfirst.trial.util.ThumbnailUtil

/**
 * Artifacts are basically any sort of image or collection of images. They can
 * contain various types of images, but the artifact itself is the core way of
 * accessing it.
 * @author dpotter
 */
class Artifact {
	// FIXME: Both of these constants should be in Config.groovy
	public static final int THUMBNAIL_MAX_WIDTH = 150;
	public static final int THUMBNAIL_MAX_HEIGHT = 50;
	/**
	 * The name of the artifact.
	 */
	String name;
	/**
	 * The time the artifact was taken.
	 */
	Date timestamp;
	/**
	 * The type of the artifact.
	 */
	ArtifactType type;
	/**
	 * The laterality of the artifact. Should generally be OD or OS, but is a
	 * two-character nullable string anyway.
	 */
	String laterality;
	/**
	 * Artifact thumbnail.
	 */
	ArtifactThumbnail thumbnail;
	/**
	 * The actual artifact data, if present.
	 */
	ArtifactData data;

	static belongsTo = [ patient: Patient ];
	static hasOne = [
		thicknessMap: org.eyesfirst.trial.oct.ThicknessMap,
		synthesizedFundusPhoto: org.eyesfirst.trial.oct.SynthesizedFundusPhoto,
		classifierResults: org.eyesfirst.trial.oct.ClassifierResults
	];

	static constraints = {
		type nullable: false
		thumbnail nullable: true
		data nullable: true
		laterality size: 1..4, nullable: true
		thicknessMap nullable: true
		synthesizedFundusPhoto nullable: true
		classifierResults nullable: true
	}

	static mapping = {
		type fetch: 'join' // Be eager about type, it's almost always needed
	}

	/**
	 * Generates a thumbnail based on the given source image. Note: image must
	 * be completely loaded (getWidth()/getHeight() must return correct values).
	 * Using ImageIO means this will always be true.
	 * @param source
	 */
	void createThumbnail(Image source, double aspectRatio = 1.0) {
		BufferedImage thumbnailImage;
		if (aspectRatio == 1.0)
			thumbnailImage = ThumbnailUtil.createThumbnail(source, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
		else
			thumbnailImage = ThumbnailUtil.createThumbnail(source, aspectRatio, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
		saveThumbnail(thumbnailImage);
	}

	/**
	 * Saves the given thumbnail image
	 * @param thumbnailImage the image to save
	 */
	void saveThumbnail(BufferedImage thumbnailImage) {
		ArtifactThumbnail tn = thumbnail;
		if (tn == null) {
			// Create a new thumbnail
			tn = new ArtifactThumbnail(artifact: this);
		}
		// Set the thumbnail data
		tn.image = ThumbnailUtil.createJPEG(thumbnailImage)
		tn.width = thumbnailImage.width;
		tn.height = thumbnailImage.height;
		thumbnail = tn;
	}
}
