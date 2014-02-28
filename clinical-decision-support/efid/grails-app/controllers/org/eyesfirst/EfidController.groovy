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
package org.eyesfirst

import java.security.SecureRandom

import javax.servlet.http.HttpServletResponse

import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit
import org.springframework.dao.DataIntegrityViolationException

class EfidController {

	static allowedMethods = [create: "POST", delete: "POST"]

	// Sooooooo......
	static final CHECKDIGIT = new LuhnCheckDigit()
	static final EFID_PTN = ~/EF[0-9A-F]{8}[0-9]/

	SecureRandom secureRandom //dependency injected

	//I only want scaffolding for list() & delete()
	static scaffold = Efid
	static Closure noAction = { response.sendError HttpServletResponse.SC_NOT_FOUND }
	def edit = noAction;
	def save = noAction;
	def create = noAction;
	def update = noAction;

	//---Actions

	def list = {
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		[efidInstanceList: Efid.list(params), efidInstanceTotal: Efid.count()]
	}

	def issue() {
		if (request.method == 'POST') {
			// FIXME: For now, just pull the first issuer
			EfidIssuer issuer = EfidIssuer.list().get(0)
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
	}

	private String generateNewEfid() {
		int number = Math.abs(secureRandom.nextInt())
		// FIXME: We're calculating a Luhn check digit based on the decimal
		// representation of the number, and then converting the number to a set
		// of hex digits, thereby defeating the purpose of using a Luhn check
		// digit.
		String checkDigit = CHECKDIGIT.calculate("" + number)//FYI doesn't like negative numbers
		String efId = String.format('EF%08X', number) + checkDigit
		assert efId ==~ EFID_PTN
		return efId
	}

	def verify = {
		assert params.id
		def found = Efid.get(params.id) != null;
		response.status = found ? 200 : 404
		render found ? 'Verified' : 'Not Found'
	}

	def delete() {
		// Disallow for now
		response.sendError(HttpServletResponse.SC_NOT_FOUND)
	}
}
