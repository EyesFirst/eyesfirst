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

import org.apache.commons.math3.FieldElement;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexField;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.FieldMatrixChangingVisitor;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * For lack of a better name (as java.lang.Math already exists), math utility
 * functions.
 * @author dpotter
 *
 */
public class MathUtil {

	private MathUtil() {
	}

	public static int findMaxIndex(double[] array) {
		double max = array[0];
		int best = 0;
		for (int i = 1; i < array.length; i++) {
			double v = array[i];
			if (v > max) {
				max = v;
				best = i;
			}
		}
		return best;
	}

	/**
	 * Given an array that is a double array of complex numbers (even indices
	 * are real, odd are imaginary, such that 1+2i would be { 1.0, 2.0 }),
	 * generates the complex conjugate.
	 * @param data the data to transform
	 */
	public static void complexConjugate(double[] data) {
		if ((data.length & 0x1) != 0)
			throw new IllegalArgumentException("Array length must be even (was " + data.length + ")");
		for (int i = 1; i < data.length; i+=2) {
			data[i] = -data[i];
		}
	}

	public static void complexConjugateInPlace(FieldMatrix<Complex> matrix) {
		matrix.walkInOptimizedOrder(new FieldMatrixChangingVisitor<Complex>() {
			@Override
			public Complex visit(int row, int column, Complex value) {
				return value.conjugate();
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
	}

	/**
	 * Multiplies two arrays together, returning the result in a new array.
	 * If one array is shorter than the other, only the results through the
	 * shorter array are multiplied and the resulting array is the length of the
	 * shorter array.
	 * 
	 * @param array1
	 *            the first array
	 * @param array2
	 *            the second array
	 * @return the first array with each index multiplied by the second array
	 */
	public static double[] multiplyArrays(double[] array1, double[] array2) {
		return multiplyArrays(array1, 0, array2, 0, Math.min(array1.length, array2.length));
	}

	public static double[] multiplyArrays(double[] array1, int start1, double[] array2, int start2, int length) {
		return multiplyArrays(array1, start1, array2, start2, length, null, 0);
	}

	/**
	 * Multiplies two arrays together, storing the result in {@code result}.
	 * Note that as long as {@code resultStart} is less than or equal to the
	 * start in the corresponding array, you may store the results in one of the
	 * input arrays.
	 * 
	 * @param array1
	 *            the first array
	 * @param start1
	 *            the first index to start with in the first input array
	 * @param array2
	 *            the second array
	 * @param start2
	 *            the first index to start with in the second input array
	 * @param length
	 *            the number of entries to multiple together in both arrays
	 * @param result
	 *            the array to store the results in, may be {@code null} to
	 *            generate an array
	 * @param resultStart
	 *            the index to start with in the results array
	 * @return the results array, or a newly created array (such that the first
	 *         answer is at {@code resultStart}) if {@code result} was
	 *         {@code null}
	 * @throws NullPointerException
	 *             if either {@code array1} or {@code array2} are {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if any of the start or length in any of the arrays would be
	 *             out of bounds
	 */
	public static double[] multiplyArrays(double[] array1, int start1, double[] array2, int start2, int length, double[] result, int resultStart) {
		if (array1 == null || array2 == null)
			throw new NullPointerException();
		if (start1 >= array1.length) {
			throw new IndexOutOfBoundsException("start in m1 " + start1 + " is greater than m1 length " + array1.length);
		}
		if (start2 >= array2.length) {
			throw new IndexOutOfBoundsException("start in m2 " + start2 + " is greater than m2 length " + array2.length);
		}
		if (result == null) {
			result = new double[length + resultStart];
		} else {
			if (resultStart >= result.length) {
				throw new IndexOutOfBoundsException("start in result " + resultStart + " is greater than result length " + result.length);
			}
			if (resultStart + length > result.length) {
				throw new IndexOutOfBoundsException("length runs past end of result (would be " + (resultStart + length) + ", result is " + result.length + " long)");
			}
		}
		if (start1 + length > array1.length) {
			throw new IndexOutOfBoundsException("length runs past end of m1 (would be " + (start1 + length) + ", m1 is " + array1.length + " long)");
		}
		if (start2 + length > array2.length) {
			throw new IndexOutOfBoundsException("length runs past end of m2 (would be " + (start2 + length) + ", m2 is " + array2.length + " long)");
		}
		for (int i = 0; i < length; i++) {
			result[i + resultStart] = array1[i + start1] * array2[i + start2];
		}
		return result;
	}

	public static double[] multiplyComplexArrays(double[] array1, double[] array2) {
		return multiplyComplexArrays(array1, 0, array2, 0, Math.min(array1.length, array2.length));
	}

	public static double[] multiplyComplexArrays(double[] array1, int start1, double[] array2, int start2, int length) {
		return multiplyComplexArrays(array1, start1, array2, start2, length, null, 0);
	}

	public static void arrayMultiplyMatricesInPlace(FieldMatrix<Complex> matrix1, final FieldMatrix<Complex> matrix2) {
		matrix1.walkInOptimizedOrder(new FieldMatrixChangingVisitor<Complex>() {
			@Override
			public void start(int rows, int columns, int startRow, int endRow,
					int startColumn, int endColumn) {
				// Don't care
			}

			@Override
			public Complex visit(int row, int column, Complex value) {
				return value.multiply(matrix2.getEntry(row, column));
			}

			@Override
			public Complex end() {
				// Don't care
				return null;
			}
		});
	}

	/**
	 * Multiplies two arrays of complex pairs together, storing the result in
	 * {@code result}. Complex numbers are stored as pairs of real, imaginary so
	 * that the sequence of { 1 + 2<var>i</var>, 3 - 4<var>i</var> } would be
	 * stored in an array as <code>{ 1.0, 2.0, 3.0, -4.0 }</code>. Note that as
	 * long as {@code resultStart} is less than or equal to the start in the
	 * corresponding array, you may store the results in one of the input
	 * arrays.
	 * 
	 * @param array1
	 *            the first array
	 * @param start1
	 *            the first index to start with in the first input array
	 * @param array2
	 *            the second array
	 * @param start2
	 *            the first index to start with in the second input array
	 * @param length
	 *            the number of entries to multiple together in both arrays -
	 *            this is the number of actual indices, <em>not</em> complex
	 *            numbers, so it <strong>must be even</strong> and an exception
	 *            will be thrown if it is not
	 * @param result
	 *            the array to store the results in, may be {@code null} to
	 *            generate an array
	 * @param resultStart
	 *            the index to start with in the results array
	 * @return the results array, or a newly created array (such that the first
	 *         answer is at {@code resultStart}) if {@code result} was
	 *         {@code null}
	 * @throws NullPointerException
	 *             if either {@code array1} or {@code array2} are {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if any of the start or length in any of the arrays would be
	 *             out of bounds
	 * @throws IllegalArgumentException
	 *             if {@code length} is not even
	 */
	public static double[] multiplyComplexArrays(double[] array1, int start1, double[] array2, int start2, int length, double[] result, int resultStart) {
		if (array1 == null || array2 == null)
			throw new NullPointerException();
		if ((length & 1) != 0) {
			throw new IllegalArgumentException("length " + length + " is not event");
		}
		if (start1 >= array1.length) {
			throw new IndexOutOfBoundsException("start in m1 " + start1 + " is greater than m1 length " + array1.length);
		}
		if (start2 >= array2.length) {
			throw new IndexOutOfBoundsException("start in m2 " + start2 + " is greater than m2 length " + array2.length);
		}
		if (result == null) {
			result = new double[length + resultStart];
		} else {
			if (resultStart >= result.length) {
				throw new IndexOutOfBoundsException("start in result " + resultStart + " is greater than result length " + result.length);
			}
			if (resultStart + length > result.length) {
				throw new IndexOutOfBoundsException("length runs past end of result (would be " + (resultStart + length) + ", result is " + result.length + " long)");
			}
		}
		if (start1 + length > array1.length) {
			throw new IndexOutOfBoundsException("length runs past end of m1 (would be " + (start1 + length) + ", m1 is " + array1.length + " long)");
		}
		if (start2 + length > array2.length) {
			throw new IndexOutOfBoundsException("length runs past end of m2 (would be " + (start2 + length) + ", m2 is " + array2.length + " long)");
		}
		for (int i = 0; i < length; i += 2) {
			// a+bi * c+di = (ac-bd) * (ad+bc)i
			double a = array1[start1 + i];
			double b = array1[start1 + i + 1];
			double c = array2[start2 + i];
			double d = array2[start2 + i + 1];
			result[resultStart + i] = a * c - b * d;
			result[resultStart + i + 1] = a * d + b * c;
		}
		return result;
	}

	public static FieldMatrix<Complex> fastFourierTransform2D(RealMatrix input, DftNormalization normalization, TransformType type) {
		// 1. Convert to a complex matrix.
		int rows = input.getRowDimension();
		int cols = input.getColumnDimension();
		FieldMatrix<Complex> complexMatrix = MatrixUtils.createFieldMatrix(ComplexField.getInstance(), rows, cols);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				complexMatrix.setEntry(r, c, Complex.valueOf(input.getEntry(r, c)));
			}
		}
		fastFourierTransform2DInPlace(complexMatrix, normalization, type);
		return complexMatrix;
	}

	public static FieldMatrix<Complex> fastFourierTransform2D(FieldMatrix<Complex> input, DftNormalization normalization, TransformType type) {
		FieldMatrix<Complex> result = input.copy();
		fastFourierTransform2DInPlace(result, normalization, type);
		return result;
	}

	public static void fastFourierTransform2DInPlace(FieldMatrix<Complex> input, DftNormalization normalization, TransformType type) {
		FastFourierTransformer fft = new FastFourierTransformer(normalization);
		int rows = input.getRowDimension();
		int cols = input.getColumnDimension();
		// First, transform each column.
		for (int col = 0; col < cols; col++) {
			Complex[] column = input.getColumn(col);
			column = fft.transform(column, type);
			input.setColumn(col, column);
		}
		// Second, transform each row.
		for (int row = 0; row < rows; row++) {
			Complex[] rowData = input.getRow(row);
			rowData = fft.transform(rowData, type);
			input.setRow(row, rowData);
		}
	}

	/**
	 * Swaps quadrants in the same way MATLAB's {@code fftshift} function does.
	 * What this does is basically swap each quadrant, so the upper-left
	 * quadrant swaps with the lower-right quadrant and the upper-right quadrant
	 * swaps with the lower-left quadrant.
	 * @param input
	 */
	public static <T extends FieldElement<T>> void fftShiftInPlace(FieldMatrix<T> input) {
		// If the number of columns/rows is even, we can do this in-place
		// easily.
		int rows = input.getRowDimension();
		int columns = input.getColumnDimension();
		if ((rows & 1) == 0 && (columns & 1) == 0) {
			int columnShift = columns / 2;
			int rowShift = rows / 2;
			for (int row = 0; row < rowShift; row++) {
				for (int col = 0; col < columnShift; col++) {
					// Grab out the two values...
					T entry1 = input.getEntry(row, col);
					T entry2 = input.getEntry(row + rowShift, col + columnShift);
					// ...and swap them.
					input.setEntry(row, col, entry2);
					input.setEntry(row + rowShift, col + columnShift, entry1);
				}
			}
		} else {
			// If it's odd, the shift becomes weird:
			/*
			 * >> fftshift([ 1,2,3; 4,5,6; 7,8,9 ])
			 *
			 * ans =
			 * 
			 *      9     7     8
			 *      3     1     2
			 *      6     4     5
			 */
			// For now, just bomb:
			throw new IllegalArgumentException("Dimensions must be even");
			// FIXME: Implement this
		}
	}

	/**
	 * Rounds in the same way MATLAB does. This is almost identical to
	 * {@link Math#round(double)}, except that it rounds negative 0.5 remainders
	 * down instead of up. (So -0.5 would be -1, and -1.5 would be -2.)
	 * @param v
	 * @return
	 */
	public static long round(double v) {
		// Use Math.round to tackle the special cases of +/- Infinity and NaN
		long result = Math.round(v);
		// The only special case kicks in if the value is negative and has a
		// 0.5 part.
		if (v < 0.0 && (result - v) == 0.5) {
			return result - 1;
		}
		return result;
	}

	/**
	 * Vectorizes in the way Matlab does, by row rather than column.
	 * @param source
	 * @param width
	 * @param height
	 * @return
	 */
	public static double[] vectorize(double[] source, int width, int height) {
		return vectorize(source, width, height, width, height);
	}

	/**
	 * 
	 * @param source
	 * @param width
	 * @param height
	 * @param destWidth
	 * @param destHeight
	 * @return
	 */
	public static double[] vectorize(double[] source, int width, int height, int destWidth, int destHeight) {
		double[] result = new double[destWidth * destHeight];
		int index = 0;
		int copyWidth = Math.min(destWidth, width);
		int copyHeight = Math.min(destHeight, height);
		for (int col = 0; col < copyWidth; col++) {
			for (int row = 0; row < copyHeight; row++) {
				result[index] = source[row * width + col];
				index++;
			}
		}
		return result;
	}

	/**
	 * Swaps width/height.
	 * @param source
	 * @param width
	 * @param height
	 * @return
	 */
	public static double[] shiftDimensions(double[] source, int width, int height) {
		if (source.length < width * height)
			throw new IllegalArgumentException("source array does not fill given width/height");
		double[] result = new double[source.length];
		int index = 0;
		System.out.println("Size=" + source.length + " " + width + "x" + height);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				result[index] = source[y * width + x];
				index++;
			}
		}
		return result;
	}
}
