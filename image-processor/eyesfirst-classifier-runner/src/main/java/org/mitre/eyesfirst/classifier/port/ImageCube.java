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

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexField;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.FieldMatrixChangingVisitor;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixChangingVisitor;
import org.mitre.eyesfirst.dicom.BasicDicomImage;
import org.mitre.eyesfirst.dicom.DicomImageException;

/**
 * An image cube. Note: this stores the values as unsigned bytes, which Java
 * doesn't really support. There are conversion functions for getting the slices
 * using doubles.
 * @author dpotter
 */
public class ImageCube {
	private final byte[] data;
	private final int width;
	private final int height;
	private final int depth;
	private final int sliceSize;

	public ImageCube(int width, int height, int depth) {
		if (width < 1 || height < 1 || depth < 1)
			throw new IllegalArgumentException("Invalid cube dimensions " + width + "x" + height + "x" + depth);
		this.width = width;
		this.height = height;
		this.depth = depth;
		sliceSize = width * height;
		this.data = new byte[sliceSize * depth];
	}

	/**
	 * Gets the entire data array. The data is stored in an array such that
	 * converting to an actual index is done via:
	 * {@code z * sliceSize + y * width + x}.
	 * 
	 * @return a reference to the actual data array, changing the returned array
	 *         changes the values retrieved through this class
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Gets a single slice. Since Java doesn't support the concept of taking
	 * slices of arrays, this returns a copy of the array. (Supposedly some
	 * JRE implementations are smart enough to return a copy-on-write view, so
	 * performance may not be as bad as you might expect, but who knows.)
	 * @param slice
	 * @return
	 */
	public byte[] getSlice(int slice) {
		if (slice < 0 || slice >= depth)
			throw new IllegalArgumentException("Requested slice " + slice + " is out of bounds (0-" + (depth-1) + ")");
		return Arrays.copyOfRange(data, slice * sliceSize, (slice+1) * sliceSize);
	}

	/**
	 * Returns the slice as a "complex" slice - each pixel is represented by
	 * two double values, x + yi.
	 * @param slice
	 * @return
	 */
	public double[] getComplexSlice(int slice) {
		double[] result = new double[sliceSize*2];
		int last = (slice + 1) * sliceSize;
		int resultIndex = 0;
		for (int i = slice * sliceSize; i < last; i++) {
			result[resultIndex] = data[i] & 0xFF;
			resultIndex += 2;
		}
		return result;
	}

	/**
	 * Returns the slice as a "real" slice - each pixel is represented by
	 * a single double value.
	 * @param slice
	 * @return
	 */
	public double[] getRealSlice(int slice) {
		double[] result = new double[sliceSize];
		int last = (slice + 1) * sliceSize;
		int resultIndex = 0;
		for (int i = slice * sliceSize; i < last; i++) {
			result[resultIndex] = data[i] & 0xFF;
			resultIndex++;
		}
		return result;
	}

	/* *
	 * Returns the slice as a "complex" slice - each pixel is represented by
	 * two double values.
	 * @param slice
	 * @return
	 * /
	public Complex[] getCommonsComplexSlice(int slice) {
		Complex[] result = new Complex[sliceSize];
		int last = (slice + 1) * sliceSize;
		int resultIndex = 0;
		for (int i = slice * sliceSize; i < last; i++) {
			result[resultIndex] = new Complex(data[i]);
			resultIndex++;
		}
		return result;
	}*/

	/* *
	 * Returns the slice as a "complex" slice - each pixel is represented by
	 * two double values.
	 * @param slice
	 * @return
	 * /
	public double[][] getCommonsComplexDoubleSlice(int slice) {
		double[][] result = new double[2][];
		result[0] = new double[sliceSize];
		result[1] = new double[sliceSize];
		int last = (slice + 1) * sliceSize;
		int resultIndex = 0;
		for (int i = slice * sliceSize; i < last; i++) {
			result[0][resultIndex] = data[i];
			// Since Java initializes arrays to 0, we can skip this:
			//result[1][resultIndex] = 0.0;
			resultIndex++;
		}
		return result;
	}*/

	public RealMatrix getSliceAsMatrix(final int slice) {
		// matrix is rows, columns which corresponds to height, width
		RealMatrix result = MatrixUtils.createRealMatrix(height, width);
		result.walkInOptimizedOrder(new RealMatrixChangingVisitor() {
			final int start = slice * sliceSize;
			final int width = ImageCube.this.width;
			@Override
			public double visit(int row, int column, double value) {
				return data[start + row * width + column] & 0xFF;
			}

			@Override
			public void start(int rows, int columns, int startRow, int endRow,
					int startColumn, int endColumn) {
				// Don't care
			}

			@Override
			public double end() {
				// Don't care
				return 0;
			}
		});
		return result;
	}

	public FieldMatrix<Complex> getSliceAsComplexMatrix(final int slice) {
		// matrix is rows, columns which corresponds to height, width
		FieldMatrix<Complex> result = MatrixUtils.createFieldMatrix(ComplexField.getInstance(), height, width);
		result.walkInOptimizedOrder(new FieldMatrixChangingVisitor<Complex>() {
			final int start = slice * sliceSize;
			final int width = ImageCube.this.width;
			
			@Override
			public Complex visit(int row, int column, Complex value) {
				return Complex.valueOf((double)(((int)data[start + row * width + column]) & 0xFF));
			}
			
			@Override
			public void start(int rows, int columns, int startRow, int endRow,
					int startColumn, int endColumn) {
				// Don't care
			}
			
			@Override
			public Complex end() {
				// Don't care
				return null;
			}
		});
		return result;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getDepth() {
		return depth;
	}

	/**
	 * Gets the "slice size", the number of pixels in a single slice. This is
	 * simply {@code width * height} but is frequently needed to compute the
	 * appropriate offset into the data array.
	 * @return
	 */
	public int getSliceSize() {
		return sliceSize;
	}

	public int getPixel(int x, int y, int z) {
		if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth)
			throw new IllegalArgumentException("Requested point (" + x + ", " + y + ", " + z + ") is outside [ " + width + " x " + height + " x " + depth + "]");
		return data[z * sliceSize + y * width + x] & 0xFF;
	}

