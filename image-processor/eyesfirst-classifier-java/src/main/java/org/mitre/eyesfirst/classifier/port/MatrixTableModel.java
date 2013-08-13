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

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

public class MatrixTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 968699679358364019L;
	private final double[] data;
	private final int width;
	private final int height;

	public MatrixTableModel(double[] data, int width, int height) {
		if (data == null)
			throw new NullPointerException();
		if (width < 0 || height < 0)
			throw new IllegalArgumentException("width/height must be positive");
		if (width * height > data.length)
			throw new IllegalArgumentException("Data array is too small to contain " + width + "x" + height + " values");
		this.data = data;
		this.width = width;
		this.height = height;
	}

	@Override
	public int getColumnCount() {
		return width;
	}

	@Override
	public int getRowCount() {
		return height;
	}

	@Override
	public Object getValueAt(int row, int column) {
		if (row < 0 || column < 0 || row >= height || column >= width)
			return null;
		return data[row * width + column];
	}

	public TableModel createRowTableModel() {
		return new AbstractTableModel() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 6374836731346118510L;

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				return rowIndex;
			}
			
			@Override
			public int getRowCount() {
				return height;
			}
			
			@Override
			public int getColumnCount() {
				return 1;
			}
		};
	}

	@Override
	public String getColumnName(int column) {
		return Integer.toString(column);
	}
}
