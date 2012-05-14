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

import grails.plugins.springsecurity.ui.RegisterCommand
import grails.plugins.springsecurity.ui.ResetPasswordCommand

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.plugins.springsecurity.ui.RegistrationCode


class RegisterController extends grails.plugins.springsecurity.ui.RegisterController {

	def springSecurityService

	//overridden to not do salting in the controller (already handled in domain object)
	def register = { RegisterCommand command ->

		if (command.hasErrors()) {
			render view: 'index', model: [command: command]
			return
		}

		def user = lookupUserClass().newInstance(email: command.email, username: command.username,
				password: command.password, accountLocked: true, enabled: true)
		if (!user.validate() || !user.save()) {
			// TODO
		}

		def registrationCode = new RegistrationCode(username: user.username).save()
		String url = generateLink('verifyRegistration', [t: registrationCode.token])

		def conf = SpringSecurityUtils.securityConfig
		def body = conf.ui.register.emailBody
		if (body.contains('$')) {
			body = evaluate(body, [user: user, url: url])
		}
		mailService.sendMail {
			to command.email
			from conf.ui.register.emailFrom
			subject conf.ui.register.emailSubject
			html body.toString()
		}

		render view: 'index', model: [emailSent: true]
	}

	//overridden to not do salting in the controller (already handled in domain object)
	def resetPassword = { ResetPasswordCommand command ->

		String token = params.t

		def registrationCode = token ? RegistrationCode.findByToken(token) : null
		if (!registrationCode) {
			flash.error = message(code: 'spring.security.ui.resetPassword.badCode')
			redirect uri: SpringSecurityUtils.securityConfig.successHandler.defaultTargetUrl
			return
		}

		if (!request.post) {
			return [token: token, command: new ResetPasswordCommand()]
		}

		command.username = registrationCode.username
		command.validate()

		if (command.hasErrors()) {
			return [token: token, command: command]
		}

		RegistrationCode.withTransaction { status ->
			def user = lookupUserClass().findByUsername(registrationCode.username)
			user.password = command.password
			user.save()
			registrationCode.delete()
		}

		springSecurityService.reauthenticate registrationCode.username

		flash.message = message(code: 'spring.security.ui.resetPassword.success')

		def conf = SpringSecurityUtils.securityConfig
		String postResetUrl = conf.ui.register.postResetUrl ?: conf.successHandler.defaultTargetUrl
		redirect uri: postResetUrl
	}

	def updatePassword = { ResetPasswordCommand command ->

		String oldPassword = params.oldPassword
		String password = command.password
		String password2 = command.password2

		User user = springSecurityService.getCurrentUser()
		String username = user.username
		if (!(user.password == springSecurityService.encodePassword(oldPassword, username))) {
			flash.message = 'Current password is incorrect'
			render view: 'changePassword'
			return
		}

		if (command.hasErrors()) {

			if(command.errors.hasFieldErrors("username")) {
				flash.message =  "Username and password cannot match"
			} else if(command.errors.hasFieldErrors("password")) {
				flash.message = "Password must be at least 8 characters long and contain at least one letter, number, and symbol"
			} else if(command.errors.hasFieldErrors("password2")) {
				flash.message = "Passwords do not match"
			} else {
				flash.message = "Unknown error occurred"
			}
			render view: 'changePassword'
			return
		}

		if (user.password == springSecurityService.encodePassword(password, username)) {
			flash.message = 'Please choose a different password from your current one'
			render view: 'changePassword'
			return
		}

		user.password = password
		user.passwordExpired = false
		user.save() // if you have password constraints check them here

		redirect controller: 'login', action: 'auth'
	}

	def changePassword = {
		if(springSecurityService.isLoggedIn()) {
			render view: "changePassword"
		} else {
			redirect controller: "login"
		}

	}
}
