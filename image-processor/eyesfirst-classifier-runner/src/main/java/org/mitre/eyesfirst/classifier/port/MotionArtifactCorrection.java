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

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.FieldMatrixPreservingVisitor;
import org.apache.commons.math3.optimization.SimpleVectorValueChecker;
import org.apache.commons.math3.optimization.fitting.PolynomialFitter;
import org.apache.commons.math3.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.TransformType;

public class MotionArtifactCorrection {
	/*
	 * 
smoothingPar.xdeg = 2;
smoothingPar.zdeg = 2;
	 */

	public MotionArtifactCorrection() {
		// TODO Auto-generated constructor stub
	}

	/*
	 * This uses JTransform, which isn't available in the Maven repository,
	 * but may ultimately prove to be faster. For now, just ignore it.
	public void sliceShift(ImageCube cube, int xdeg, int zdeg) {
		// Note: I store the cube as "slice/height/width" which makes the most
		// sense to me, but in MATLAB the cube was a matrix stored as
		// [height][width][depth].
		if ((cube.getWidth() & 0x01) != 0 || (cube.getHeight() & 0x01) != 0)
			throw new IllegalArgumentException("cube dimensions must be even");
		// aa = height
		// bb = width
		// cc = slices
		double halfWidth = (cube.getWidth()/2)+1;
		double halfHeight = (cube.getHeight()/2)+1;
		int slices = cube.getDepth() - 1;
		slices = 1;// FIXME: DEBUG ONLY
		DoubleFFT_2D fft = new DoubleFFT_2D(cube.getHeight(), cube.getWidth());
		for (int i_y = 0; i_y < slices; i_y++) {
			double[] slice1 = cube.getComplexSlice(i_y);
			System.out.print("Slice 1: ");
			dumpComplexMatrix(slice1, cube.getWidth(), cube.getHeight());
			double[] slice2 = cube.getComplexSlice(i_y+1);
			System.out.print("Slice 2: ");
			dumpComplexMatrix(slice2, cube.getWidth(), cube.getHeight());
			fft.complexForward(slice2);
			System.out.print("FFT: ");
			dumpComplexMatrix(slice2, cube.getWidth(), cube.getHeight());
			MathUtil.complexConjugate(slice2);
			System.out.print("Complex conjugate: ");
			dumpComplexMatrix(slice2, cube.getWidth(), cube.getHeight());
			fft.complexForward(slice1);
			System.out.print("FFT 2: ");
			dumpComplexMatrix(slice1, cube.getWidth(), cube.getHeight());
			// Multiple the two complex matrices together, but throw them into
			// slice1 for now
			MathUtil.multiplyComplexArrays(slice1, 0, slice2, 0, slice1.length, slice1, 0);
			System.out.print(".*: ");
			dumpComplexMatrix(slice1, cube.getWidth(), cube.getHeight());
			fft.complexInverse(slice1, true);
			System.out.print("IFFT: ");
			dumpComplexMatrix(slice1, cube.getWidth(), cube.getHeight());
			//s1=double(squeeze(A(:,:,i_y)));
			//s2=double(squeeze(A(:,:,i_y+1)));
			/*
    t1 = fft2(s2);
    t2 = conj(t1);
    t3 = fft2(s1);
    t4 = t3.*t2;
    c12=ifft2(t4);
    c12=fftshift(c12);
			c12=ifft2(fft2(s1).*conj(fft2(s2)));
			c12=fftshift(c12);
			[value,index]=max(c12(:));
			[z_shift,x_shift] = ind2sub(size(c12),index);
			z_array(i_y) = z_shift-haap1;
			x_array(i_y) = x_shift-hbbp1;* /
		}
		//return shiftAlignImage(A,z_array,x_array,zdeg,xdeg);
	}
*/
	public ImageCube sliceShift(ImageCube cube, int xdeg, int zdeg) {
		// Note: I store the cube as "slice/height/width" which makes the most
		// sense to me, but in MATLAB the cube was a matrix stored as
		// [height][width][depth].
		if ((cube.getWidth() & 0x01) != 0 || (cube.getHeight() & 0x01) != 0)
			throw new IllegalArgumentException("cube dimensions must be even");
		// aa = height
		// bb = width
		// cc = slices
		int width = cube.getWidth();
		int height = cube.getHeight();
		int halfWidth = cube.getWidth()/2;
		int halfHeight = cube.getHeight()/2;
		int slices = cube.getDepth() - 1;
		int[] zArray = new int[slices];
		int[] xArray = new int[slices];
		for (int i_y = 0; i_y < slices; i_y++) {
			FieldMatrix<Complex> slice1 = cube.getSliceAsComplexMatrix(i_y);
			FieldMatrix<Complex> slice2 = cube.getSliceAsComplexMatrix(i_y+1);
			MathUtil.fastFourierTransform2DInPlace(slice2, DftNormalization.STANDARD, TransformType.FORWARD);
			MathUtil.complexConjugateInPlace(slice2);
			MathUtil.fastFourierTransform2DInPlace(slice1, DftNormalization.STANDARD, TransformType.FORWARD);
			// Multiply the two complex matrices together, but throw them into
			// slice1 for now
			MathUtil.arrayMultiplyMatricesInPlace(slice1, slice2);
			MathUtil.fastFourierTransform2DInPlace(slice1, DftNormalization.STANDARD, TransformType.INVERSE);
			// Find the max value in this
			Complex max = slice1.walkInOptimizedOrder(new FieldMatrixPreservingVisitor<Complex>() {
				private int maxRow = 0;
				private int maxCol = 0;
				private double max = Double.MIN_VALUE;

				@Override
				public void start(int rows, int columns, int startRow, int endRow,
						int startColumn, int endColumn) {
					// Don't care
				}

				@Override
				public void visit(int row, int column, Complex value) {
					// We only care about the real part
					double r = value.getReal();
					if (r > max) {
						max = r;
						maxRow = row;
						maxCol = column;
					}
				}

				@Override
				public Complex end() {
					// Abuse the fact that complex is a pair to return the result
					return Complex.valueOf(maxRow, maxCol);
				}
			});
			zArray[i_y] = (int) max.getReal();
			if (zArray[i_y] > halfHeight)
				zArray[i_y] = zArray[i_y] - height;
			xArray[i_y] = (int) max.getImaginary();
			if (xArray[i_y] > halfWidth)
				xArray[i_y] = xArray[i_y] - width;
			//System.out.println("Calculated shift as " + zArray[i_y] + ", " + xArray[i_y]);
		}
		return shiftAlignImage(cube, zArray, xArray, zdeg, xdeg);
	}
/* Unused debug functions
	private static void dumpMatrix(double[] data, int width, int height) {
		// For now, just dump the first eight entries
		Formatter f = new Formatter(System.out);
		for (int i = 0; i < 8; i++) {
			if (i > 0)
				f.format(",  ");
			f.format("%.05e", data[i]);
		}
		f.format("%n");
		f.flush();
	}

	private static void dumpComplexMatrix(double[] data, int width, int height) {
		// For now, just dump the first eight entries
		Formatter f = new Formatter(System.out);
		for (int i = 0; i < 8; i+=2) {
			if (i > 0)
				f.format(", ");
			f.format("%.5e %s %.5ei", data[i], Math.signum(data[i+1]) < 0.0 ? "-" : "+", Math.abs(data[i+1]));
		}
		f.format("%n");
		f.flush();
	}

	private static void dumpComplexMatrix(FieldMatrix<Complex> matrix, int width, int height) {
		// For now, just dump the first eight entries
		Formatter f = new Formatter(System.out);
		for (int i = 0; i < 4; i++) {
			if (i > 0)
				f.format(", ");
			Complex c = matrix.getEntry(0, i);
			double imag = c.getImaginary();
			f.format("%.5e %s %.5ei", c.getReal(), Math.signum(imag) < 0.0 ? "-" : "+", Math.abs(imag));
		}
		f.format("%n");
		f.flush();
	}
*/
	/**
	 * 
	 * @param cube
	 * @param zArray
	 * @param xArray
	 * @param zDeg degree of the Z polynomial
	 * @param xDeg degree of the X polynomial
	 * @return
	 */
	private ImageCube shiftAlignImage(ImageCube cube, int[] zArray, int[] xArray, int zDeg, int xDeg) {
		// Note: Several APIs in the Commons Math library have been deprecated
		// in 3.1 and replaced with APIs that don't exist in 3.0.
		// Unfortunately Commons Math 3.1 is still in development, so the 3.0
		// APIs are in use instead and comments are being used to mark changes
		// that will be needed when 3.1 is released.
		// Note: the following is deprecated in Commons Math 3.1, to be replaced
		// with new PolynomialFitter(new GaussNewtonOptimizer(new SimpleVectorValueChecker()));
		PolynomialFitter fitter = new PolynomialFitter(zDeg, new GaussNewtonOptimizer(new SimpleVectorValueChecker()));
		// assuming I understand this:
		for (int i = 0; i < zArray.length; i++) {
			fitter.addObservedPoint(i + 1, zArray[i]);
		}
		// Note: the following is deprecated in Commons Math 3.1 to be replaced
		// with fitter.fit(new double[zDeg+1])
		double[] polyZ = fitter.fit();
		// Note: In 3.1, the following can be replaced with just
		// fitter.clearObservations()
		// as the degree will be able to change without creating a new instance
		if (xDeg != zDeg) {
			fitter = new PolynomialFitter(xDeg, new GaussNewtonOptimizer(new SimpleVectorValueChecker()));
		} else {
			fitter.clearObservations();
		}
		for (int i = 0; i < zArray.length; i++) {
			fitter.addObservedPoint(i + 1, xArray[i]);
		}
		double[] polyX = fitter.fit();
		PolynomialFunction func = new PolynomialFunction(polyZ);
		int slices = cube.getDepth() - 1;
		int[] zCorrect = new int[slices];
		for (int i = 0; i < slices; i++) {
			zCorrect[i] = (int) MathUtil.round(zArray[i] - func.value(i+1));
		}
		func = new PolynomialFunction(polyX);
		int[] xCorrect = new int[slices];
		for (int i = 0; i < slices; i++) {
			xCorrect[i] = (int) MathUtil.round(xArray[i] - func.value(i+1));
		}
		// And with that done, create the final motion corrected cube.
		// First calculate the extents.
		int cumZ = zCorrect[0], cumX = xCorrect[0];
		// Left/top are the minimum X/Z
		int left = cumX, top = cumZ;
		// Right/bottom are the maximum X/Z
		int right = cumX, bottom = cumZ;
		// Note that we're skipping 0 because we already did it (effectively)
		// with the variable initialization.
		for (int i = 1; i < slices; i++) {
			cumZ += zCorrect[i];
			cumX += xCorrect[i];
			// Top is the min Z value
			if (top > cumZ) {
				top = cumZ;
			}
			// Bottom is the max Z value
			if (bottom < cumZ) {
				bottom = cumZ;
			}
			// Left is the min X value
			if (left > cumX) {
				left = cumX;
			}
			// Right is the max X value
			if (right < cumX) {
				right = cumX;
			}
		}
		//System.out.println("Cube: " + top + ", " + right + ", " + bottom + ", " + left);
		// Generate the new cube
		ImageCube result = new ImageCube(cube.getWidth() + right - left, cube.getHeight() + bottom - top, cube.getDepth());
		// I'm honestly somewhat confused about this, but:
		result.copySlice(cube, 0, Math.abs(left), Math.abs(top));
		// Note that slices is currently really depth-1, which is why this
		// uses <=.
		int xShift = 0, yShift = 0;
		for (int i = 1; i <= slices; i++) {
			xShift += xCorrect[i-1];
			yShift += zCorrect[i-1];
			result.copySlice(cube, i, xShift - left, yShift - top);
		}
		return result;
	}

	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			try {
				long start = System.currentTimeMillis();
				ImageCube cube = ImageCube.fromRawBinary(new FileInputStream(args[i]));
				System.out.println("Loaded cube in " + (System.currentTimeMillis() - start) + "ms");
				start = System.currentTimeMillis();
				ImageCube finalCube = new MotionArtifactCorrection().sliceShift(cube, 2, 2);
				System.out.println("Ran artifact correction in " + (System.currentTimeMillis() - start) + "ms");
				FileOutputStream out = new FileOutputStream(args[i] + ".out");
				finalCube.writeRawBinary(out);
				out.close();
			} catch (Exception e) {
				System.err.println("--- Failed on " + args[i] + " ---");
				e.printStackTrace();
			}
		}
	}
}
