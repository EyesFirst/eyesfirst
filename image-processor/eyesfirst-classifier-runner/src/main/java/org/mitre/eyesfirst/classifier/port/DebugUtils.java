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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.TableColumnModel;

/**
 * Some things for making debugging easier.
 * @author dpotter
 */
public class DebugUtils {
	private DebugUtils() {
	}

	public static void showDebugImage(String title, byte[] image, int width, int height) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
		int x = 0;
		int y = 0;
		int off = 0xFF000080;
		int on = 0xFFFF0000;
		for (int i = 0; i < image.length; i++) {
			img.setRGB(x, y, image[i] == 0 ? off : on);
			x++;
			if (x >= width) {
				x = 0;
				y++;
			}
		}
		frame.getContentPane().add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}

	public static void showDebugImage(String title, double[] image, int width, int height) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
		// Find min/max of the image
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 1; i < image.length; i++) {
			if (!Double.isNaN(image[i])) {
				if (min > image[i]) {
					min = image[i];
				}
				if (max < image[i]) {
					max = image[i];
				}
			}
		}
		int x = 0;
		int y = 0;
		double range = max - min;
		System.out.println("Range is " + min + " to " + max + ", range is " + range);
		// Now that we have the max/min, we can generate the image
		for (int i = 0; i < image.length; i++) {
			if (Double.isNaN(image[i])) {
				img.setRGB(x, y, 0);
			} else {
				//System.out.print((int)(((image[i] - min) / range) * 255.0 + 0.5));
				//System.out.print(" ");
				img.setRGB(x, y, JET_COLORIZER.getColor((int)(((image[i] - min) / range) * 255.0 + 0.5)));
			}
			x++;
			if (x >= width) {
				x = 0;
				y++;
				//System.out.println();
			}
		}
		//System.out.println();
		frame.getContentPane().add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}

	public static void showDebugMatrix(String title, double[] matrix, int width, int height) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		MatrixTableModel tableModel = new MatrixTableModel(matrix, width, height);
		JTable table = new JTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		TableColumnModel columnModel = table.getColumnModel();
		for (int i = 0; i < width; i++) {
			columnModel.getColumn(i).setPreferredWidth(72);
		}
		JScrollPane scrollPane = new JScrollPane(table);
		JTable rowHeader = new JTable(tableModel.createRowTableModel());
		rowHeader.setPreferredScrollableViewportSize(new Dimension(48, 100));
		rowHeader.setDefaultRenderer(Object.class, table.getTableHeader().getDefaultRenderer());
		scrollPane.setRowHeaderView(rowHeader);
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		frame.setSize(640, 480);
		frame.setVisible(true);
	}

	public static class ColorizerPaletteEntry implements Comparable<ColorizerPaletteEntry> {
		private double value;
		int index;
		private Color color;
		public ColorizerPaletteEntry(double value, Color color) {
			this.value = value;
			index = -1;
			this.color = color;
		}
		@Override
		public int compareTo(ColorizerPaletteEntry other) {
			if (value >= 0 && other.value >= 0)
				return (int) Math.signum(value - other.value);
			else
				return index - other.index;
		}
		public double getValue() {
			return value;
		}
		public void setValue(double value) {
			this.value = value;
		}
		public Color getColor() {
			return color;
		}
		public void setColor(Color color) {
			this.color = color;
		}
	}

	public static class Colorizer {
		private int[] palette;
		public Colorizer(Color[] colors) {
			if (colors == null)
				throw new NullPointerException();
			if (colors.length < 2)
				throw new IllegalArgumentException("Must have at least two colors");
			ColorizerPaletteEntry[] palette = new ColorizerPaletteEntry[colors.length];
			// Take the colors array, and "normalize" it
			for (int i = 0; i < colors.length; i++) {
				palette[i] = new ColorizerPaletteEntry(Double.NaN, colors[i]);
				palette[i].index = i;
			}
			init(palette);
		}
		public Colorizer(ColorizerPaletteEntry[] colors) {
			for (int i = 0; i < colors.length; i++) {
				colors[i].index = i;
			}
			init(colors);
		}
		private void init(ColorizerPaletteEntry[] colors) {
			Arrays.sort(colors);
			int pad = 0;
			if (Double.isNaN(colors[0].getValue()) || colors[0].getValue() > 0) {
				pad++;
			}
			if (Double.isNaN(colors[0].getValue()) || colors[colors.length-1].getValue() < 255) {
				pad++;
			}
			if (pad > 0) {
				// We need to pad things
				ColorizerPaletteEntry[] nColors = new ColorizerPaletteEntry[colors.length + pad];
				if (colors[0].getValue() > 0) {
					// Need to pad bottom
					nColors[0] = new ColorizerPaletteEntry(0, colors[0].getColor());
					System.arraycopy(colors, 0, nColors, 1, colors.length);
				} else {
					System.arraycopy(colors, 0, nColors, 0, colors.length);
				}
				if (colors[colors.length-1].getValue() < 255) {
					nColors[nColors.length-1] = new ColorizerPaletteEntry(255, colors[colors.length-1].getColor());
				}
			}
			colors[0].setValue(0);
			colors[colors.length-1].setValue(255);
			// Now go through and add in any missing values using linear interpolation
			int start = 0;
			for (int i = 0; i < colors.length; i++) {
				if (colors[i].getValue() >= 0) {
					if (start + 1 < i) {
						// Set all values between start and this
						double value = colors[start].getValue();
						double interval = (colors[i].getValue() - value) / (i - start + 1);
						start++;
						for (; start < i; start++) {
							value += interval;
							colors[start].setValue(value);
						}
					}
					start = i;
				}
			}
			// And, finally, create our palette:
			palette = new int[256];
			for (int i = 1; i < colors.length; i++) {
				Color c1 = colors[i-1].getColor();
				Color c2 = colors[i].getColor();
				System.out.println("Entry " + i + ", from " + c1 + " to " + c2 + ", " + colors[i-1].getValue() + " to " + colors[i].getValue());
				int r1 = c1.getRed(),
					g1 = c1.getGreen(),
					b1 = c1.getBlue(),
					r2 = c2.getRed(),
					g2 = c2.getGreen(),
					b2 = c2.getBlue();
				double a = 1;
				double deltaA = 1 / (colors[i].getValue() - colors[i-1].getValue());
				for (int p = (int)(colors[i-1].getValue()); p <= colors[i].getValue(); p++) {
					palette[p] = 0xFF000000 |
						(((int)(r1 * a + r2 * (1-a))) << 16) |
						(((int)(g1 * a + g2 * (1-a))) << 8) |
						  (int)(b1 * a + b2 * (1-a));
					a -= deltaA;
				}
			}
			System.out.println("Palette: " + Arrays.toString(palette));
		}
		public int getColor(int value) {
			if (value < 0)
				return palette[0];
			if (value >= palette.length)
				return palette[palette.length-1];
			return palette[value];
		}
	}

	public static final Colorizer JET_COLORIZER = new Colorizer(new Color[] {
			new Color(0, 0, 128),
			new Color(0, 0, 255),
			new Color(0, 255, 255),
			new Color(0, 255, 0),
			new Color(255, 255, 0),
			new Color(255, 0, 0),
			new Color(128, 0, 0)
	});

	public static void main(String[] args) {
		showDebugMatrix("Test", new double[100], 10, 10);
	}
}
