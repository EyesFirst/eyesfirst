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

package org.mitre.eyesfirst.applet;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import netscape.javascript.JSObject;

import org.dcm4che2.data.DicomObject;
import org.mitre.eyesfirst.Uploader;
import org.mitre.eyesfirst.ZipFileCollection;
import org.mitre.eyesfirst.dcm4che2.DCM4CHE2Util;
import org.mitre.eyesfirst.dicom.DicomJSONConverter;
import org.mitre.eyesfirst.ui.ProgressMonitor;

public class UploaderApplet extends Applet {
	private static final long serialVersionUID = -7420324102994361269L;

	private JFileChooser fileChooser;
	private JDialog fileChooserDialog;
	private JSObject fileChooserCallbacks;

	private Preferences preferences;
	private String currentDirectory;

	private Uploader uploader = new Uploader();

	private String efid;

	private File dicomFile;
	private File imageryFile;
	private File fundusFile;

	/**
	 * Boolean flag indicating if the applet has initialized.
	 */
	private boolean ready = false;
	/**
	 * JSObject to callback when the applet is ready. (See
	 * {@link #checkReady(JSObject)} for details.)
	 */
	private JSObject readyCallback;

	/**
	 * An error message to send to the JavaScript if the applet knows it cannot
	 * run for some reason.
	 */
	private String errorMessage;

	public String getClasspathString() {
		StringBuffer classpath = new StringBuffer();
		ClassLoader applicationClassLoader = this.getClass().getClassLoader();
		if (applicationClassLoader == null) {
			applicationClassLoader = ClassLoader.getSystemClassLoader();
		}
		URL[] urls = ((URLClassLoader) applicationClassLoader).getURLs();
		for (int i = 0; i < urls.length; i++) {
			classpath.append(urls[i].getFile()).append("\r\n");
		}
		return classpath.toString();
	}

	public void setEFID(String efid) {
		this.efid = efid;
		System.out.println("EFID=" + efid);
	}

