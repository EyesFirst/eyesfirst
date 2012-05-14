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