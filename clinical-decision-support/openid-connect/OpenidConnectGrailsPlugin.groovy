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
import org.mitre.openid.grails.OpenIDSecurityUtils;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.web.filter.DelegatingFilterProxy;

class OpenidConnectGrailsPlugin {
    // the plugin version
    def version = "0.1.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
    // resources that are excluded from plugin packaging
	def pluginExcludes = [
		"grails-app/views/error.gsp",
		"grails-app/views/index.gsp",
		"grails-app/views/debug/*",
		// Don't include the debug controller - it's for debugging the plugin
		// and exposes configuration details.
		"grails-app/controllers/openid/connect/DebugController.groovy",
		// Don't include the domain classes, they're also for testing purposes
		"grails-app/domain/org/mitre/openid/*"
	]

    def title = "OpenID Connect Plugin" // Headline display name of the plugin
    def author = "Dan Potter"
    def authorEmail = "dpotter@mitre.org"
    def description = '''\
Provides very basic support for OpenID Connect using Spring Security
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/openid-connect"
	def groupId="org.mitre"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

	def getWebXmlFilterOrder() {
		def FilterManager = getClass().getClassLoader().loadClass('grails.plugin.webxml.FilterManager')
		[springSecurityFilterChain: FilterManager.GRAILS_WEB_REQUEST_POSITION + 100]
	}

	def doWithWebDescriptor = { xml ->
		// Inject the Spring Security filter

		// Add the filter after the last context-param
		def contextParam = xml.'context-param';

		contextParam[contextParam.size() - 1] + {
			'filter' {
				'filter-name'('springSecurityFilterChain')
				'filter-class'(DelegatingFilterProxy.name)
			}
		}

		// We need to add the filter mapping immediately after the
		// charEncodingFilter

		def filter = xml.'filter-mapping'.find {
			it.'filter-name'.text() == "charEncodingFilter"
		}
		
		filter + {
			'filter-mapping' {
				'filter-name'('springSecurityFilterChain')
				'url-pattern'('/*')
				'dispatcher'('ERROR')
				'dispatcher'('REQUEST')
			}
		}
		// The following is a complete hack and is designed to replace the much
		// more capable Grails Spring Security Core plugin's "secureChannel"
		// configuration. It will almost assuredly be removed at some point in
		// the future but for now, it works.
		OpenIDSecurityUtils.application = application
		if (OpenIDSecurityUtils.forceHTTPS) {
			def children = xml.children()
			children[children.size() - 1] + {
				'security-constraint' {
					'web-resource-collection' {
						'web-resource-name'('securedapp')
						'url-pattern'('/*')
					}
					'user-data-constraint' {
						'transport-guarantee'('CONFIDENTIAL')
					}
				}
			}
		}
	}

	def doWithSpring = {
		println '\nConfiguring OpenID Connect...'
		OpenIDSecurityUtils.application = application

		xmlns context:"http://www.springframework.org/schema/context"
		xmlns security:"http://www.springframework.org/schema/security"

		context.'component-scan'('base-package':'org.mitre.web')
		security.'global-method-security'(
			'pre-post-annotations':"enabled",
			'proxy-target-class':"true",
			'authentication-manager-ref':"authenticationManager"
		)
		security.'http'(
			'auto-config':false,
			'use-expression':true,
			'disable-url-rewriting':true,
			'entry-point-ref':"authenticationEntryPoint",
			'pattern':"/**") {
			security.'custom-filter'('before':"PRE_AUTH_FILTER", 'ref':"openIdConnectAuthenticationFilter")
			security.'logout'
		}

		authenticationEntryPoint(org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint, "/login/auth") {
		}
/*
		exceptionTranslationFilter(org.springframework.security.web.access.ExceptionTranslationFilter, authenticationEntryPoint) {
			accessDeniedHandler = ref("accessDeniedHandler")
		}

		accessDeniedHandler(org.mitre.openid.grails.OpenIDAccessDeniedHandler) {
			//errorPage="/login/denied"
		}*/

		security.'authentication-manager'(alias:"authenticationManager") {
			security.'authentication-provider'(ref:"openIdConnectAuthenticationProvider")
		}

		def configgedAuthoritiesMapper = OpenIDSecurityUtils.openIDConfig.authoritiesMapper
		if (configgedAuthoritiesMapper == 'domain') {
			openIdConnectAuthenticationProvider(org.mitre.openid.connect.client.OIDCAuthenticationProvider) {
				authoritiesMapper = { org.mitre.openid.grails.DomainAuthoritiesMapper m ->
					application = application
				}
			}
		} else {
			openIdConnectAuthenticationProvider(org.mitre.openid.connect.client.OIDCAuthenticationProvider) {
				authoritiesMapper = { org.mitre.openid.grails.GrailsConfigAuthorityMapper m ->
					application = application
				}
			}
		}

		openIdConnectAuthenticationFilter(org.mitre.openid.connect.client.OIDCAuthenticationFilter) {
			authenticationManager = ref("authenticationManager")
			serverConfigurationService = { org.mitre.openid.connect.client.service.impl.DynamicServerConfigurationService s ->
			}
			clientConfigurationService = { org.mitre.openid.connect.client.service.impl.DynamicRegistrationClientConfigurationService s ->
				template = { org.mitre.oauth2.model.RegisteredClient c ->
					clientName = OpenIDSecurityUtils.appName
					scope = [ "openid", "email", "address", "profile", "phone" ]
					tokenEndpointAuthMethod = "SECRET_BASIC"
					redirectUris = OpenIDSecurityUtils.redirectUris
				}
			}
			issuerService = { org.mitre.openid.connect.client.service.impl.WebfingerIssuerService s ->
				loginPageUrl = "login"
			}
			authRequestUrlBuilder = { org.mitre.openid.connect.client.service.impl.PlainAuthRequestUrlBuilder b ->
			}
		}

		validatorCache(org.mitre.jwt.signer.service.impl.JWKSetSigningAndValidationServiceCacheService)

		println '... finished configuring OpenID Connect\n'
	}

	def doWithDynamicMethods = { ctx ->
		// TODO Implement registering dynamic methods to classes (optional)
	}

	def doWithApplicationContext = { applicationContext ->
		/*
		 * This is debug crap to try and figure out why the exception handler
		 * never receives the exception but isn't really needed
		def filterChain = applicationContext.springSecurityFilterChain
		System.out.println(filterChain.filterChainMap)
		// Grab the exception translation handler and dump it
		filterChain.filterChainMap.each { k, v->
			System.out.println("Under " + k + ":");
			v.collect { filter ->
				if (filter instanceof ExceptionTranslationFilter) {
					System.out.println("-- " + filter);
					System.out.println("AuthenticationEntryPoint: " + filter.authenticationEntryPoint);
					System.out.println("URL: " + filter.authenticationEntryPoint.loginFormUrl);
					filter = new OpenIDExceptionTranslationFilter(filter.authenticationEntryPoint)
				}
				filter
			}
		}
		*/
		// TODO Implement post initialization spring config (optional)
	}

	def onChange = { event ->
		// TODO Implement code that is executed when any artefact that this plugin is
		// watching is modified and reloaded. The event contains: event.source,
		// event.application, event.manager, event.ctx, and event.plugin.
	}

	def onConfigChange = { event ->
		// TODO Implement code that is executed when the project configuration changes.
		// The event is the same as for 'onChange'.
	}

	def onShutdown = { event ->
		// TODO Implement code that is executed when the application shuts down (optional)
	}
}
