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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dcm4che2.data.DicomObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps around an existing {@link DicomAccessService}, caching requests.
 *
 * @author dpotter
 */
public class CachingDicomAccessService implements DicomAccessService {
	private static final long CLEAR_INTERVAL = 30*60*1000;
	private final Map<DicomID, CacheEntry> cache;
	private final DicomAccessService wrapped;
	private final Logger log;
	private final CacheCleanupThread cacheCleanupThread;

	/**
	 * Thread responsible for removing dead cache entries (entries where the
	 * object being cached has been GCed, but the ID and soft reference are
	 * hanging around anyway).
	 *
	 * @author dpotter
	 */
	private class CacheCleanupThread extends Thread {
		private boolean alive = true;

		public CacheCleanupThread(String name) {
			super(name);
			setDaemon(true);
		}

		public void run() {
			Logger log = CachingDicomAccessService.this.log;
			log.info("Starting cache clean-up thread.");
			do {
				// Go ahead and do a useless clear at start, allows us to quit
				// faster without making the logic pointlessly convoluted.
				clearDeadCacheEntries();
				try {
					Thread.sleep(CLEAR_INTERVAL);
				} catch (InterruptedException e) {
					// Ignore, it's fine/expected
				}
			} while (alive);
			log.info("Cache clean-up thread complete.");
		}

		/**
		 * Notifies the cache cleanup thread to end.
		 */
		public void end() {
			alive = false;
			interrupt();
		}
	}

	/**
	 * Class containing an actual entry in the cache.
	 *
	 * @author dpotter
	 */
	private static class CacheEntry {
		private final DicomID id;
		private final Logger log;
		private SoftReference<DicomObject> entry;

		/**
		 * Create a new cache entry.
		 * 
		 * @param id
		 *            the ID of the entry
		 * @param log
		 *            the logger to use to log status information
		 * @throws NullPointerException
		 *             if the ID or log are {@code null}
		 */
		public CacheEntry(DicomID id, Logger log) {
			if (id == null || log == null)
				throw new NullPointerException();
			this.id = id;
			this.log = log;
		}

		/**
		 * Checks if this entry is dead: the soft reference has been GCed, or
		 * the entry was never created.
		 * 
		 * @return {@code true} if this cache entry is dead
		 */
		public boolean isDead() {
			return entry == null || entry.get() == null;
		}

		/**
		 * Fetches the data associated with this cache entry.
		 * 
		 * @param service
		 *            the service to use to fetch the actual data
		 * @return the data associated with this object or {@code null} if the
		 *         given {@code service} returned {@code null} (the object does
		 *         not exist)
		 * @throws DicomAccessException
		 *             if the given {@code service} raises an exception
		 */
		public synchronized DicomObject get(DicomAccessService service) throws DicomAccessException {
			if (entry != null) {
				DicomObject result = entry.get();
				if (result != null) {
					log.debug("Using cached entry for {}.", id);
					return result;
				}
				log.debug("Cache entry reclaimed for {}, refetching.", id);
				entry = null;
				// Otherwise, fall through and fetch
			} else {
				log.debug("Fetching new entry for {}.", id);
			}
			DicomObject object = service.retrieveDicomObject(id.getStudyUID(), id.getSeriesUID(), id.getObjectUID());
			if (object == null) {
				log.debug("No result found for {}.", id);
			} else {
				log.debug("Caching {}.", id);
				// We don't bother caching failures.
				entry = new SoftReference<DicomObject>(object);
			}
			return object;
		}
	}

	/**
	 * Creates a new caching DICOM access service, which caches results returned
	 * from the given access service.
	 * 
	 * @param wrapped
	 *            the access service to use to fetch actual DICOM, which is then
	 *            cached within this class
	 */
	public CachingDicomAccessService(DicomAccessService wrapped) {
		if (wrapped == null)
			throw new NullPointerException();
		log = LoggerFactory.getLogger(getClass().getName() + "." + wrapped.getClass().getName());
		this.wrapped = wrapped;
		cache = new HashMap<DicomID, CacheEntry>();
		cacheCleanupThread = new CacheCleanupThread("CachingDicomAccessService-" + wrapped.getClass().getName());
		cacheCleanupThread.start();
	}

	/**
	 * Retrieves a DICOM object, using the cached version (if any) before
	 * invoking the wrapped {@link DicomAccessService}. Cached entries are
	 * (conceptually) kept indefinitely before being flushed. They are stored
	 * using a {@link SoftReference}. Failures (a {@code null} result from
	 * the wrapped {@code DicomAccessService}) will not be cached and will
	 * always be reattempted.
	 * 
	 * @param studyUID
	 *            the DICOM study UID
	 * @param seriesUID
	 *            the DICOM series UID
	 * @param objectUID
	 *            the DICOM object UID
	 * @throws DicomAccessException
	 *             if the cache does not contain the data and fetching it
	 *             through the {@code DicomAccessService} throws this exception
	 */
	@Override
	public DicomObject retrieveDicomObject(String studyUID, String seriesUID,
			String objectUID) throws DicomAccessException {
		DicomID id = new DicomID(studyUID, seriesUID, objectUID);
		CacheEntry entry;
		synchronized(this) {
			// We need to lock while fetching the object from the cache.
			entry = cache.get(id);
			if (entry == null) {
				// Create a new entry for this ID
				entry = new CacheEntry(id, log);
				cache.put(id, entry);
			}
		}
		return entry.get(wrapped);
	}

	/**
	 * If a given cache entry exists, removes it from the cache.
	 *
	 * @param studyUID
	 *            the DICOM study UID
	 * @param seriesUID
	 *            the DICOM series UID
	 * @param objectUID
	 *            the DICOM object UID
	 * @return {@code true} if the object existed and was removed, {@code false}
	 * if nothing was there
	 */
	public synchronized boolean clearCacheEntry(String studyUID,
			String seriesUID, String objectUID) {
		return cache.remove(new DicomID(studyUID, seriesUID, objectUID)) != null;
	}

	/**
	 * Removes all cache entries.
	 */
	public synchronized void clearCache() {
		log.info("Clearing {} cache entries.", cache.size());
		cache.clear();
	}

	/**
	 * Actual method for removing dead cache entries.
	 */
	private synchronized void clearDeadCacheEntries() {
		log.info("Clearing dead cache entries...");
		Iterator<CacheEntry> iter = cache.values().iterator();
		int removed = 0;
		while (iter.hasNext()) {
			CacheEntry entry = iter.next();
			if (entry.isDead()) {
				iter.remove();
				removed++;
			}
		}
		log.info("Removed {} dead cache entries.", removed);
	}

	/**
	 * Destroys various cache resources before invoking the wrapped
	 * {@link DicomAccessService}'s {@code destroy()} method.
	 */
	@Override
	public void destroy() {
		cacheCleanupThread.end();
		clearCache();
		wrapped.destroy();
	}
}
