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
package org.mitre.eyesfirst.classifier.port;

import java.util.Arrays;

/**
 * Tools for dealing with images.
 * @author dpotter
 */
public class ImageUtil {
	private ImageUtil() { }

	/**
	 * Kernel for image erosion/dilation.
	 * @author dpotter
	 */
	public static class Kernel {
		private final byte[] kernel;
		/**
		 * The x coordinate of the center of the kernel. 
		 */
		private final int centerX;
		private final int centerY;
		private final int width;
		private final int height;

		/**
		 * Creates a kernel of the given size. The center point will be half
		 * the width/height, rounded down. By default the entire kernel is
		 * filled (set to 1).
		 * @param width
		 * @param height
		 */
		public Kernel(int width, int height) {
			kernel = new byte[width * height];
			this.width = width;
			this.height = height;
			centerX = width / 2;
			centerY = height / 2;
			Arrays.fill(kernel, (byte) 1);
		}

		/**
		 * Gets the kernel. This is the internal array used, so modifications
		 * to it affect the matching done.
		 * @return
		 */
		public byte[] getKernel() {
			return kernel;
		}

		public int getCenterX() {
			return centerX;
		}

		public int getCenterY() {
			return centerY;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		/**
		 * Check to see if placing the kernel at the given x/y position "hits"
		 * either an "on" or "off" pixel.
		 * @param image
		 * @param width
		 * @param height
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean containsPixel(byte[] image, int width, int height, int x, int y, boolean on) {
			// There is a slight, but noticeable, speed-up by having special
			// "on/off" versions of the algorithm.
			return on ? containsOnPixel(image, width, height, x, y) : containsOffPixel(image, width, height, x, y);
		}

		/**
		 * Check to see if placing the kernel at the given x/y position "hits"
		 * either an "on" or "off" pixel.
		 * @param image
		 * @param width
		 * @param height
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean containsOnPixel(byte[] image, int width, int height, int x, int y) {
			int minX = x - centerX;
			int minY = y - centerY;
			int kx = 0;
			int ky = 0;
			if (minX < 0) {
				kx = -minX;
				minX = 0;
			}
			if (minY < 0) {
				ky = -minY;
				minY = 0;
			}
			int maxX = Math.min(x - centerX + this.width, width);
			int maxY = Math.min(y - centerY + this.height, height);
			// Within this area, test for a hit
			for (int ty = minY, tky = ky; ty < maxY; ty++, tky++) {
				int offset = ty * width;
				int kOffset = this.width * tky + kx;
				for (int tx = minX; tx < maxX; tx++) {
					if (kernel[kOffset] > 0 && image[offset + tx] != 0) {
						return true;
					}
					kOffset++;
				}
			}
			return false;
		}

		/**
		 * Check to see if placing the kernel at the given x/y position "hits"
		 * either an "on" or "off" pixel.
		 * @param image
		 * @param width
		 * @param height
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean containsOffPixel(byte[] image, int width, int height, int x, int y) {
			int minX = x - centerX;
			int minY = y - centerY;
			int kx = 0;
			int ky = 0;
			if (minX < 0) {
				kx = -minX;
				minX = 0;
			}
			if (minY < 0) {
				ky = -minY;
				minY = 0;
			}
			int maxX = Math.min(x - centerX + this.width, width);
			int maxY = Math.min(y - centerY + this.height, height);
			// Within this area, test for a hit
			for (int ty = minY, tky = ky; ty < maxY; ty++, tky++) {
				int offset = ty * width;
				int kOffset = this.width * tky + kx;
				for (int tx = minX; tx < maxX; tx++) {
					if (kernel[kOffset] > 0 && image[offset + tx] == 0) {
						return true;
					}
					kOffset++;
				}
			}
			return false;
		}
	}

	/**
	 * Executes a binary erosion
	 * @param image
	 * @param width
	 * @param height
	 * @param kernel
	 * @return
	 */
	public static byte[] binaryErode(byte[] image, int width, int height, Kernel kernel) {
		long start = System.currentTimeMillis();
		// The way this works is that if there is a pixel that is off inside the
		// kernel, the output pixel is off.
		int offset = 0;
		byte[] result = new byte[image.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				result[offset] = kernel.containsOffPixel(image, width, height, x, y) ? (byte) 0 : (byte) 1;
				offset++;
			}
		}
		System.out.println("Erode in " + (System.currentTimeMillis() - start) + "ms");
		return result;
	}

	public static byte[] binaryDilate(byte[] image, int width, int height, Kernel kernel) {
		// As above, there is a slight speed advantage to making these specific
		// erode/dilate versions, even though they're almost identical.
		long start = System.currentTimeMillis();
		// The way this works is that if there is a pixel that is on inside the
		// kernel, the output pixel is on.
		int offset = 0;
		byte[] result = new byte[image.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				result[offset] = kernel.containsOnPixel(image, width, height, x, y) ? (byte) 1 : (byte) 0;
				offset++;
			}
		}
		System.out.println("Dilate in " + (System.currentTimeMillis() - start) + "ms");
		return result;
	}

	/**
	 * Dilate or erode an image. Dilate and erode are effectively the same
	 * thing, just inverted: erode is identical to inverting the image, dilating,
	 * and reinverting the image.
	 * @param image
	 * @param width
	 * @param height
	 * @param kernel
	 * @param inverse {@code false} to erode rather than dilate
	 * @return
	 */
	public static byte[] binaryDilate(byte[] image, int width, int height, Kernel kernel, boolean dilate) {
		long start = System.currentTimeMillis();
		// The way this works is that if there is a pixel that is off inside the
		// kernel, the output pixel is off.
		int offset = 0;
		byte[] result = new byte[image.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				result[offset] = (kernel.containsPixel(image, width, height, x, y, dilate) ^ dilate) ? (byte) 0 : (byte) 1;
				offset++;
			}
		}
		System.out.println("Dilate/erode in " + (System.currentTimeMillis() - start) + "ms");
		return result;
	}
}
