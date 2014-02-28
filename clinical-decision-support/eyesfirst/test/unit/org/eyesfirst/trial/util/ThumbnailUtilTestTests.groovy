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
package org.eyesfirst.trial.util

import static org.junit.Assert.*

import java.awt.image.BufferedImage;

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class ThumbnailUtilTestTests {
	BufferedImage generateTestImage(int width, int height) {
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	void setUp() {
	}

	void tearDown() {
	}

	void testWideThumbnail() {
		BufferedImage timg = generateTestImage(400, 200);
		BufferedImage res = ThumbnailUtil.createThumbnail(timg, 32, 32);
		assert res.width == 32
		assert res.height == 16
		res = ThumbnailUtil.createThumbnail(timg, 64, 32);
		assert res.width == 64
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 32, 64);
		assert res.width == 32
		assert res.height == 16
	}

	void testTallThumbnail() {
		BufferedImage timg = generateTestImage(200, 400);
		BufferedImage res = ThumbnailUtil.createThumbnail(timg, 32, 32);
		assert res.width == 16
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 64, 32);
		assert res.width == 16
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 32, 64);
		assert res.width == 32
		assert res.height == 64
	}

	void testWideThumbnailWithWideAspectRatio() {
		BufferedImage timg = generateTestImage(400, 200);
		BufferedImage res = ThumbnailUtil.createThumbnail(timg, 1.5, 32, 32);
		assert res.width == 32
		assert res.height == 21
		res = ThumbnailUtil.createThumbnail(timg, 1.5, 64, 32);
		assert res.width == 48
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 1.5, 32, 64);
		assert res.width == 32
		assert res.height == 21
	}

	void testTallThumbnailWithWideAspectRatio() {
		BufferedImage timg = generateTestImage(200, 400);
		BufferedImage res = ThumbnailUtil.createThumbnail(timg, 1.5, 32, 32);
		assert res.width == 32
		assert res.height == 21
		res = ThumbnailUtil.createThumbnail(timg, 1.5, 64, 32);
		assert res.width == 48
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 1.5, 32, 64);
		assert res.width == 32
		assert res.height == 21
	}

	void testWideThumbnailWithTallAspectRatio() {
		BufferedImage timg = generateTestImage(400, 200);
		BufferedImage res = ThumbnailUtil.createThumbnail(timg, 0.5, 32, 32);
		assert res.width == 16
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 0.5, 64, 32);
		assert res.width == 16
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 0.5, 32, 64);
		assert res.width == 32
		assert res.height == 64
	}

	void testTallThumbnailWithTallAspectRatio() {
		BufferedImage timg = generateTestImage(200, 400);
		BufferedImage res = ThumbnailUtil.createThumbnail(timg, 0.5, 32, 32);
		assert res.width == 16
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 0.5, 64, 32);
		assert res.width == 16
		assert res.height == 32
		res = ThumbnailUtil.createThumbnail(timg, 0.5, 32, 64);
		assert res.width == 32
		assert res.height == 64
	}
}
