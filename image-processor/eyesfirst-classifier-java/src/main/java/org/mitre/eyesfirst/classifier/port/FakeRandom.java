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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a known set of "random" numbers. Used for the Java port to compare
 * with the MATLAB version.
 * @author dpotter
 */
public class FakeRandom {
	private final double[] values;
	private int index;

	public FakeRandom(InputStream in) throws IOException {
		DataInputStream dataIn = new DataInputStream(in);
		List<Double> data = new ArrayList<Double>(100);
		while (true) {
			try {
				data.add(dataIn.readDouble());
			} catch (EOFException e) {
				// Ignore this (although technically we could be in the middle
				// of a double)
				break;
			}
		}
		if (data.isEmpty())
			throw new IOException("Input stream was empty.");
		values = new double[data.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = data.get(i);
		}
	}

	public double random() {
		double rv = values[index];
		index++;
		if (index >= values.length)
			index = 0;
		return rv;
	}

	public int getTotalAvailableValues() {
		return values.length;
	}

	/**
	 * Restart back at the start of the list of random numbers.
	 */
	public void reset() {
		index = 0;
	}

	/**
	 * Quick test program. Load a given random file and dump its contents.
	 * @param args
	 */
	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			try {
				FakeRandom r = new FakeRandom(new FileInputStream(args[i]));
				int total = r.getTotalAvailableValues();
				int line = 0;
				for (int j = 0; j < total; j++) {
					if (j > 0) {
						System.out.print(", ");
						if (line > 10) {
							System.out.println();
							line = 0;
						}
					}
					System.out.print(r.random());
					line++;
				}
				System.out.println();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
