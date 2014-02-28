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
grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.dependency.resolution = {
	// inherit Grails' default dependencies
	inherits("global") {
		// specify dependency exclusions here; for example, uncomment this to disable ehcache:
		// excludes 'ehcache'
	}
	log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
	checksums true // Whether to verify checksums on resolve

	repositories {
		inherits true // Whether to inherit repository definitions from plugins

		grailsPlugins()
		grailsHome()
		grailsCentral()

		mavenLocal()
		mavenCentral()

		// uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
		//mavenRepo "http://snapshots.repository.codehaus.org"
		//mavenRepo "http://repository.codehaus.org"
		//mavenRepo "http://download.java.net/maven/2/"
		//mavenRepo "http://repository.jboss.com/maven2/"
		// For ImageIO:
		mavenRepo "http://maven-proxy.mitre.org/artifactory/ext-releases-local"
	}
	dependencies {
		// specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

		// runtime 'mysql:mysql-connector-java:5.1.20'
		compile ('org.mitre.eyesfirst:eyesfirst-common:0.0.4-SNAPSHOT') { changing=true; excludes "slf4j-log4j12" }
		runtime 'com.sun.media:jai_imageio:1.1'
		// This is really only needed for the development build, but whatever
		// See http://jira.grails.org/browse/GRAILS-9154 for why I don't bother
		// trying to make this environment-specific
		runtime 'mysql:mysql-connector-java:5.1.24'
	}

	plugins {
		runtime ":hibernate:$grailsVersion"
		runtime ":jquery:1.8.3"
		runtime ":resources:1.1.6"
		runtime ":jquery-ui:1.8.24"
		runtime("org.mitre:openid-connect:0.1.1") { changing=true }
		
		// Uncomment these (or add new ones) to enable additional resources capabilities
		runtime ":lesscss-resources:1.3.0.3"
		//runtime ":zipped-resources:1.0"
		//runtime ":cached-resources:1.0"
		//runtime ":yui-minify-resources:0.1.4"

		build ":tomcat:$grailsVersion"

		runtime ":database-migration:1.1"

		compile ':cache:1.0.1'
	}
}
