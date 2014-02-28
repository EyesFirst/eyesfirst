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

import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.mitre.openid.connect.client.SubjectIssuerGrantedAuthority
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper

/**
 * Maps authorities based on the current Grails configurtion. This is a very
 * simple method of granting roles. A "real" application would likely wish to
 * do this using domain objects.
 * @author dpotter
 */
class GrailsConfigAuthorityMapper implements GrantedAuthoritiesMapper {
	private static final log = LogFactory.getLog(this)
	def grailsApplication
	SimpleGrantedAuthority defaultRole
	/**
	 * A map of issuers to the roles granted to a given issuer.
	 */
	def issuersToRoles = [:]
	def usersToRoles = [:]

	public GrailsConfigAuthorityMapper() {
	}

	public void setApplication(GrailsApplication application) {
		grailsApplication = application
		// If there is a generic role in the configuration for this application, create it
		def role = grailsApplication.config.security?.openid?.defaultRole
		defaultRole = role ? new SimpleGrantedAuthority(role) : null
		// Pull issuer/user roles out of the configuration
		issuersToRoles = generateRoleMap(grailsApplication.config.security?.openid?.issuers)
		usersToRoles = generateRoleMap(grailsApplication.config.security?.openid?.users)
	}

	private def generateRoleMap(def config) {
		def result = [:]
		if (config) {
			config.each { key, roles ->
				// Generate the GrantedAuthorities now
				def gas = []
				if (roles instanceof List) {
					roles.each { r ->
						gas.add(new SimpleGrantedAuthority(r))
					}
				} else {
					gas.add(new SimpleGrantedAuthority(roles))
				}
				result.put(key, gas);
			}
		}
		return result
	}

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(
			Collection<? extends GrantedAuthority> authorities) {
		Set<GrantedAuthority> result = new HashSet<GrantedAuthority>()
		result.addAll(authorities)
		for (GrantedAuthority authority : authorities) {
			if (authority instanceof SubjectIssuerGrantedAuthority) {
				def sigAuth = (SubjectIssuerGrantedAuthority) authority
				log.info("Adding authorities for user with issuer " + sigAuth.issuer + " with authority " + sigAuth.authority)
				def roles = issuersToRoles.get(sigAuth.issuer)
				if (roles) {
					//System.out.println("Adding roles based on issuer: " + roles);
					result.addAll(roles)
				}
				roles = usersToRoles.get(sigAuth.authority)
				if (roles) {
					//System.out.println("Adding roles based on authority: " + roles);
					result.addAll(roles)
				}
			}
		}
		// Always add the default role, if there is one
		if (defaultRole != null)
			result.add(defaultRole)
		return result
	}

}
