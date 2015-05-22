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
// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
	all:           '*/*',
	atom:          'application/atom+xml',
	css:           'text/css',
	csv:           'text/csv',
	form:          'application/x-www-form-urlencoded',
	html:          ['text/html','application/xhtml+xml'],
	js:            'text/javascript',
	json:          ['application/json', 'text/json'],
	multipartForm: 'multipart/form-data',
	rss:           'application/rss+xml',
	text:          'text/plain',
	xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// The default codec used to encode data with ${}
grails.views.default.codec = "html" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

eyesfirst {
	// URL for accessing WADO
	wadoURL = 'http://localhost:8888/wado'
	// Note: When using SSL, you need to provide the server certificate:
	// wadoCertificate {
	//    file = '/WEB-INF/wado.jks'
	//    key = 'key'
	//    password = 'password'
	// }
	// URL for accessing the EFID service.
	efidService = 'http://localhost:8080/efid/efid'
	// Note: When using SSL, you need to provide the server certificate:
	// efidServiceCertificate {
	//    file = '/WEB-INF/efid.jks'
	//    key = 'key'
	//    password = 'password'
	// }
	imageProcessorService = 'http://localhost:8080/image-processor-webapp/process/start'
	// Note: When using SSL, you need to provide the server certificate:
	// imageProcessorCertificate {
	//    file = '/WEB-INF/image-processor.jks'
	//    key = 'key'
	//    password = 'password'
	// }
	dcm4cheeHost = 'localhost'
	dcm4cheePort = 11112
}

environments {
	development {
		grails.logging.jul.usebridge = true
		grails.converters.default.pretty.print=true
		eyesfirst {
			loadSampleData = true
			efidService = 'http://localhost:8180/efid/efid'
			imageProcessorService = 'http://localhost:8280/image-processor-webapp/process/start'
		}
	}
	production {
		grails.logging.jul.usebridge = false
		grails.serverURL = "http://www.example.com"
		eyesfirst {
			// URL for accessing WADO
			wadoURL = 'http://pacs.example.com:8888/wado'
			// URL for accessing the EFID service.
			efidService = 'http://efid.example.com:8080/efid/efid'
			imageProcessorService = 'http://efid.example.com:8080/image-processor-webapp/process/start'
			dcm4cheeHost = 'pacs.example.com'
			dcm4cheePort = 11112
		}
	}
}

// log4j configuration
log4j = {
	// Example of changing the log pattern for the default console appender:
	//
	appenders {
		console name:'stdout', layout:pattern(conversionPattern: '%-6r [%t] %-5p %c %x - %m%n')
	}

	error  'org.codehaus.groovy.grails.web.servlet',        // controllers
		   'org.codehaus.groovy.grails.web.pages',          // GSP
		   'org.codehaus.groovy.grails.web.sitemesh',       // layouts
		   'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
		   'org.codehaus.groovy.grails.web.mapping',        // URL mapping
		   'org.codehaus.groovy.grails.commons',            // core / classloading
		   'org.codehaus.groovy.grails.plugins',            // plugins
		   'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
		   'org.springframework',
		   'org.hibernate',
		   'net.sf.ehcache.hibernate'

	//debug 'org.hibernate.SQL' // Show Hibernate SQL queries
	all 'org.eyesfirst'
	all 'org.mitre'
	all 'grails.app.controllers.org.eyesfirst'
	//all 'grails.app'
	//all 'grails.app.conf.BootStrap' <-- According to the documentation, that is supposed to be the bootstrap logger
	all 'BootStrap' // <- That is ACTUALLY the bootstrap logger. Good going, Grails.
}

security {
	openid {
		appName = "EyesFirst"
		// The default role for all authenticated users
		defaultRole = "ROLE_USER"
		issuers = [
			// Give all users authenticated by "https://eyesfirst.example.com/connect/" the "ROLE_CLINICIAN" role
			"https://eyesfirst.example.org/connect/": "ROLE_CLINICIAN"
		]
	}
}
// Added by the Spring Security Core plugin:
grails.plugins.springsecurity.userLookup.userDomainClassName = 'org.eyesfirst.User'
grails.plugins.springsecurity.userLookup.authorityJoinClassName = 'org.eyesfirst.UserRole'
grails.plugins.springsecurity.authority.className = 'org.eyesfirst.Role'
