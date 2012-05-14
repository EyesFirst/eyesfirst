/*
 * Copyright 2012 The MITRE Corporation
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
package org.mitre.eyesfirst.viewer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.mitre.eyesfirst.viewer.web.TagNameUtil;

public class DicomMetadataTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3547593525433002962L;
	private DicomElement[] dataElements;
	private SpecificCharacterSet charset;

	public DicomMetadataTableModel() {
		
	}

	public void setDicomObject(DicomObject object) {
		if (dataElements != null)
			fireTableRowsDeleted(0, dataElements.length-1);
		List<DicomElement> list = new ArrayList<DicomElement>(object.size());
		Iterator<DicomElement> iter = object.iterator();
		while (iter.hasNext()) {
			list.add(iter.next());
		}
		System.out.println("Found " + list.size() + ", size was " + object.size());
		dataElements = list.toArray(new DicomElement[list.size()]);
		charset = object.getSpecificCharacterSet();
		fireTableRowsInserted(0, dataElements.length - 1);
	}

	@Override
	public int getRowCount() {
		return dataElements == null ? 0 : dataElements.length;
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex < 0 || rowIndex >= dataElements.length)
			throw new IndexOutOfBoundsException(Integer.toString(rowIndex));
		DicomElement element = dataElements[rowIndex];
		if (element == null)
			return "<null>";
		switch (columnIndex) {
		case 0:
			return TagNameUtil.toName(dataElements[rowIndex].tag());
		case 1:
			return dataElements[rowIndex].vr().toString();
		case 2:
			return convertToJavaValue(dataElements[rowIndex], charset);
		}
		return null;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "Tag";
		case 1:
			return "VR";
		case 2:
			return "Data";
		}
		return super.getColumnName(column);
	}

	public static Object convertToJavaValue(DicomElement element, SpecificCharacterSet charset) {
		if (element == null) {
			return null;
		}
		if (element.tag() == Tag.PixelData) {
			return "<imagery>";
		}
		VR vr = element.vr();
		try {
			if (vr == null) {
				return "(no VR for element)";
			} else if (vr.equals(VR.SS) || vr.equals(VR.SL) ||
					vr.equals(VR.US) || vr.equals(VR.UL)){
				// FIXME: UL should be unsigned
				return element.getInt(true);
			} else if (vr.equals(VR.FD) || vr.equals(VR.FL) || vr.equals(VR.DS)) {
				return element.getDouble(true);
			} else if (vr.equals(VR.SH) || vr.equals(VR.ST) ||
					vr.equals(VR.LO) || vr.equals(VR.LT) ||
					vr.equals(VR.OB) || vr.equals(VR.OF) || vr.equals(VR.OW) || // I think this is wrong
					vr.equals(VR.UI) || vr.equals(VR.UT) ||
					vr.equals(VR.AE) || vr.equals(VR.CS) || vr.equals(VR.IS) ||
					vr.equals(VR.PN)) {
				return element.getString(charset, true);
			} else if (vr.equals(VR.DA) || vr.equals(VR.DT) || vr.equals(VR.TM)) {
				return element.getDate(true);
			} else {
				// If we've fallen to here, we can't convert
				return "(unable to convert VR " + vr.toString() + ")";
			}
		} catch (UnsupportedOperationException e) {
			return "(unable to display this value)";
		}
	}
}
