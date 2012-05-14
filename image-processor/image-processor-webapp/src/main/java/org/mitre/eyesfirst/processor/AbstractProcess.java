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

import javax.swing.event.EventListenerList;

import org.slf4j.LoggerFactory;

/**
 * Abstract class that represents a process that can be run.
 * @author dpotter
 */
public abstract class AbstractProcess {
	public static enum Status {
		/**
		 * The process has not been run yet and is waiting to be started.
		 */
		WAITING(false),
		/**
		 * The process is currently running.
		 */
		RUNNING(false),
		/**
		 * The process failed to complete successfully.
		 */
		FAILED(true),
		/**
		 * The process has completed successfully.
		 */
		COMPLETED(true);
		private final boolean dead;
		private Status(boolean dead) {
			this.dead = dead;
		}
		/**
		 * Determines whether this status is "dead" - that is, a status in which
		 * the process is no longer running and is not waiting to run.
		 * @return
		 */
		public boolean isDead() {
			return dead;
		}
	}

	private Status status = Status.WAITING;
	private Throwable exception = null;
	private final EventListenerList statusListeners = new EventListenerList();
	private long uid;

	/**
	 * Gets the {@link ProcessManager} assigned UID. This field only has meaning
	 * after a process has been added to a {@code ProcessManager}.
	 * @return the UID used to retrieve this process from a process manager
	 */
	public final long getUID() {
		return uid;
	}

	/**
	 * Sets the UID (used by the {@link ProcessManager}). Only intended to be
	 * invoked by the {@code ProcessManager}.
	 * 
	 * @param uid
	 *            the new UID
	 */
	final void setUID(long uid) {
		this.uid = uid;
	}

	/**
	 * Gets the current process status, as set by {@link #setStatus(Status)}.
	 * @return the current process status
	 */
	public synchronized final Status getStatus() {
		return status;
	}

	/**
	 * Changes the process status. If set to a new status, this will alert all
	 * registered listeners of the new status.
	 * @param status the new status
	 */
	protected synchronized final void setStatus(Status status) {
		if (status == null)
			throw new NullPointerException();
		if (this.status != status) {
			ProcessStatusEvent event = new ProcessStatusEvent(this, this.status, status);
			this.status = status;
			Object[] listeners = statusListeners.getListenerList();
			// The listeners list is stored in [ Listener class, listener ]
			// pairs. This makes a vague amount of sense in cases where the same
			// list object is being used to store multiple event listener types.
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				try {
					if (listeners[i] == ProcessStatusListener.class) {
						((ProcessStatusListener) listeners[i+1]).processStatusChanged(event);
					}
				} catch (Exception e) {
					LoggerFactory.getLogger(getClass()).warn("Unhandled exception in process status event handler", status);
				}
			}
		}
	}

	/**
	 * Adds a process status listener.
	 * 
	 * @param listener
	 *            the listener to add
	 */
	public synchronized final void addStatusListener(ProcessStatusListener listener) {
		if (listener != null)
			statusListeners.add(ProcessStatusListener.class, listener);
	}

	/**
	 * Remove an existing listener.
	 * 
	 * @param listener
	 *            the listener to remove
	 */
	public synchronized final void removeStatusListener(ProcessStatusListener listener) {
		if (listener != null)
			statusListeners.remove(ProcessStatusListener.class, listener);
	}

	/**
	 * Gets the exception that caused the process to fail, or {@code null} if
	 * the process didn't fail (status is not {@link Status#FAILED}) or the
	 * failure was not caused by an exception.
	 * 
	 * @return the exception that caused the process to fail, or {@code null} if
	 *         there was no exception
	 */
	public synchronized final Throwable getException() {
		return exception;
	}

	/**
	 * Changes the process status to {@link Status#FAILED} and sets the exception
	 * to the given exception.
	 * @param t
	 */
	protected synchronized final void setException(Throwable t) {
		setStatus(Status.FAILED);
		exception = t;
	}

	/**
	 * Execute the process asynchronously. The default implementation creates a
	 * new {@link Thread} and then invokes {@link #runProcess()} from that
	 * {@code Thread}.
	 */
	public synchronized void start() {
		if (status != Status.RUNNING) {
			new Thread(new Runnable() {
				public void run() {
					try {
						runProcess();
						setStatus(Status.COMPLETED);
					} catch (Throwable t) {
						setException(t);
						// We shouldn't try and handle instances of Error, though,
						// let that propagate.
						if (t instanceof Error)
							throw (Error) t;
					}
				}
			}).start();
			setStatus(Status.RUNNING);
		}
	}

	/**
	 * Indicates that a process should be stopped. The default implementation
	 * does nothing.
	 */
	public void stop() {
	}

	/**
	 * Destroy this process. This is called to free any resources associated
	 * with the process via {@link ProcessManager#destroy()}. The default
	 * implementation calls {@link #stop()} but otherwise does nothing.
	 */
	public void destroy() {
		stop();
	}

	/**
	 * Actually run the process. This is invoked via {@link #start()}.
	 * 
	 * @throws Exception
	 *             if an error occurs running the process. The default
	 *             implementation will catch that exception, and use
	 *             {@link #setException(Throwable)} to set the exception, which
	 *             will move the process into the {@link Status#FAILED} state.
	 */
	protected abstract void runProcess() throws Exception;

	/**
	 * Gets a human-readable name for this process. The default method returns
	 * the last portion of the current class name.
	 * @return
	 */
	public String getName() {
		String name = getClass().getName();
		int i = name.lastIndexOf('.');
		if (i >= 0)
			name = name.substring(i+1);
		return name;
	}

	/**
	 * Gets a human-readable string indicating the current status. The default
	 * implementation just returns {@link #getStatus()}{@code .toString()}
	 * (unless the status is {@link Status#FAILED}, in which case
	 * {@link #getException()}{@code .toString()} is used if non-{@code null}),
	 * but implementations are encouraged to do something more useful. For
	 * example, they could return the current step of the process, or some
	 * other indicator of how far along the process is or what it's doing.
	 * 
	 * @return a human-readable string describing the current status of this
	 *         process
	 */
	public String getStatusString() {
		// Use getStatus() to deal with locking.
		Status st = getStatus();
		if (st == Status.FAILED) {
			Throwable t = getException();
			if (t != null)
				return t.toString();
		}
		return st.toString();
	}

	/**
	 * Gets the total number of units worked. Together with
	 * {@link #getTotalWorkUnits()}, this forms a percentage of how far along
	 * the process is. The default implementation always returns 0.
	 * <p>
	 * Note that while the returned value should always be between 0 and the
	 * result of {@code getTotalWorkUnits()} (inclusive), callers should be
	 * prepared to handle values outside this range.
	 * 
	 * @return the number of work units currently complete, this should always
	 *         be less than or equal to {@link #getTotalWorkUnits()}
	 */
	public int getUnitsWorked() {
		return 0;
	}

	/**
	 * Get the total number of work units this process requires, or 0 if the
	 * total amount of work required is not known. Together with
	 * {@link #getUnitsWorked()}, this can be used to generate a percent of how
	 * far along this process is. The default implementation always returns 0.
	 * <p>
	 * Note that while negative numbers should not be returned, callers are
	 * encouraged to treat negative numbers as 0.
	 * 
	 * @return how many work units there are in total, this should always be 0
	 *         or positive
	 * @see #getUnitsWorked()
	 */
	public int getTotalWorkUnits() {
		return 0;
	}
}
