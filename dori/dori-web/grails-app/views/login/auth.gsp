<!DOCTYPE html>
<%--
Copyright 2012 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<html>
<head>
<title><g:message code='spring.security.ui.login.title'/></title>
<meta name='layout' content='register'/>
</head>

<body>

<div class="login s2ui_center ui-corner-all" style='text-align:center;'>
	<div class="login-inner">
	<form action='${postUrl}' method='POST' id="loginForm" name="loginForm" autocomplete='off'>
	<div class="sign-in">

	<h1><g:message code='spring.security.ui.login.signin'/></h1>
    <input type="hidden" value="${ params['spring-security-redirect'] ?: '' }"></input>
	<table>
		<tr>
			<td><label for="username"><g:message code='spring.security.ui.login.username'/></label></td>
			<td><input name="j_username" id="username" size="15" /></td>
		</tr>
		<tr>
			<td><label for="password"><g:message code='spring.security.ui.login.password'/></label></td>
			<td><input type="password" name="j_password" id="password" size="15" /></td>
		</tr>
		<tr>
			<td colspan='2'>
				%{--<input type="checkbox" class="checkbox" name="${rememberMeParameter}" id="remember_me" checked="checked" />--}%
				%{--<label for='remember_me'><g:message code='spring.security.ui.login.rememberme'/></label> |--}%
				<span class="forgot-link">
					<g:link controller='register' action='forgotPassword'><g:message code='spring.security.ui.login.forgotPassword'/></g:link>
				</span>
			</td>
		</tr>
		<tr>
			<td colspan='2'>
				<s2ui:linkButton elementId='register' controller='register' messageCode='spring.security.ui.login.register'/>
				<s2ui:submitButton elementId='loginButton' form='loginForm' messageCode='spring.security.ui.login.login'/>
			</td>
		</tr>
	</table>

	</div>
	</form>
	</div>
</div>

<script>
$(document).ready(function() {
	$('#username').focus();
});

<s2ui:initCheckboxes/>

</script>

</body>
</html>