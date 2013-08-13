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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.FastMath;

/**
 * Represents the semclassify MATLAB function group. Note: This class is not
 * thread safe.
 * @author dpotter
 */
public class SemClassify {
	// FIXME: For now, use a canned set of "random" values
	private static FakeRandom random;
	static {
		try {
			InputStream in = SemClassify.class.getResourceAsStream("random.dat");
			if (in == null) {
				System.err.println("Note: unable to find canned random data, using PRNG as normal instead.");
			} else {
				random = new FakeRandom(in);
			}
		} catch (IOException e) {
			System.err.println("Error loading fake random values!");
			e.printStackTrace();
		}
	}
	public static class Stat {
		public double mean;
		public double covm;
		public Stat(double mean, double covm) {
			this.mean = mean;
			this.covm = covm;
		}
		/**
		 * Copy constructor.
		 * @param other
		 */
		public Stat(Stat other) {
			this.mean = other.mean;
			this.covm = other.covm;
		}
		@Override
		public String toString() {
			return "Stat [mean=" + mean + ", covm=" + covm + "]";
		}
	}

	public SemClassify() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 * @param dm data matrix
	 * @param dmwt data matrix with targets
	 * @param iclstat initial class statistics
	 * @param iclpd initial class probability distribution
	 * @param maxnumit maximum number of iterations used by the sem update equations
	 */
	public void semClassify(byte[] dm, byte[] dmwt, Stat[] iclstat, double[] iclpd, int maxnumit) {
		semEstimate(dm, iclstat, iclpd, maxnumit);
		purepixmclf(dmwt, semcl, sempd);
		double[] maclmap = new double[dm.length];
		maclIndMap = new byte[maclmap.length];
		for (int i = 0; i < maclmap.length; i++) {
			double max = cclf[0][i];
			for (int j = 1; j < cclf.length; j++) {
				double v = cclf[j][i];
				if (v > max) {
					max = v;
					maclIndMap[i] = (byte) j;
				}
			}
			maclmap[i] = max;
		}
		System.out.println("maclmap = " + Arrays.toString(Arrays.copyOfRange(maclmap, 0, 10)));
		System.out.println("maclIndMap = " + Arrays.toString(Arrays.copyOfRange(maclIndMap, 0, 10)));
	}
	/**
	 * Result computed by {@link #semClassify(byte[], byte[], Stat[], double[], int)}.
	 */
	private byte[] maclIndMap;

	public double[] getClpd() {
		return sempd;
	}

	public Stat[] getClstat() {
		return semcl;
	}

	public byte[] getMaclIndMap() {
		return maclIndMap;
	}
/*
 * function [clstat,clpd,cclmap,ppmclf,maclmap,stclest,maclIndMap]=semclassify(dm,dmwt,iclstat,iclpd,maxnumit);
% function [clstat,clmap,ppcclf]=semclassify(dm,dmwt,iclstat,maxnumit);
% input  dm: data matrix
%        dmwt: data matrix with targets
%        iclstat:  initial class statistics
%        iclpd:    initial class probability distribution
%        maxnumit;  maximum number of iterations used by the sem update equations
% output  clstat:  sem estimate of class statistics
%         clmap:   sem class map
%         ppcclf:  pure pixel class conditional likelihood function
%         maclmap:  maximum (aposteriori ?)  class map
[semstat,sempd,stclest]=semestimate(dm,iclstat,iclpd,maxnumit);
[ppmclf,cclmap]=purepixmclf(dmwt,semstat,sempd);
[maclmap,maclIndMap]=max(cclmap,[],2);
clstat=semstat;
clpd=sempd;
% semclmap=clconddist(simdmat,semstat,sempd);
% gtclmap=clconddist(simdmat,clstat,clpd);

 */

	/**
	 * Return values from {@link #semEstimate(byte[], Stat[], double[], int)}.
	 * Used by {@link #semClassify(byte[], byte[], Stat[], double[], int)}.
	 */
	private Stat[] semcl;
	/**
	 * Return values from {@link #semEstimate(byte[], Stat[], double[], int)}.
	 * Used by {@link #semClassify(byte[], byte[], Stat[], double[], int)}.
	 */
	private double[] sempd;
	/**
	 * Return values from {@link #semEstimate(byte[], Stat[], double[], int)}.
	 * Used by {@link #semClassify(byte[], byte[], Stat[], double[], int)}.
	 */
	private double[][] stcl;

