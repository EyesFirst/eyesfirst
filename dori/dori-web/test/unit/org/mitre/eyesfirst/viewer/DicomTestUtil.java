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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.codehaus.jackson.JsonGenerationException;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.mitre.eyesfirst.viewer.web.DicomJSONConverter;

/**
 * Simple utility to test the DICOM features.
 * @author DPOTTER
 *
 */
public class DicomTestUtil {
	private final JFrame frame;
	private final JFileChooser fileChooser;
	private final JLabel displayArea;
	private DicomMetadataTableModel metadataTableModel;

	public DicomTestUtil() {
		frame = new JFrame("DICOM Test");
		JMenuBar menuBar = new JMenuBar();
		frame.getContentPane().add(menuBar, BorderLayout.NORTH);
		JMenu fileMenu = new JMenu("File");
		JMenuItem openMenuItem = new JMenuItem("Open...");
		fileMenu.add(openMenuItem);
		menuBar.add(fileMenu);
		openMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openDicomFile();
			}
		});
		JTabbedPane tabs = new JTabbedPane();
		frame.getContentPane().add(tabs, BorderLayout.CENTER);
		displayArea = new JLabel("Choose File -> Open to load a DICOM file.");
		metadataTableModel = new DicomMetadataTableModel();
		JTable metadataTable = new JTable(metadataTableModel);
		metadataTable.setAutoCreateRowSorter(true);
		tabs.addTab("Meta Data", new JScrollPane(metadataTable));
		tabs.addTab("Image", new JScrollPane(displayArea));
		fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileNameExtensionFilter("DICOM Files", "dcm"));
		Dimension size = frame.getToolkit().getScreenSize();
		frame.setSize(size.width * 3 / 4, size.height * 3 / 4);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void openDicomFile() {
		if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			openDicomFile(fileChooser.getSelectedFile());
		}
	}

	private static class DicomIcon implements Icon {
		private final int width, height;
		private final Image image;
		public DicomIcon(Image image, int width, int height) {
			this.image = image;
			this.width = width;
			this.height = height;
		}

		@Override
		public int getIconWidth() {
			return width;
		}

		@Override
		public int getIconHeight() {
			return height;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.drawImage(image, x, y, width, height, c);
		}
	}

	public void openDicomFile(File f) {
		if (f == null)
			throw new NullPointerException();
		fileChooser.setCurrentDirectory(f.getParentFile());
		DicomObject obj;
		try {
			obj = new DicomInputStream(new FileInputStream(f)).readDicomObject();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Unable to open DICOM file: " + e, "Error Loading DICOM", JOptionPane.ERROR_MESSAGE);
			return;
		}
		metadataTableModel.setDicomObject(obj);
		double aspectRatio = DicomJSONConverter.findAspectRatio(obj);
		System.out.println("Aspect ratio=" + aspectRatio);
		Iterator<DicomElement> iter = obj.iterator();
		while (iter.hasNext()) {
			DicomElement elem = iter.next();
			if (elem.tag() == Tag.PixelData) {
				byte[] pixels;
				if (elem.hasItems()) {
					pixels = elem.getFragment(elem.countItems() / 2);
				} else {
					pixels = elem.getBytes();
				}
				BufferedImage image;
				try {
					image = ImageIO.read(new ByteArrayInputStream(pixels));
					System.out.println("Image: " + image);
					if (image == null) {
						displayArea.setText("Unable to read image data.");
						displayArea.setIcon(null);
					} else {
						displayArea.setText("");
						int width = image.getWidth();
						int height = image.getHeight();
						if (aspectRatio > 1.0) {
							width = (int)(width * aspectRatio + 0.5);
						} else {
							height = (int)(height / aspectRatio + 0.5);
						}
						displayArea.setIcon(new DicomIcon(image, width, height));
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		try {
			DicomJSONConverter.convertToJSON(obj, System.out);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get the test DICOM object.
	 * @return the test DICOM object
	 * @throws RuntimeException if an error occurs that prevents the object from
	 * being loaded
	 */
	public static DicomObject loadTestDicom() {
		try {
			return new DicomInputStream(DicomTestUtil.class.getResourceAsStream("sample.dcm")).readDicomObject();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load test image", e);
		}
	}

	public static void main(String[] args) {
		DicomTestUtil util = new DicomTestUtil();
		if (args.length > 0) {
			util.openDicomFile(new File(args[0]));
		}
	}

}
