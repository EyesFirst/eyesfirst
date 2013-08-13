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
package org.eyesfirst.dori

class ImageAnnotation {
	/**
	 * The user that created the image. Note that at present the creator is
	 * set to be nullable: false, but that means users cannot be deleted without
	 * also deleting all their annotations, which is probably not desireable.
	 */
	User creator;
	DicomImage image;
	boolean processed;
	int x;
	int y;
	int slice;
	int width;
	int height;
	int depth;
	String annotation;

	// This belongs to the image, not the creator. Conceptually I suppose it
	// could belong to both?
	static belongsTo = [ image:DicomImage ]

	static constraints = {
		creator nullable: false
		image nullable: false
		x nullable: false, min: 0
		y nullable: false, min: 0
		slice nullable: false, min: 0
		width nullable: false, min: 1
		height nullable: false, min: 1
		depth nullable: false, min: 1
		annotation nullable: false, maxSize: 60000
	}
}
