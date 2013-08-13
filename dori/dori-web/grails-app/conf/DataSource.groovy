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
dataSource {
	dbCreate = "update" // one of 'create', 'create-drop','update'
	pooled = true
	driverClassName = "org.apache.derby.jdbc.EmbeddedDriver"
	dialect='org.hibernate.dialect.DerbyDialect'
	url = "jdbc:derby:memory:doriweb;create=true"
	username = "doriweb"
	password = "doriweb"
}
hibernate {
	cache.use_second_level_cache = true
	cache.use_query_cache = true
	cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
}

// environment specific settings
environments {
	development {
		dataSource {
			dbCreate = "update"//"create-drop"
			driverClassName = "com.mysql.jdbc.Driver"
			dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
			url = "jdbc:mysql://localhost:3306/doriweb"
			pooled = true
		}
	}
	test { dataSource { } }
	production {
		dataSource {
			dbCreate = "update"
			driverClassName = "com.mysql.jdbc.Driver"
			dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
			url = "jdbc:mysql://localhost:3306/doriweb"
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