	/**
	 * applies the SEM algorithm to the data set dm (nbandsXnobs) fitting a
	 * Gaussian mixture model to the data.
	 * 
	 * @param dm
	 * @param iclstat
	 *            the structure of initial classes
	 * @param initcpd
	 *            the vector(numclX1) of the initial class probability
	 *            distribution
	 * @param maxnumit
	 */
	private void semEstimate(byte[] dm, Stat[] iclstat, double[] initcpd, int maxnumit) {
		Stat[] curcl = iclstat;
		double[] curclpd = initcpd;
		int numcl = initcpd.length;
		// The MATLAB code only bothered initializing stcl if numcl was 1. Not
		// sure why, doesn't seem to make a difference but does make MATLAB
		// slower. Whatever.
		stcl = new double[maxnumit][];
		for (int i = 0; i < maxnumit; i++) {
			double[][] cclp = clconddist(dm, curcl, curclpd);
			// Note: the indices into cclp are "backwards" in Java, to reduce
			// the number of array objects required
			int[] ccla = classign(cclp);
			semUpdate(ccla, dm, numcl);
			// MATLAB returned the values as pairs. Instead we return them as
			// internal private class members. Not the best design, really, but
			// better than creating a new inner private class for a pair of
			// values or needlessly returning an object array.
			int nupclass = upcl.size();
			if (nupclass < numcl) {
				// Pad out the remaining values with the last class
				Stat base = upcl.get(nupclass - 1);
				for (int j = nupclass; j < numcl; j++) {
					upcl.add(new Stat(base));
				}
				// uppd will already be padded with 0s, so just skip that
			}
			curcl = upcl.toArray(new Stat[upcl.size()]);
			curclpd = uppd;
			/*
			System.out.println("Iteration " + (i+1) + ":");
			System.out.println("  curcl = " + Arrays.toString(curcl));
			System.out.println("  curclpd = " + Arrays.toString(curclpd));
			*/
			stcl[i] = new double[4];
			// This seems to require the number of classes to always be at least
			// 2.
			stcl[i][0] = curcl[0].mean;
			stcl[i][1] = curcl[0].covm;
			stcl[i][2] = curcl[1].mean;
			stcl[i][3] = curcl[1].covm;
		}
		semcl = curcl;
		sempd = curclpd;
		/*
		System.out.println("semclassify final values:");
		System.out.println("  semcl:");
		for (int i = 0; i < curcl.length; i++) {
			System.out.println("    [" + (i + 1) + "] = " + curcl[i]);
		}
		System.out.println("  sempd: " + Arrays.toString(curclpd));
		*/
	}

	/**
	 * Stats as calculated by {@link #semUpdate(int[], byte[], int)}. Used by
	 * {@link #semEstimate(byte[], Stat[], double[], int)}.
	 */
	private List<Stat> upcl;
	/**
	 * Stats as calculated by {@link #semUpdate(int[], byte[], int)}. Used by
	 * {@link #semEstimate(byte[], Stat[], double[], int)}.
	 */
	private double[] uppd;

	/**
	 * 
	 * @param ccla is the vector of current class assignments for each datum
	 * @param dm is the (nbdsXnobs) matrix of observations
	 * @param numcl is the number of classes
	 */
	private void semUpdate(int[] ccla, byte[] dm, int numcl) {
		// function [upcl,uppd]=semupdate(ccla,dm,numcl);
		int nobs = dm.length;
		int clct = 0;
		int multfac = 10;
		// Return values (returned through class members, as this is a private
		// method)
		upcl = new ArrayList<Stat>(numcl);
		uppd = new double[numcl];
		for (int clazz = 1; clazz <= numcl; clazz++) {
			// We need to count the number of entries in ccla that are this "class"
			int count = 0;
			// For the mean
			long total = 0;
			for (int i = 0; i < nobs; i++) {
				if (ccla[i] == clazz) {
					count++;
					total += ((int)dm[i]) & 0xFF;
				}
			}
			// Now that we have, see what we do with it
			if (count > multfac) {
				double mean = ((double)total) / ((double)count);
				double[] vector = new double[count];
				for (int i = 0, vi = 0; i < nobs; i++) {
					if (ccla[i] == clazz) {
						vector[vi] = (double)(((int)dm[i]) & 0xFF);
						vi++;
					}
				}
				Variance v = new Variance();
				upcl.add(new Stat(mean, v.evaluate(vector, mean)));
				uppd[clct] = ((double)count) / ((double)nobs);
				clct++;
			}
		}
	}

