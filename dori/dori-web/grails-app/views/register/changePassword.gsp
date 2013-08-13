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
    <title>DORI</title>
    <meta name='layout' content='register'/>
</head>
<body>
<div id='login'>
   <div class='inner'>
      <g:if test='${flash.message}'>
      <div class='login_message'>${flash.message}</div>
      </g:if>
      <div class='fheader'>Please update your password: </div>
      <g:form action='updatePassword' id='passwordResetForm' class='cssform' autocomplete='off'>
         <p>
            <label for='password'>Current Password</label>
            <g:passwordField name='oldPassword' class='text_' />
         </p>
         <p>
            <label for='password'>New Password</label>
            <g:passwordField name='password' class='text_' />
         </p>
         <p>
            <label for='password'>New Password (again)</label>
            <g:passwordField name='password2' class='text_' />
         </p>
         <p>
            <input type='submit' value='Reset' />
         </p>
      </g:form>
   </div>
</div>
</body>
</html>