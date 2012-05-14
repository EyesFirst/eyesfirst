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

class UserController extends grails.plugins.springsecurity.ui.UserController {

	//overridden to not do salting in the controller (already handled in domain object)
	def save = {
		def user = lookupUserClass().newInstance(params)
		if (!user.save(flush: true)) {
			render view: 'create', model: [user: user, authorityList: sortedRoles()]
			return
		}

		addRoles(user)
		flash.message = "${message(code: 'default.created.message', args: [message(code: 'user.label', default: 'User'), user.id])}"
		redirect action: edit, id: user.id
	}

	//overridden to not do salting in the controller (already handled in domain object)
	def update = {
		def user = findById()
		if (!user) return
			if (!versionCheck('user.label', 'User', user, [user: user])) {
				return
			}

		user.properties = params

		if (!user.save()) {
			render view: 'edit', model: buildUserModel(user)
			return
		}

		lookupUserRoleClass().removeAll user
		addRoles user
		userCache.removeUserFromCache user.username
		flash.message = "${message(code: 'default.updated.message', args: [message(code: 'user.label', default: 'User'), user.id])}"
		redirect action: edit, id: user.id
	}
}
