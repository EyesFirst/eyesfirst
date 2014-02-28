<%--

Copyright 2013 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

--%><%@ page import="org.eyesfirst.Efid" %>



<div class="fieldcontain ${hasErrors(bean: efidInstance, field: 'issuer', 'error')} required">
	<label for="issuer">
		<g:message code="efid.issuer.label" default="Issuer" />
		<span class="required-indicator">*</span>
	</label>
	<g:select id="issuer" name="issuer.id" from="${org.eyesfirst.EfidIssuer.list()}" optionKey="id" required="" value="${efidInstance?.issuer?.id}" class="many-to-one"/>
</div>

