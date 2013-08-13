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
<title><g:message code='spring.security.ui.resetPassword.title'/></title>
<meta name='layout' content='register'/>
</head>

<body>

<p/>

<s2ui:form width='475' height='250' elementId='resetPasswordFormContainer'
           titleCode='spring.security.ui.resetPassword.header' center='true'>

	<g:form action='resetPassword' name='resetPasswordForm' autocomplete='off'>
	<g:hiddenField name='t' value='${token}'/>
	<div class="sign-in">

	<br/>
	<h4><g:message code='spring.security.ui.resetPassword.description'/></h4>

	<table>
		<s2ui:passwordFieldRow name='password' labelCode='resetPasswordCommand.password.label' bean="${command}"
                             labelCodeDefault='Password' value="${command?.password}"/>

		<s2ui:passwordFieldRow name='password2' labelCode='resetPasswordCommand.password2.label' bean="${command}"
                             labelCodeDefault='Password (again)' value="${command?.password2}"/>
	</table>

	<s2ui:submitButton elementId='reset' form='resetPasswordForm' messageCode='spring.security.ui.resetPassword.submit'/>

	</div>
	</g:form>

	</div>
</s2ui:form>

<script>
$(document).ready(function() {
	$('#password').focus();
});
</script>

</body>
</html>