	private double[][] clconddist(byte[] dm, Stat[] curcl, double[] curclpd) {
		int ncl = curclpd.length;
		int nobs = dm.length;
		// NOTE: In the MATLAB code, most of these were nobs x ncl matrices.
		// Because ncl is going to be 2 and nobj is going to be huge (around
		// 500,000), doing that in Java would create a ridiculous number of
		// arrays. Instead, we do it backwards, with ncl arrays of nobs each.
		double[][] condlikemat = new double[ncl][];
		for (int i = 0; i < ncl; i++) {
			condlikemat[i] = clcondpd(dm, curcl[i].mean, curcl[i].covm);
		}
		// The following loops are:
		// prodprobmat=condlikemat.*repmat(curclpd',nobs,1);
		// pobs=sum(prodprobmat,2);
		double[][] cclp = new double[ncl][];
		for (int i = 0; i < ncl; i++) {
			cclp[i] = new double[nobs];
		}
		double[] temp = new double[ncl];
		for (int i = 0; i < nobs; i++) {
			double sum = 0;
			for (int j = 0; j < ncl; j++) {
				temp[j] = condlikemat[j][i] * curclpd[j];
				sum += temp[j];
			}
			if (sum == 0) {
				// FIXME: That MATLAB code does something with NaN that I currently
				// don't bother doing.
				// Also, the MATLAB code only does it ONCE and I'm not sure why.
				throw new RuntimeException("NaN in row " + i + "! I think this row is supposed to become " + 1/ncl + " but I'm not sure.");
			}
			// Once we have the sum, we use it to generate the final value
			for (int j = 0; j < ncl; j++) {
				cclp[j][i] = temp[j] / sum;
			}
		}
		return cclp;
	}

	/**
	 * Computes the probability of the observations dm (nbandsXnobs) for given
	 * normal class defined by mean curclmean and covariance curclcov
	 * @param dm
	 * @param curclmean
	 * @param curclcov
	 */
	private double[] clcondpd(byte[] dm, double curclmean, double curclcov) {
		System.out.println("clcondpd " + curclmean + ", " + curclcov);
		// Because curclmean and curclcov are ALWAYS doubles, we can skip the
		// following useless logic:
		//
		// [U,D,V]=svd(curclcov);
		// eigval=diag(D);
		// condnumv=eigval/eigval(1);
		// I1=find(log10(condnumv)>=-maxcondnum);
		// LI1=length(I1);
		// teigval=eigval;
		// if LI1<nbds
		//     teigval(LI1+1:nbds)=eigval(LI1+1)*ones(nbds-LI1,1);
		// end;
		//
		// Because curclcov is a single value, D = curclcov, diag(D) = curclcov,
		// eigval = curclcov, eigval/eigval(1) = 1
		// I1 will also always be 1, because log10(1) = 0 which is >= -10.
		// Likewise the length of a single value is still 1, and 1 is never less
		// than 1.

		// acovinv=U*diag(invteigval)*U';
		// U is always 1, and a diagonal matrix of a single value is... that
		// value. So:
		double invteigval = FastMath.pow(curclcov, -1);

		// whitemat=diag(sqrt(invteigval))*U';
		// Again, invteigval is a single value, so its sqrt is a single value,
		// and multiplying by 1 is the same value:
		double whitemat = FastMath.sqrt(invteigval);

		// srdetcm=sqrt(prod(teigval));
		// teigval is always curclcov (see above), so prod is just that value.
		double srdetcm = FastMath.sqrt(curclcov);
		// srdetcm is only used in (srdetcm*((2*pi)^(nbds/2))), so do that now.
		srdetcm *= FastMath.pow(2 * Math.PI, 0.5); 

		// The following code is all done in the same loop:
		//
		// wdm=whitemat*(dm-repmat(curclmean,1,nobs));
		// if nbds > 1
		//     qf=sum(wdm.^2);
		// else
		//     qf = wdm.^2;
		// end;
		// pobsgcl=exp(-qf/2)/(srdetcm*((2*pi)^(nbds/2)));

		// Create the result array:
		double[] pobsgcl = new double[dm.length];
		for (int i = 0; i < dm.length; i++) {
			// wdm=whitemat*(dm-repmat(curclmean,1,nobs));
			// OK, curclmean is a single value, so all this does is subtract it
			// from all values in dm and then multiply it by whitemat, which is
			// also a single value.
			double dataValue = (double) (((int)dm[i]) & 0xFF);
			double wdm = whitemat * (dataValue - curclmean);
			// nbds is always 1, so just ignore it.
			double qf = FastMath.pow(wdm, 2);
			// That brings us to:
			// pobsgcl=exp(-qf/2)/(srdetcm*((2*pi)^(nbds/2)));
			// We already did the second part above, so:
			pobsgcl[i] = FastMath.exp(-qf / 2.0) / srdetcm;
		}
		return pobsgcl;
	}

