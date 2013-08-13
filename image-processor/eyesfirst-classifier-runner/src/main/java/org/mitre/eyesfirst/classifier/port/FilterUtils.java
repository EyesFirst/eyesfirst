/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Note: This was submitted as a feature request to Commons Math but not
 * accepted at this time. It comes from:
 * https://issues.apache.org/jira/browse/MATH-851
 */
package org.mitre.eyesfirst.classifier.port;

import org.apache.commons.math3.exception.NoDataException;

/**
 * Basic filter routines.
 * This class provides currently only a (static) function which calculates the 
 * convolution of two sequences (represented as double[]).
 *
 * @version $Id$
 */
public class FilterUtils {

    private FilterUtils() {
        super();
    }


    /**
     * Calculates the convolution between two sequences. The solution is 
     * obtained via straightforward computation of the convolution sum (and 
     * not via FFT; for longer sequences, the performance of this method might 
     * be inferior to an FFT-based implementation).
     * 
     * @param x the first sequence (double array of length {@code lenX}); the 
     * sequence is assumed to be zero elsewhere (i.e. {x[i]}=0 for i<0 
     * and i>={@code lenX}). Typically, this sequence will represent an input
     * signal to a system.
     * 
     * @param h the second sequence (double array of length {@code lenH}); the 
     * sequence is assumed to be zero elsewhere (i.e. {h[i]}=0 for i<0 
     * and i>={@code lenH}). Typically, this sequence will represent the impulse
     * response of the system.
     * 
     * @return the convolution of {@code x} and {@code h} (double array of 
     * length {@code lenX} + {@code lenH} -1)
     * 
     * @throws NoDataException if {@code x}, or {@code h}, or both are empty
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Convolution">Wikipedia (Convolution)</a>
     * 
     */
    public static double[] convolve(double[] x, double[] h) {

        int lenH = h.length;
        if (lenH == 0) {
            throw new NoDataException();
        }

        int lenX = x.length;
        if (lenX == 0) {
            throw new NoDataException();
        }

        // initialize the array for the output
        double[] y = new double[lenH + lenX - 1];

        // straightforward implementation of the convolution sum
        for (int n = 0; n < lenH + lenX - 1; n++) {

            double yn = 0;

            for (int k = 0; k < lenH; k++) {
                if ((n-k > -1) && (n-k < lenX) ) {
                    yn = yn + x[n-k] * h[k];
                }
            }
            y[n] = yn;
        }

        // return the output
        return y;

    }
}
