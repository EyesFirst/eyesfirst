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
package org.mitre.eyesfirst.processor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An external process that is executed using {@link Process}.
 * @author dpotter
 *
 */
public abstract class ExternalProcess extends AbstractProcess {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7834756229140388097L;
	/**
	 * A unique number used to create a name when the process is running. Note
	 * that this means that the PID does not need to be reset when an external
	 * process is deserialized.
	 */
	private static AtomicInteger pid = new AtomicInteger(0);
	private static class StreamReader extends Thread {
		public static final int BUFFER_SIZE = 1024*8;
		private final InputStream in;
		private final boolean err;
		private final ExternalProcess p;
		private boolean alive = true;
		public StreamReader(String name, InputStream stream, boolean err, ExternalProcess p) {
			super(name);
			this.in = stream;
			this.err = err;
			this.p = p;
			start();
		}
		public void run() {
			byte[] buf = new byte[BUFFER_SIZE];
			while (alive) {
				try {
					int r = in.read(buf);
					if (r < 0)
						break;
					if (err) {
						p.receiveStdErr(buf, r);
					} else {
						p.receiveStdOut(buf, r);
					}
				} catch (IOException e) {
					//
				}
			}
			alive = false;
		}
		public void quit() {
			alive = false;
			try {
				this.join();
			} catch (InterruptedException e) {
				// Don't care about this
			}
		}
	}

	/**
	 * Whether or not to throw an exception if {@link #getResult()} is non-zero
	 * (usually means the child process failed). The default value is {@code true}.
	 */
	private boolean exceptionOnProcessFailure = true;
	private int result;
	private boolean haveResult = false;
	private final AtomicReference<Process> process = new AtomicReference<Process>();

	static String createName(String command) {
		int i = command.lastIndexOf(File.separatorChar);
		if (i < 0)
			return command;
		else
			return command.substring(i+1);
	}

	protected boolean isExceptionOnProcessFailure() {
		return exceptionOnProcessFailure;
	}

	protected void setExceptionOnProcessFailure(boolean exceptionOnProcessFailure) {
		this.exceptionOnProcessFailure = exceptionOnProcessFailure;
	}

	/**
	 * Runs the external process. If the external process signals an error
	 * (returns a non-zero result), then an exception will be thrown if
	 * {@link #exceptionOnProcessFailure} is {@code true}.
	 */
	@Override
	protected void runProcess() throws Exception {
		List<String> arguments = new ArrayList<String>(32);
		arguments.add(getCommand());
		String name = createName(arguments.get(0)) + "-" + pid.addAndGet(1);
		Logger log = LoggerFactory.getLogger(getClass().getName() + "." + ProcessManager.uidToString(getUID()));
		createArguments(arguments);
		ProcessBuilder pb = new ProcessBuilder(arguments);
		pb.directory(getWorkingDirectory());
		configureProcess(pb);
		log.info("Starting process {}", name);
		if (log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Process arguments:");
			for (String s : arguments) {
				sb.append(' ');
				sb.append('"');
				sb.append(s);
				sb.append('"');
			}
			log.debug(sb.toString());
		}
		Process p = pb.start();
		process.set(p);
		// We need to read in the stdout/stderr, or the process may block
		// waiting for us to do so. So we need to spawn threads for each stream,
		// because while NIO offers selecting, it does not work with processes.
		// Yay Java.
		StreamReader stdout = new StreamReader(name, p.getInputStream(), false, this);
		StreamReader stderr = null;
		if (!pb.redirectErrorStream()) {
			stderr = new StreamReader(name + "-err", p.getErrorStream(), true, this);
		}
		// Once that's done, nothing to do but wait.
		result = p.waitFor();
		haveResult = true;
		// I'm not sure this is necessary, but whatever:
		stdout.quit();
		if (stderr != null)
			stderr.quit();
		if (exceptionOnProcessFailure && result != 0)
			throw new Exception("Process returned failure result (exit code " + result + ")");
	}

	@Override
	public void stop() {
		Process p = process.getAndSet(null);
		if (p != null)
			p.destroy();
	}

	/**
	 * Gets the name of the command to run.
	 * 
	 * @return the name of the command to run
	 */
	protected abstract String getCommand();

	/**
	 * Create arguments for the command. The default implementation does
	 * nothing. The list given will initially contain a single value, the result
	 * of {@link #getCommand()}. While it's possible to change the command run
	 * by inserting values prior to this, it's not recommended.
	 * 
	 * @param arguments
	 *            the list of arguments
	 */
	protected void createArguments(List<String> arguments) {
		// Does nothing.
	}

	/**
	 * Gets the working directory where the process should run. The default
	 * method returns {@code null}, which causes the current working directory
	 * to be used.
	 * 
	 * @return the working directory where the child process should be run, or
	 *         {@code null} to use the current directory
	 */
	protected File getWorkingDirectory() {
		return null;
	}

	/**
	 * Configure the process immediately prior to it being invoked. The default
	 * method does nothing.
	 * @param processBuilder
	 */
	protected void configureProcess(ProcessBuilder processBuilder) {
		// Nothing
	}

	/**
	 * Gets the result exit code from the process. If called before the process
	 * has run, the result is undefined.
	 * @return the exit code from the process
	 */
	protected int getResult() {
		return result;
	}

	/**
	 * Receive data from standard out. The default implementation does nothing.
	 * Note that this will be called on a separate thread from
	 * {@link #runProcess()}.
	 * 
	 * @param b
	 *            the buffer containing data from the child's standard out
	 * @param length
	 *            the number of bytes received (the buffer may be larger than
	 *            length, but any data after length is meaningless)
	 */
	protected void receiveStdOut(byte[] b, int length) {
		// Nothing.
	}

	/**
	 * Receive data from standard error. The default implementation does
	 * nothing. Note that this will be called on a separate thread from both
	 * {@link #runProcess()} and {@link #receiveStdOut(byte[], int)} as Java
	 * (still) does not support using selectors on processes. Note that if the
	 * original process was configured to redirect standard error to standard
	 * out, this method will never be invoked.
	 * 
	 * @param b
	 *            the buffer containing data from the child's standard error
	 * @param length
	 *            the number of bytes received (the buffer may be larger than
	 *            length, but any data after length is meaningless)
	 */
	protected void receiveStdErr(byte[] b, int length) {
		// Nothing.
	}

	/**
	 * Gets the current status as a string. This is basically identical to
	 * {@link AbstractProcess#getStatusString()}, except that when the process
	 * has finished, the result code is appended.
	 */
	@Override
	public String getStatusString() {
		AbstractProcess.Status status = getStatus();
		String res = super.getStatusString();
		if (status == AbstractProcess.Status.COMPLETED) {
			if (haveResult && result != 0)
				res += " (exit code " + result + ")";
		}
		return res;
	}

}
