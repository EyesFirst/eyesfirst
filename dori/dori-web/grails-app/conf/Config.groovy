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
// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

grails.config.locations = [
	"classpath:eyesfirst-config.properties",
	"classpath:${appName}-config.properties",
	"classpath:${appName}-config.groovy",
	"file:${userHome}/.grails/${appName}-config.properties",
	"file:${userHome}/.grails/${appName}-config.groovy"
]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = 'org.eyesfirst.dori' // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [html: [
		'text/html',
		'application/xhtml+xml'
	],
	xml: [
		'text/xml',
		'application/xml'
	],
	text: 'text/plain',
	js: 'text/javascript',
	rss: 'application/rss+xml',
	atom: 'application/atom+xml',
	css: 'text/css',
	csv: 'text/csv',
	all: '*/*',
	json: [
		'application/json',
		'text/json'
	],
	form: 'application/x-www-form-urlencoded',
	multipartForm: 'multipart/form-data'
]

//EyesFirst custom

eyesfirst.solrURL = 'http://localhost:8080/solr/dicomobjects/ajaxsolr'
// I'd remove this entirely, but I can't:
eyesfirst.octScanViewerURL = '/doriweb/oct-scan-viewer'
eyesfirst.imageProcessorUrl = 'http://localhost:8080/image-processor-webapp/process/start'
eyesfirst.wadoURL = 'http://localhost:8888/wado'
eyesfirst.dcm4cheeHost = 'localhost'
eyesfirst.dcm4cheePort = 11112

environments {
	development {
		eyesfirst.solrURL = 'http://localhost:8983/solr/dicomobjects/ajaxsolr'
		eyesfirst.solrUpdate = 'http://localhost:8983/solr/dicomobjects/dataimport?command=full-import'
		eyesfirst.octScanViewerURL = 'http://localhost:8080/doriweb/oct-scan-viewer'
		eyesfirst.imageProcessorUrl = 'http://localhost:8081/image-processor-webapp/process/start'
	}
	demo {
		eyesfirst.solrURL = 'http://localhost:8080/solr/dicomobjects/ajaxsolr'
		eyesfirst.solrUpdate = 'http://localhost:8080/solr/dicomobjects/dataimport?command=full-import'
		eyesfirst.octScanViewerURL = 'http://localhost:8080/doriweb/oct-scan-viewer'
		eyesfirst.imageProcessorUrl = 'http://localhost:8080/image-processor-webapp/process/start'
	}
	production {
		eyesfirst.solrURL = 'http://eyesfirst.mitre.org/solr/dicomobjects/ajaxsolr'
		eyesfirst.solrUpdate = 'http://eyesfirst.mitre.org/solr/dicomobjects/dataimport?command=full-import'
		eyesfirst.octScanViewerURL = 'http://eyesfirst.mitre.org/doriweb/oct-scan-viewer'
		eyesfirst.imageProcessorUrl = 'http://localhost:8080/image-processor-webapp/process/start'
	}

	test { eyesfirst.imageProcessorUrl = 'http://localhost:8080/image-processor-webapp/process/start' }
}


// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// The default codec used to encode data with ${}
grails.views.default.codec = "html" // none, html, base64
grails.views.javascript.library="jquery"
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
// whether to install the java.util.logging bridge for sl4j. Disable for AppEngine!
grails.logging.jul.usebridge = false //DWS: prefer not to for jboss
// packages to include in Spring bean scanning
grails.spring.bean.packages = []

// set per-environment serverURL stem for creating absolute links
environments {
	production { grails.serverURL = "eyesfirst.mitre.org" }
	demo { grails.serverURL = "http://localhost:8080/${appName}"}
	development { grails.serverURL = "http://localhost:8080/${appName}" }
	test { grails.serverURL = "http://localhost:8080/${appName}" }

}

// log4j configuration
log4j = {

	info 'org.codehaus.groovy.grails.web.servlet',  //  controllers
			'org.codehaus.groovy.grails.web.pages', //  GSP
			'org.codehaus.groovy.grails.web.sitemesh', //  layouts
			'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
			'org.codehaus.groovy.grails.web.mapping', // URL mapping
			'org.codehaus.groovy.grails.commons', // core / classloading
			'org.codehaus.groovy.grails.plugins', // plugins
			'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
			'org.springframework',
			'org.hibernate',
			'net.sf.ehcache.hibernate'

	error 'org.mortbay.log'

	info 'grails.app.controller'

	info 'org.codehaus.groovy.grails.web.servlet'

	info 'org.springframework.security'

	info 'org.mitre.eyesfirst'
}

// Added by the Spring Security Core plugin:
grails.plugins.springsecurity.userLookup.userDomainClassName = 'org.eyesfirst.dori.User'
grails.plugins.springsecurity.userLookup.authorityJoinClassName = 'org.eyesfirst.dori.UserRole'
grails.plugins.springsecurity.authority.className = 'org.eyesfirst.dori.Role'
grails.plugins.springsecurity.dao.reflectionSaltSourceProperty = 'username'

//http://grails-plugins.github.com/grails-spring-security-core/docs/manual/guide/19%20Session%20Fixation%20Prevention.html
grails.plugins.springsecurity.useSessionFixationPrevention = true

//grails.plugins.springsecurity.successHandler.useReferer = true

//We have rules, including /** ADMIN_ROLE.
//grails.plugins.springsecurity.rejectIfNoRule = true

//If a controller action doesn't have @Secured annotations then these staticRules apply.
grails.plugins.springsecurity.controllerAnnotations.staticRules = [
			//          A white-list approach.  First rule found wins.
			'/':            [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],
			'/js/**':       [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],
			'/css/**':      [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],
			'/images/**':   [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],
			'/login/**':    [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],
			'/logout/**':   [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],
			'/register/**': [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],
			//concerns me a little, although only static web stuff seems to be here
			'/plugins/**':  [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],
			'/upload/processed': [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],

			'/securityInfo/currentAuth': [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],//diagnostics

			'/browser/**':  ['ROLE_BROWSE'],//the dori browser

			'/feeback/**': ['ROLE_BROWSE'],

			'/diagnosis/**': ['ROLE_BROWSE'],

			'/upload/applet/**': [
				'IS_AUTHENTICATED_ANONYMOUSLY'
			],

			'/**': ['ROLE_ADMIN']//default
		]

// *** email ***
// http://grails.org/plugin/mail
environments {
	test {
		grails.mail.disabled = true
	}
	development {
		grails.mail.host = 'mail.mitre.org'
	}
	//default assumes localhost:25
}
