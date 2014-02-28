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
package org.eyesfirst.trial.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for generating thumbnail images.
 *
 * @author dpotter
 */
public class ThumbnailUtil {
	private static final Log log = LogFactory.getLog(ThumbnailUtil.class);
	private ThumbnailUtil() {
	}

	/**
	 * Generates a thumbnail based on the given source image. Note: image must
	 * be completely loaded (getWidth()/getHeight() must return correct values).
	 * Using ImageIO means this will always be true.
	 * @param source
	 */
	public static BufferedImage createThumbnail(Image source, int maxWidth, int maxHeight) {
		// Figure out the thumbnail size
		int width = source.getWidth(null);
		int height = source.getHeight(null);
		if (width < 0 || height < 0)
			throw new IllegalArgumentException("image not completely loaded");
		// First, try fitting to width
		int thumbnailWidth, thumbnailHeight = maxWidth * height / width;
		if (thumbnailHeight <= maxHeight) {
			// Use it.
			thumbnailWidth = maxWidth;
		} else {
			// Otherwise, go the other way
			thumbnailWidth = maxHeight * width / height;
			thumbnailHeight = maxHeight;
		}
		// Create the thumbnail!
		BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = thumbnail.createGraphics();
		g2.drawImage(source.getScaledInstance(thumbnailWidth, thumbnailHeight, Image.SCALE_SMOOTH), 0, 0, null);
		return thumbnail;
	}

	/**
	 * Generates a thumbnail based on the given source image. Note: image must
	 * be completely loaded (getWidth()/getHeight() must return correct values).
	 * Using ImageIO means this will always be true.
	 * 
	 * @param source
	 *            the source image
	 * @param aspectRatio
	 *            the correct aspect ratio of the source image, used to correct
	 *            images that do not have correct aspect ratios
	 * @param maxWidth
	 *            the maximum width of a thumbnail
	 * @param maxHeight
	 *            the maximum height of a thumbnail
	 */
	public static BufferedImage createThumbnail(Image source, double aspectRatio, int maxWidth, int maxHeight) {
		if (log.isDebugEnabled())
			log.debug("Create thumbnail AR=" + aspectRatio + ", within [" + maxWidth + "x" + maxHeight + "]");
		// Figure out the thumbnail size
		int width = source.getWidth(null);
		int height = source.getHeight(null);
		if (width < 0 || height < 0)
			throw new IllegalArgumentException("image not completely loaded");
		// First, calculate the actual aspect ratio
		double ar = ((double)width)/((double)height);
		// Next, calculate how we'd make the image square
		double sw, sh;
		if (width > height) {
			// We'd increase the height to make it square, so use the AR directly
			sw = 1.0;
			sh = ar;
		} else {
			// We'd increase the width, so use the inverse
			sw = 1/ar;
			sh = 1.0;
		}
		// Now that we have a "square", apply the desired aspect ratio
		sw *= aspectRatio;
		// Now that we have that, we can finally calculate the "actual size" of
		// the full image. (Which is, strictly speaking, "wrong" in cases where
		// the desired SR is < 1.0. But it doesn't matter, because the math
		// still comes out right.)
		double aw = width * sw, ah = height * sh;
		// And with these "actual" values, see which one fits best.
		// First, fit to width
		int thumbnailWidth;
		int thumbnailHeight = (int) Math.round(maxWidth * ah / aw);
		if (thumbnailHeight <= maxHeight) {
			// Use it
			thumbnailWidth = maxWidth;
		} else {
			// Otherwise, do the reverse
			thumbnailWidth = (int) Math.round(maxHeight * aw / ah);
			thumbnailHeight = maxHeight;
		}
		// SANITY CHECK
		double tnAR = ((double)thumbnailWidth) / ((double)thumbnailHeight);
		if (log.isDebugEnabled())
			log.debug("Creating thumbnail AR=" + aspectRatio + ", from source " + width + "x" + height + " thumbnail is " + thumbnailWidth + "x" + thumbnailHeight + ", thumbnail AR=" + tnAR);
		if (Math.abs(tnAR - aspectRatio) >= 0.125) {
			log.warn("Thumbnail created bad aspect ratio! Wanted " + aspectRatio + " but calculated a size that produces " + tnAR + "!");
		}
		// And, finally, create the thumbnail!
		BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = thumbnail.createGraphics();
		g2.drawImage(source.getScaledInstance(thumbnailWidth, thumbnailHeight, Image.SCALE_SMOOTH), 0, 0, null);
		return thumbnail;
	}

	public static byte[] createJPEG(RenderedImage image) throws IOException {
		return createImageBinary(image, "jpeg");
	}

	public static byte[] createPNG(RenderedImage image) throws IOException {
		return createImageBinary(image, "png");
	}

	public static byte[] createImageBinary(RenderedImage image, String format) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(8*1024);
		ImageIO.write(image, format, out);
		return out.toByteArray();
	}
}