	public void processDeID(final JSObject callbacks) {
		AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				new Thread(new ProcessDeIDRunnable(callbacks)).start();
				return true;
			}
		});
	}

	private class ProcessDeIDRunnable implements Runnable {
		private final JSObject callbacks;
		public ProcessDeIDRunnable(JSObject callbacks) {
			this.callbacks = callbacks;
		}
		@Override
		public void run() {
			try {
				uploader.reset();
				if (fundusFile != null) {
					uploader.processFundus(fundusFile);
				}
				List<DicomObject> dicomDump = uploader.process(new ZipFileCollection(dicomFile), new ZipFileCollection(imageryFile), efid, new JSObjectProgressMonitor(callbacks));
				for (DicomObject object : dicomDump) {
					callbacks.call("displayDicomFile", new Object[] { DicomJSONConverter.convertToJSON(object) });
				}
				callbacks.call("done", new Object[] { });
			} catch (Exception e) {
				e.printStackTrace();
				invokeError(callbacks, e);
			}
		}
	}

	public void upload(final JSObject callbacks) {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				new Thread(new UploadRunnable(callbacks)).start();
				return null;
			}
		});
	}

	private class UploadRunnable implements Runnable {
		private final JSObject callbacks;
		public UploadRunnable(JSObject callbacks) {
			this.callbacks = callbacks;
		}
		@Override
		public void run() {
			try {
				ProgressMonitor pm = new JSObjectProgressMonitor(callbacks);
				uploader.upload(efid, pm);
				uploader.uploadFundus(pm);
				callbacks.call("done", new Object[] { });
			} catch (Exception e) {
				e.printStackTrace();
				invokeError(callbacks, e);
			}
		}
	}

	/**
	 * Internal method to invoke the error callback from JavaScript. Takes a
	 * JSObject and invokes {@code error(name, message, trace)} on it, where
	 * {@code name} is the exception class name, {@ccode message} is the
	 * exception message, and {@code trace} is the actual stack trace.
	 * 
	 * @param callbacks
	 *            the JavaScript object to invoke {@code error} on
	 * @param error
	 *            the error to notify JavaScript about
	 */
	private static void invokeError(JSObject callbacks, Throwable error) {
		StringWriter string = new StringWriter();
		PrintWriter trace = new PrintWriter(string);
		error.printStackTrace(trace);
		trace.flush();
		callbacks.call("error", new Object[] { error.getClass().getName(), error.getMessage(), string.toString() });
	}

	@Override
	public void init() {
		// First up, let's see if we can use the system look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// Apparently not. Just eat this, it's completely pointless.
		}
		fileChooser = new JFileChooser();
		fileChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (JFileChooser.APPROVE_SELECTION.equals(event.getActionCommand())) {
					// Update the current directory
					preferences.put("currentDirectory", fileChooser.getCurrentDirectory().getAbsolutePath());
					fileChooserCallbacks.call("accepted", new Object[] { fileChooser.getSelectedFile().getAbsolutePath() });
				} else {
					fileChooserCallbacks.call("canceled", new Object[] { });
				}
				fileChooserCallbacks = null;
				fileChooserDialog.setVisible(false);
			}
		});
		fileChooserDialog = new JDialog();
		fileChooserDialog.getContentPane().add(fileChooser, BorderLayout.CENTER);
		preferences = Preferences.userRoot().node("org/eyesfirst/uploader");
		currentDirectory = preferences.get("currentDirectory", null);
		if (currentDirectory != null)
			fileChooser.setCurrentDirectory(new File(currentDirectory));

		uploader.setSessionInformation(getParameter("hostname"), getParameter("Cookie"));

		DCM4CHE2Util.setImageIOSettings();

		System.out.println("Classpath: ");
		System.out.println(getClasspathString());
		System.out.println("End Classpath");

		System.out.println("Scanning for plugins");
		ImageIO.scanForPlugins();
		System.out.println("Printing Image Writer Capabilities");
		boolean haveJpeg2000 = false;
		for (String s : ImageIO.getWriterFormatNames()) {
			if (s.equalsIgnoreCase("jpeg2000")) {
				haveJpeg2000 = true;
			}
			System.out.println(s + ": ");
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(s);
			while (iter.hasNext()) {
				System.out.println(iter.next());
			}
		}
		if (!haveJpeg2000) {
			System.out.println("No JPEG 2000 library found.");
			errorMessage = "Unable to initialize JPEG2000 decoder.";
		}
		System.out.println("End Printing Image Writer Capabilities");

		invokeReadyCallback(true);
	}

	/**
	 * JavaScript callback to ensure that the applet is ready. If it is, it
	 * calls success. If it isn't, it calls error.
	 * @param object
	 */
	public synchronized void checkReady(JSObject object) {
		readyCallback = object;
		invokeReadyCallback(false);
	}

	/**
	 * Internal method to invoke the ready callback, if there is one. There is
	 * some multithreaded stuff going on here, but basically: {@link #init()}
	 * is <strong>not</strong> synchronized. This allows {@link #checkReady(JSObject)}
	 * to return immediately if the applet is <strong>not</strong> ready, and
	 * to wait for {@code init()} to call this method on its own.
	 * <p>
	 * {@code checkReady(JSObject)}
	 * invokes this with {@code false} so that if it's called after the applet
	 * is ready, it will work as normal.
	 * <p>
	 * {@code init()}, on the other hand, invokes this with {@code true} so that
	 * when the applet is ready, it will always set {@code ready} to true with
	 * the monitor locked, and then properly invoke the callback only when the
	 * applet has completely initialized.
	 * 
	 * @param setReady
	 *            {@code true} to set ready to {@code true}, {@code false} to
	 *            leave it as-is
	 */
	private synchronized void invokeReadyCallback(boolean setReady) {
		if (setReady)
			ready = true;
		if (ready) {
			if (readyCallback != null) {
				if (errorMessage == null) {
					readyCallback.call("success", new Object[] {});
				} else {
					readyCallback.call("error", new Object[] { errorMessage });
				}
				// This is paranoia more than anything else. readyCallback can
				// only be set with the monitor locked, so it shouldn't be able
				// to double-invoke it. If it's ready, that means init() set
				// ready while the callback wasn't set. If the callback gets set
				// once the applet is ready, it will be invoked immediately.
				// But reset it to null anyway - if only to free the no-longer
				// needed JavaScript object. :)
				readyCallback = null;
			}
		}
		// Otherwise, do nothing, and it will be invoked by init() when it's
		// done.
	}

	/**
	 * Browses for a file in another thread. Takes a JavaScript object with
	 * two fields indicating callbacks: {@code accepted(filename)} which is
	 * invoked if a file is choosen, and {@code canceled()} which is invoked
	 * if the browse is canceled.
	 * <p>
	 * Reinvoking this before the callback have been called brings the file
	 * chooser to the front but will not invoke the callbacks given.
	 * @param object
	 */
	public void browseForFiles(final JSObject object) {
		AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (fileChooserDialog.isVisible()) {
							System.out.println("Already visible, bringing to front");
							fileChooserDialog.toFront();
						} else {
							fileChooserDialog.setVisible(true);
							fileChooserDialog.pack();
							fileChooserCallbacks = object;
						}
					}
				});
				return Boolean.TRUE;
			}
		});
	}

	public boolean setDicomFile(final String fileName) {
		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				dicomFile = new File(fileName);
				System.out.println("Dicom file=" + dicomFile);
				return verifyFile(dicomFile);
			}
		});
	}

	public boolean setImageryFile(final String fileName) {
		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				imageryFile = new File(fileName);
				System.out.println("Imagery file=" + imageryFile);
				return verifyFile(imageryFile);
			}
		});
	}

	public boolean setFundusFile(final String fileName) {
		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				fundusFile = new File(fileName);
				System.out.println("Fundus file=" + fundusFile);
				return verifyFile(fundusFile);
			}
		});
	}

	/**
	 * Verify a file exists/is a file. Used fir
	 * @param f
	 * @return
	 */
	private boolean verifyFile(File f) {
		// canRead() is horribly buggy on some platforms, so just ignore
		// that for now.
		return f.isFile();
	}
}
