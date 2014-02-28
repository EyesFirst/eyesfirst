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
dataSource {
	pooled = true
	driverClassName = "org.h2.Driver"
	username = "sa"
	password = ""
}
hibernate {
	cache.use_second_level_cache = true
	cache.use_query_cache = false
	cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
}
// environment specific settings
environments {
	development {
		dataSource {
			dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
			url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
		}
	}
	test {
		dataSource {
			dbCreate = "update"
			url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
		}
	}
	production {
		dataSource {
			dbCreate = "update"
			driverClassName = "com.mysql.jdbc.Driver"
			dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
			url = "jdbc:mysql://localhost:3306/eyesfirst"
			username = "eyesfirst"
			password = "eyesfirst"
			pooled = true
			properties {
				maxActive = 50
				maxIdle = 25
				minIdle = 5
				initialSize = 5
				minEvictableIdleTimeMillis = 1800000
				timeBetweenEvictionRunsMillis = 1800000
				maxWait = 10000
			}
		}
	}
}