	public void setPixel(int x, int y, int z, int value) {
		if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth)
			throw new IllegalArgumentException("Requested point (" + x + ", " + y + ", " + z + ") is outside [ " + width + " x " + height + " x " + depth + "]");
		data[z * sliceSize + y * width + x] = (byte) value;
	}

	/**
	 * Copy one slice out of another image cube
	 * @param source the cube to copy from
	 * @param slice the slice to copy
	 * @param xShift the x shift to shift the source slice onto this cube
	 * @param yShift the y shift to shift the source slice onto this cube
	 */
	public void copySlice(ImageCube source, int slice, int xShift, int yShift) {
		// Make sure the copy will fit
		if (xShift < 0 || yShift < 0 ||
				(source.width + xShift) > width ||
				(source.height + yShift) > height)
			throw new IllegalArgumentException("Invalid shift " + xShift + ", " + yShift + " (places it outside cube)");
		int sourceStart = slice * source.sliceSize;
		int destStart = slice * sliceSize + yShift * width + xShift;
		for (int y = 0; y < source.height; y++) {
			System.arraycopy(source.data, sourceStart, data, destStart, source.width);
			sourceStart += source.width;
			destStart += width;
		}
	}

	/**
	 * Finds the maximum value in the image cube and returns that.
	 * @return
	 */
	public int findMax() {
		int max = 0;
		for (int i = 0; i < data.length; i++) {
			int d = data[i] & 0xFF;
			// If this is already the max, just return that
			if (d == 255)
				return 255;
			if (d > max)
				max = d;
		}
		return max;
	}

	/**
	 * @deprecated Not really, but this is currently full of debug code that
	 * needs to be removed.
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws DicomImageException
	 */
	public static ImageCube fromDicom(InputStream in) throws IOException, DicomImageException {
		// Rather than deal with loading images ourselves, throw it off to
		// BasicDicomImage.
		BasicDicomImage image = new BasicDicomImage(in);
		// FIXME: This only deals with 8-bit images
		int slices = image.getSliceCount();
		if (slices < 1)
			throw new DicomImageException("Unable to load slice count");
		// Grab the first slice to get the width/height
		BufferedImage slice = image.getSlice(0);
		int width = slice.getWidth();
		int height = slice.getHeight();
		ImageCube cube = new ImageCube(width, height, slices);
		int sliceSize = cube.getSliceSize();
		byte[] data = cube.getData();
		JFrame f = new JFrame("Slice Debug");
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		JTabbedPane tabs = new JTabbedPane();
		f.getContentPane().add(tabs);
		f.setVisible(true);
		for (int i = 0; i < slices; i++) {
			if (i > 0)
				slice = image.getSlice(i);
			tabs.addTab("Slice " + (i+1), new JLabel(new ImageIcon(slice)));
			// It's almost certainly possible to speed this up in some way, but
			// whatever.
			for (int y = 0; y < 2; y++) {
				for (int x = 0; x < 2; x++) {
					int rgb = slice.getRGB(x, y);
					System.out.print(x + "," + y + ": " + Integer.toHexString(rgb));
					// Convert RGB to gray
					rgb = ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
					rgb /= 3;
					System.out.println(" -> " + Integer.toHexString(rgb));
					// And store it in the matrix
					data[i * sliceSize + y * width + x] = (byte) rgb; 
				}
			}
		}
		return cube;
	}
	public void writeRawBinary(OutputStream out) throws IOException {
		writeRawBinary(out, true);
	}
	public void writeRawBinary(OutputStream out, boolean includeHeader) throws IOException {
		DataOutputStream data = new DataOutputStream(out);
		if (includeHeader) {
			data.writeInt(width);
			data.writeInt(height);
			data.writeInt(depth);
		}
		data.write(this.data);
	}

	/**
	 * Read in a raw image cube written in a binary format. Pixel values are
	 * assumed to be stored from left-to-right, top-down, first slice-last slice.
	 * The width/height/depth are saved as 32-bit big endian ints.
	 * @param in
	 * @return
	 */
	public static ImageCube fromRawBinary(InputStream in) throws IOException {
		DataInputStream data = new DataInputStream(in);
		int width = data.readInt();
		int height = data.readInt();
		int depth = data.readInt();
		return fromRawBinaryImpl(data, width, height, depth);
	}

	/**
	 * Read in a raw image cube written in a binary format. Pixel values are
	 * assumed to be stored in the order 
	 * @param in
	 * @param width
	 * @param height
	 * @param depth
	 * @return
	 */
	public static ImageCube fromRawBinary(InputStream in, int width, int height, int depth) throws IOException {
		return fromRawBinaryImpl(new DataInputStream(in), width, height, depth);
	}

	/**
	 * Read in a raw image cube written in a binary format. Pixel values are
	 * assumed to be stored in the order 
	 * @param in
	 * @param width
	 * @param height
	 * @param depth
	 * @return
	 */
	private static ImageCube fromRawBinaryImpl(DataInputStream in, int width, int height, int depth) throws IOException {
		ImageCube result = new ImageCube(width, height, depth);
		byte[] data = result.getData();
		in.readFully(data);
		return result;
	}
}
