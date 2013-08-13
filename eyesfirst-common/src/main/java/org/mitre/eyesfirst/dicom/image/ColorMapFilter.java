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
package org.mitre.eyesfirst.dicom.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RGBImageFilter;
import java.util.Arrays;

/**
 * Filter an input image using a color map similar to the ones available in
 * MATLAB.
 * <p>
 * This probably could be implemented as a {@code java.awt.image.LookupOp} but
 * considering that the documentation on how those works is incredibly useless,
 * it isn't. Instead it's an RGBImageFilter, which is also amazingly useless,
 * but whatever.
 * 
 * @author dpotter
 */
public class ColorMapFilter extends RGBImageFilter {
	public static class ColorStop implements Comparable<ColorStop> {
		private int where;
		private Color color;
		public ColorStop(int where, Color color) {
			super();
			this.where = where;
			this.color = color;
		}
		public int getWhere() {
			return where;
		}
		public void setWhere(int where) {
			this.where = where;
		}
		public Color getColor() {
			return color;
		}
		public void setColor(Color color) {
			this.color = color;
		}
		@Override
		public int compareTo(ColorStop other) {
			return where - other.where;
		}
	}

	private int[] palette;

	public ColorMapFilter(Color... colors) {
		// Generate colors that are equidistant between the stops.
	}

	public ColorMapFilter(ColorStop... stops) {
		// Sort the color stops by "where"
		Arrays.sort(stops);
		// And generate our palette.
	}

	@Override
	public int filterRGB(int x, int y, int rgb) {
		// Generate a gray
		int gray = (((rgb & 0xFF0000) >> 24) + ((rgb & 0xFF00) >> 16) + (rgb & 0xFF)) / 3;
		return palette[gray];
	}

	public BufferedImage filterInPlace(BufferedImage input) {
		// Screw the image API, just do this the dumb way
		int width = input.getWidth();
		int height = input.getHeight();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				input.setRGB(x, y, filterRGB(x, y, input.getRGB(x, y)));
			}
		}
		return input;
	}
}
