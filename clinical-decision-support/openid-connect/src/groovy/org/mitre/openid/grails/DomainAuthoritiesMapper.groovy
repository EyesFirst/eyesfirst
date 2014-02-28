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
package org.mitre.openid.grails

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.mitre.openid.connect.client.SubjectIssuerGrantedAuthority;
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper

class DomainAuthoritiesMapper implements GrantedAuthoritiesMapper {
	private static final log = LogFactory.getLog(this)
	def grailsApplication
	SimpleGrantedAuthority defaultRole

	public DomainAuthoritiesMapper() {
		// does nothing
	}

	public void setApplication(GrailsApplication application) {
		grailsApplication = application
		// If there is a generic role in the configuration for this application, create it
		def role = grailsApplication.config.security?.openid?.defaultRole
		defaultRole = role ? new SimpleGrantedAuthority(role) : null
	}

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(
			Collection<? extends GrantedAuthority> authorities) {
		log.debug("Mapping authorities for user...")
		// Create our return value set
		Set<GrantedAuthority> result = new HashSet<GrantedAuthority>()
		result.addAll(authorities)
		// And go through the authorities, looking for a
		// SubjectIssuerGrantedAuthority which will determine any authorities
		// we add
		boolean foundAuthority = false
		for (GrantedAuthority authority : authorities) {
			if (authority instanceof SubjectIssuerGrantedAuthority) {
				foundAuthority = true
				def sigAuth = (SubjectIssuerGrantedAuthority) authority
				fetchUserAuthorities(sigAuth.issuer, sigAuth.subject).each {
					log.info("Adding authority " + it)
					result.add(new SimpleGrantedAuthority(it))
				}
			}
		}
		if (foundAuthority == false)
			log.warn("Did not add any authorities to this user - we never found a SubjectIssuerGrantedAuthority for it!")
		return result;
	}

	def fetchUser(String issuer, String subject) {
		def conf = OpenIDSecurityUtils.openIDConfig;
		String issuerClassName = conf.issuerLookup.issuerDomainClassName
		def issuerDC = grailsApplication.getDomainClass(issuerClassName)
		if (!issuerDC) {
			throw new RuntimeException("The issuer domain class " + issuerClassName + " is not a domain class")
		}
		Class<?> Issuer = issuerDC.clazz
		String userClassName = conf.userLookup.userDomainClassName
		def userDC = grailsApplication.getDomainClass(userClassName)
		if (!userDC) {
			throw new RuntimeException("The user domain class " + userClassName + " is not a domain class")
		}
		Class<?> User = userDC.clazz
		log.debug("Looking up issuer for \"" + issuer + "\"")
		Issuer.withTransaction {
			def issuerDO = Issuer.findWhere((conf.issuerLookup.issuerPropertyName): issuer)
			if (!issuerDO) {
				log.warn("Unable to find issuer for \"" + issuer + "\"")
				return null
			}
			// Pull the user out of the issuer
			def userDO = User.findWhere(issuer: issuerDO, (conf.userLookup.subjectPropertyName): subject)
			if (!userDO) {
				log.warn("Unable to find user \"" + subject + "\" for issuer \"" + issuer + "\"")
				return null
			}
			log.info("Found user \"" + subject + "\" for issuer \"" + issuer + "\"")
			return userDO;
		}
	}

	def fetchUserAuthorities(String issuer, String subject) {
		// We need to set up a transaction
		def conf = OpenIDSecurityUtils.openIDConfig;
		String issuerClassName = conf.issuerLookup.issuerDomainClassName
		def issuerDC = grailsApplication.getDomainClass(issuerClassName)
		if (!issuerDC) {
			throw new RuntimeException("The issuer domain class " + issuerClassName + " is not a domain class")
		}
		Class<?> Issuer = issuerDC.clazz
		Issuer.withTransaction {
			def user = fetchUser(issuer, subject)
			if (user == null) {
				return Collections.EMPTY_LIST
			} else {
				String authoritiesPropertyName = conf.userLookup.authoritiesPropertyName
				String authorityName = conf.authority.nameField
				return user."$authoritiesPropertyName".collect {
					it."$authorityName"
				}
			}
		};
	}
}
