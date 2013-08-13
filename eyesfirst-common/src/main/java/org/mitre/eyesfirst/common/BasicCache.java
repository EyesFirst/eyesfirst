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
package org.mitre.eyesfirst.common;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic cache.
 * @author dpotter
 *
 * @param <K> the key under which objects are stored
 * @param <V> the objects that are being stored
 */
public class BasicCache<K, V> {
	private static class CleanupThread extends Thread {
		private static final long CLEAR_INTERVAL = 30*60*1000;
		private final Logger log = LoggerFactory.getLogger(getClass());
		private List<BasicCache<?,?>> caches;
		private boolean alive = false;
		public CleanupThread() {
			super("CacheCleaner");
			setDaemon(true);
			caches = new LinkedList<BasicCache<?,?>>();
		}
		public synchronized void addCache(BasicCache<?,?> cache) {
			if (!isAlive()) {
				alive = true;
				start();
			}
			caches.add(cache);
		}
		/**
		 * Removes the given cache. Returns {@code true} if there are no
		 * caches left.
		 * @param cache
		 * @return
		 */
		public synchronized boolean removeCache(BasicCache<?,?> cache) {
			caches.remove(cache);
			if (caches.size() == 0) {
				alive = false;
				interrupt();
				return true;
			}
			return false;
		}
		@Override
		public void run() {
			log.info("Cache cleanup thread is starting...");
			do {
				// Go ahead and do a useless clear at start, allows us to quit
				// faster without making the logic pointlessly convoluted.
				synchronized(this) {
					log.debug("Cleaning up {} caches...", caches.size());
					for (BasicCache<?,?> cache : caches) {
						cache.cleanup();
					}
				}
				try {
					Thread.sleep(CLEAR_INTERVAL);
				} catch (InterruptedException e) {
					// Ignore, it's expected.
				}
			} while (alive);
			log.info("Cache cleanup thread closing (no caches left)");
		}
		
	}
	private static CleanupThread cleaner = null;
	private static synchronized void addCacheToCleaner(BasicCache<?,?> cache) {
		if (cleaner == null) {
			cleaner = new CleanupThread();
		}
		cleaner.addCache(cache);
	}
	private static synchronized void removeCacheFromCleaner(BasicCache<?,?> cache) {
		if (cleaner == null)
			throw new IllegalStateException("Removing cleaner without ever having created it?!");
		if (cleaner.removeCache(cache))
			cleaner = null;
	}

	private Map<K, SoftReference<V>> cache;

	public BasicCache() {
		cache = new HashMap<K, SoftReference<V>>();
		addCacheToCleaner(this);
	}

	public synchronized V get(K key) {
		SoftReference<V> ref = cache.get(key);
		if (ref == null)
			return null;
		V res = ref.get();
		if (res == null) {
			cache.remove(key);
		}
		return res;
	}

	public synchronized void put(K key, V value) {
		cache.put(key, new SoftReference<V>(value));
	}

	/**
	 * Removes any dead entries from the cache.
	 */
	public synchronized void cleanup() {
		Iterator<Map.Entry<K, SoftReference<V>>> iter = cache.entrySet().iterator();
		while (iter.hasNext()) {
			if (iter.next().getValue().get() == null) {
				iter.remove();
			}
		}
	}

	/**
	 * Clears the cache, removing all entries.
	 */
	public synchronized void clear() {
		cache.clear();
	}

	/**
	 * Destroys the cache, freeing up any associated resources.
	 */
	public void destroy() {
		cache.clear();
		cache = null;
		removeCacheFromCleaner(this);
	}

	/**
	 * Invokes destroy. Generally you shouldn't rely on the finalizer to do this
	 * for you.
	 */
	@Override
	protected void finalize() throws Throwable {
		destroy();
		super.finalize();
	}
}
