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
package org.eyesfirst.dori

import grails.plugins.springsecurity.Secured

import java.security.SecureRandom

import javax.servlet.http.HttpServletResponse

import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit
import org.springframework.dao.DataIntegrityViolationException

@Secured ('IS_AUTHENTICATED_ANONYMOUSLY')
class EfidController {

	static allowedMethods = [create: "POST", delete: "POST"]

	static final CHECKDIGIT = new LuhnCheckDigit()
	static final EFID_PTN = ~/EF[0-9A-F]{8}[0-9]/

	SecureRandom secureRandom //dependency injected
	def springSecurityService

	//I only want scaffolding for list() & delete()
	def scaffold = true
	static Closure noAction = { response.sendError HttpServletResponse.SC_NOT_FOUND }
	def edit = noAction;
	def save = noAction;
	def create = noAction;
	def update = noAction;

	//---Actions

	def index = {

	}

	@Secured (['ROLE_CREATE', 'ROLE_ADMIN'])
	def list = {
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		[efidInstanceList: Efid.list(params), efidInstanceTotal: Efid.count()]
	}

	@Secured ('ROLE_CREATE')
	def issue = {
		EfidIssuer issuer = springSecurityService.currentUser.efidIssuer
		if (issuer == null) {
			response.sendError HttpServletResponse.SC_UNAUTHORIZED, "User $springSecurityService.currentUser needs an issuer."
			return;
		}
		if (issuer.efids >= issuer.maxEfids) {
			response.sendError HttpServletResponse.SC_FORBIDDEN, "User $springSecurityService.currentUser issued all $issuer.maxEfids efids."
			return;
		}

		def efid = new Efid()
		efid.id = generateNewEfid()
		issuer.efids++
		issuer.addToEfidList(efid)
		issuer.save(failOnError: true);


		render efid.id
	}

	//TODO not an "action", a utility method
	String generateNewEfid() {
		int number = Math.abs(secureRandom.nextInt())
		String checkDigit = CHECKDIGIT.calculate("" + number)//FYI doesn't like negative numbers
		String efId = String.format('EF%08X', number) + checkDigit
		assert efId ==~ EFID_PTN
		return efId
	}

	def verify = {
		assert params.id
		response.status = Efid.get(params.id) ? 200 : 404
		render ''
	}

	@Secured ('ROLE_ADMIN')
	def delete = {
		def efidInstance = Efid.get(params.id)
		if (efidInstance) {
			try {
				efidInstance.delete(flush: true)
				flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'efid.label', default: 'Efid'), params.id])}"
				redirect(action: "list")
			}
			catch (org.springframework.dao.DataIntegrityViolationException e) {
				log.error(e.toString(), e)
				flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'efid.label', default: 'Efid'), params.id])}"
				redirect(action: "show", id: params.id)
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'efid.label', default: 'Efid'), params.id])}"
			redirect(action: "list")
		}
	}
}
