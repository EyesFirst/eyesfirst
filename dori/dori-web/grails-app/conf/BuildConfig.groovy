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
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.war.file = "target/${appName}.war"

grails.project.dependency.resolution = {
	// inherit Grails' default dependencies
	inherits("global") {
		// uncomment to disable ehcache
		// excludes 'ehcache'
		// excludes 'httpclient'//we need to use v4.1.2 via httpproxy instead
	}
	//log "verbose"
	log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
	repositories {
		// Use local repositories first
		grailsHome()
		mavenLocal()
		grailsPlugins()
		grailsCentral()

		// uncomment the below to enable remote dependency resolution
		// from public Maven repositories
		mavenCentral()
		//mavenRepo "http://snapshots.repository.codehaus.org"
		//mavenRepo "http://repository.codehaus.org"
		//mavenRepo "http://download.java.net/maven/2/"
		//mavenRepo "http://repository.jboss.com/maven2/"
	}
	dependencies {
		// specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
		test 'org.apache.derby:derby:10.7.1.1'
		runtime 'mysql:mysql-connector-java:5.1.24'
		runtime 'org.apache.httpcomponents:httpclient:4.2.1'
		compile 'net.sourceforge.javacsv:javacsv:2.0'
		compile ('org.eyesfirst:eyesfirst-common:0.0.4-SNAPSHOT') { changing=true; excludes "slf4j-log4j12" }
	}
	plugins {
		// Mark this plugin as "provided" otherwise Grails will include the
		// Tomcat JAR files in the WAR file, and things will NOT work that way!
		provided ':tomcat:2.1.0'
	}
}

// Remove the jar before the war is bundled
//grails.war.resources = { stagingDir, args ->
//    delete {
//        fileset(dir:"${stagingDir}/WEB-INF/lib/", includes:"*slf4j* *log4j*")
//    }
//}
