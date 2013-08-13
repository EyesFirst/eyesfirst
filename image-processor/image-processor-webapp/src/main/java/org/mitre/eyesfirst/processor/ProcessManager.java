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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that handles running multiple simulations via multiple threads.
 * @author DPOTTER
 */
public class ProcessManager {
	// Currently not used.
	/* *
	 * Maximum time in milliseconds before pruning a process. (After a process
	 * has been pruned, it's no longer possible to get updated status reports
	 * on it.)
	 * /
	//private long maxLiveTime = 30*60*1000;*/

	private int maxProcesses = 2;

	/**
	 * Data structure used for storing the process, used to prune old processs.
	 * @author DPOTTER
	 *
	 */
	private static class ProcessEntry {
		// Currently not used.
		//long lastAccess;
		AbstractProcess process;
		public ProcessEntry(AbstractProcess process) {
			this.process = process;
			//lastAccess = System.currentTimeMillis();
		}
		/**
		 * Updates the "sinceLast" time to the current time.
		 */
		public void touch() {
			//lastAccess = System.currentTimeMillis();
		}
	}

	private final Map<Long, ProcessEntry> processes = new LinkedHashMap<Long, ProcessEntry>(10, 0.75f);
	private final LinkedList<ProcessEntry> waitingQueue = new LinkedList<ProcessEntry>();
	private int runningCount = 0;

	private class CullThread extends Thread {
		/**
		 * Time to poll when checking
		 */
		long pollInterval = 30*60*1000;
		boolean alive = true;
		Logger logger = LoggerFactory.getLogger(getClass());
		public CullThread() {
			super("ProcessManagerCullThread");
		}
		@Override
		public void run() {
			logger.info("Starting dead process cull thread...");
			while (alive) {
				try {
					Thread.sleep(pollInterval);
				} catch (InterruptedException e) {
					// Don't care
				}
				logger.info("Removing dead processes...");
				cullDeadProcesses();
			}
			logger.info("Terminating dead process cull thread.");
		}
		public synchronized void kill() {
			alive = false;
			interrupt();
		}
	}

	private class ProcessManagerStatusListener implements ProcessStatusListener {
		private final long uid;
		public ProcessManagerStatusListener(long uid) {
			this.uid = uid;
		}
		@Override
		public void processStatusChanged(ProcessStatusEvent event) {
			// We really only care about a process dying.
			if (event.getNewStatus().isDead() && event.getOldStatus() == AbstractProcess.Status.RUNNING) {
				freeProcess(uid);
			}
		}
	}
	private CullThread cullThread;

	/**
	 * The random used to generate UIDs.
	 */
	private final Random uidGenerator = new Random();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Creates a new process manager.
	 * @param maxProcesses the maximum number of processes to allow to run at
	 * the same time. If greater
	 */
	public ProcessManager(int maxProcesses) {
		this.maxProcesses = maxProcesses;
	}

	public int getMaxProcesses() {
		return maxProcesses;
	}

	/**
	 * Sets the maximum number of processes that can be run at once.
	 * @param maxProcesses
	 */
	public void setMaxProcesses(int maxProcesses) {
		int old = this.maxProcesses;
		this.maxProcesses = maxProcesses;
		if (old > maxProcesses)
			checkRunningProcesses();
	}

	/**
	 * Adds a new process to the queue, generating a new UID for it. The process
	 * will be started immediately if there are enough terminated processes.
	 * @param process the process to track
	 * @return a new, randomly generated UID for this process
	 */
	public synchronized long queueProcess(AbstractProcess process) {
		if (process == null)
			throw new NullPointerException();
		// TODO: See if this process already belongs to a process manager
		long uid = uidGenerator.nextLong();
		// Prevent collisions (should be very unlikely)
		while (processes.containsKey(uid)) {
			uid = uidGenerator.nextLong();
		}
		if (cullThread == null) {
			cullThread = new CullThread();
			cullThread.start();
		}
		process.setUID(uid);
		ProcessEntry entry = new ProcessEntry(process);
		processes.put(uid, entry);
		process.addStatusListener(new ProcessManagerStatusListener(uid));
		// Queue it up
		waitingQueue.addLast(entry);
		checkRunningProcesses();
		return uid;
	}

	private synchronized void freeProcess(long uid) {
		// TODO: Make sure that we agree that this process is one of the processes
		// that we were running. But for now:
		runningCount--;
		logger.debug("End process {}, have {} processes still running.", uid, runningCount);
		checkRunningProcesses();
	}

	/**
	 * Checks to see if we can start any waiting processes, and does if we can.
	 */
	private synchronized void checkRunningProcesses() {
		// If maxProcesses is 0 or lower, we treat it as "infinite"
		if (maxProcesses <= 0 || runningCount < maxProcesses) {
			if (!waitingQueue.isEmpty()) {
				// Remove the first entry from the queue
				ProcessEntry entry = waitingQueue.removeFirst();
				// And start it
				entry.process.start();
				runningCount++;
				logger.debug("Starting process {}, have {} running processes, wait queue contains {} entries.", new Object[] { entry.process, runningCount, waitingQueue.size() });
			}
		}
	}

