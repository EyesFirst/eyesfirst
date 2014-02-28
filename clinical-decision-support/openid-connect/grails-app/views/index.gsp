<!DOCTYPE html>

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
--%>

<html>
<head><title>OpenID Connect Test</title></head>
<body>
<h1>OpenID Connect Test Page</h1>
<p>This is the test home page for the OpenID Connect plugin. It is not secured
and therefore not required for anything.</p>
<ul>
<openid:loggedOut><li><g:link controller="login" action="auth">Login</g:link></li></openid:loggedOut>
<openid:loggedIn><li><g:link controller="login" action="logout">Logout <openid:userName/></g:link></li></openid:loggedIn>
<li><g:link controller="debug" action="secured">View secured page</g:link></li>
</ul>
</body>
</html>