	/**
	 * 
	 * @param cclp the ncl x nobs matrix of class conditional probabilities
	 */
	private int[] classign(double[][] cclp) {
		// The MATLAB code re-reads this, so reset
		if (random != null)
			random.reset();
		// NOTE: cclp is "backwards" compared to MATLAB to reduce the number
		// of arrays required.
		int ncl = cclp.length;
		int nobs = cclp[0].length;
		// ccld=cumsum(cclp,2);
		int[] ccla = new int[nobs];
		// What we're doing is basically finding the first index that's greater
		// than or equal to 0, and storing that. Not clear on why quite yet.
		for (int i = 0; i < nobs; i++) {
			double sum = cclp[0][i];
			double rand;
			if (random == null) {
				rand = FastMath.random();
			} else {
				rand = random.random();
			}
			if (sum - rand >= 0) {
				ccla[i] = 1;
				// No need to keep looking (so continue the main loop)
				continue;
			}
			for (int j = 1; j < ncl; j++) {
				sum += cclp[j][i];
				if (sum - rand >= 0) {
					ccla[i] = j + 1;
					// No need to keep looking (so break this loop and drop back
					// to the main loop)
					break;
				}
			}
		}
		return ccla;
	}

	private double[][] cclf;
	private double[] ppmclf;
	/**
	 * computes the likelihood of each observation in dm based on the Gaussian
	 * mixture model having classes curcl and class probability distribution
	 * curclpd
	 * ppmclf is the pure-pixel multiple class likelihood function
	 * cclf is the class conditional likelihood function
	 * @param dm
	 * @param curcl
	 * @param curclpd
	 */
	private void purepixmclf(byte[] dm, Stat[] curcl, double[] curclpd) {
		int ncl = curclpd.length;
		// This array has the array index backwards as compared to MATLAB. Again,
		// it's to make things easier in Java.
		double[][] cclf = new double[ncl][];
		double[] pobs = new double[dm.length];
		for (int i = 0; i < ncl; i++) {
			double[] pobsgcl = clcondpd(dm, curcl[i].mean, curcl[i].covm);
			cclf[i] = new double[pobsgcl.length];
			for (int j = 0; j < pobsgcl.length; j++) {
				cclf[i][j] = pobsgcl[j] * curclpd[i];
				pobs[j] += cclf[i][j];
			}
		}
		// Now that we've calculated pobs for each class, we can go back and fix
		// cclf (divide each entry by it)
		for (int i = 0; i < cclf.length; i++) {
			double[] a = cclf[i];
			for (int j = 0; j < a.length; j++) {
				a[j] /= pobs[j];
			}
		}
		this.cclf = cclf;
		this.ppmclf = pobs;
	}
}