	/**
	 * Gets an array of all currently known processes.
	 * @return
	 */
	public synchronized AbstractProcess[] getProcesses() {
		AbstractProcess[] result = new AbstractProcess[processes.size()];
		Iterator<ProcessEntry> iter = processes.values().iterator();
		for (int i = 0; iter.hasNext(); i++) {
			result[i] = iter.next().process;
		}
		return result;
	}

	/**
	 * Gets the process with the given UID, or returns {@code null} if there is
	 * no such process.
	 * @param uid
	 * @return
	 */
	public synchronized AbstractProcess getProcess(long uid) {
		ProcessEntry entry = processes.get(uid);
		if (entry == null) {
			return null;
		} else {
			entry.touch();
			return entry.process;
		}
	}

	/**
	 * Removes the process with the given UID. Note that you cannot remove a
	 * running process (a process in the {@link AbstractProcess.Status#RUNNING}
	 * state).
	 * 
	 * @param uid
	 *            the UID of the process to remove
	 * @return {@code true} if the process was removed, {@code false} if no
	 *         process was running with that ID
	 * @throws IllegalStateException
	 *             if the process with that UID is currently running
	 */
	public synchronized boolean removeProcess(long uid) {
		ProcessEntry entry = processes.get(uid);
		if (entry == null)
			return false;
		// Otherwise, determine if we even can remove this process
		if (entry.process.getStatus() == AbstractProcess.Status.RUNNING) {
			throw new IllegalStateException("Process is still running");
		}
		processes.remove(uid);
		return true;
	}

	synchronized void stopCullThread() {
		if (cullThread != null) {
			cullThread.kill();
			cullThread = null;
		}
	}

	// Currently not used
	//private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	/**
	 * Remove processes that have not been checked on for too long.
	 */
	synchronized void cullDeadProcesses() {
		// Removes processes that have completed and haven't been checked on
		// for maxLiveTime milliseconds.
		//long cullBefore = System.currentTimeMillis() - maxLiveTime;
		//int before = processes.size();
		Iterator<Map.Entry<Long, ProcessEntry>> iter = processes.entrySet().iterator();
		int runningCount = 0;
		while (iter.hasNext()) {
			Map.Entry<Long, ProcessEntry> entry = iter.next();
			ProcessEntry pe = entry.getValue();
			AbstractProcess.Status status = pe.process.getStatus();
			// For now, we never remove "dead" processes to ensure the status
			// is maintained. So instead all this does is check to make sure
			// that the run count doesn't get messed up.
			/*if (pe.lastAccess < cullBefore && status.isDead()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Process {} last accessed {}, before cutoff of {}.",
							new Object[] { uidToString(entry.getKey()),
							dateFormat.format(new Date(entry.getValue().lastAccess)),
							dateFormat.format(new Date(cullBefore)) });
				}
				pe.process.destroy();
				iter.remove();
			} else */
			if (status == AbstractProcess.Status.RUNNING) {
				runningCount++;
			}
		}
		//logger.info("Removed {} processes.", before - processes.size());
		if (this.runningCount != runningCount) {
			logger.error("Actual running count {} does not match stored running count of {}! This is a bug!", runningCount, this.runningCount);
			// Fix this
			this.runningCount = runningCount;
			checkRunningProcesses();
		}
	}

	/**
	 * Destroys the process manager. All existing processes will have
	 * {@link AbstractProcess#destroy()} called on them, and the wait queue and
	 * existing process maps will be cleared. The process manager should no
	 * longer be used after it has been destroyed.
	 */
	public synchronized void destroy() {
		logger.info("Destroying process manager...");
		for (Map.Entry<Long, ProcessEntry> entry : processes.entrySet()) {
			entry.getValue().process.destroy();
		}
		processes.clear();
		waitingQueue.clear();
		runningCount = 0;
		stopCullThread();
	}

	/**
	 * Converts the given UID to a URL-safe string. Convert back to a UID via
	 * {@link #stringToUid(String)}.
	 * @param uid
	 * @return
	 */
	public static String uidToString(long uid) {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putLong(uid);
		return Base64.encodeBase64URLSafeString(buf.array());
	}

	/**
	 * Converts the given string encoded via {@link #uidToString(long)} back
	 * into a UID. The commons codec encoder does not validate that the input
	 * string is valid - any non-Base 64 characters are silently discarded.
	 * <p>
	 * However, there still is a requirement that the given string decode into 8
	 * bytes.
	 * 
	 * @param str
	 *            the string to convert
	 * @return the decoded string
	 * @throws IllegalArgumentException
	 *             if the given string does not decode into an 8-byte long
	 */
	public static long stringToUid(String str) {
		byte[] data = Base64.decodeBase64(str);
		if (data.length != 8) {
			throw new IllegalArgumentException("Invalid UID string " + str);
		}
		ByteBuffer buf = ByteBuffer.wrap(data);
		return buf.getLong();
	}
}
