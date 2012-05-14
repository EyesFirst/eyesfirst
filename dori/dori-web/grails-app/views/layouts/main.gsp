<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <title><g:layoutTitle default="Grails"/></title>
    <meta http-equiv="Content-Script-Type" content="text/javascript"/>
    <meta http-equiv="Content-Style-Type" content="text/css" />
    <link rel="stylesheet" href="${resource(dir: 'css', file: 'main.css')}"/>
    <link rel="shortcut icon" href="${resource(dir: 'images', file: 'favicon.ico')}" type="image/x-icon"/>
    <g:javascript library="jquery" plugin="jquery"/>
    <jqui:resources />
    <g:javascript library="application"/>
    <g:layoutHead/>
    
</head>
<body>
<div id="spinner" class="spinner" style="display:none;">
    <img src="${resource(dir: 'images', file: 'spinner.gif')}" alt="${message(code: 'spinner.alt', default: 'Loading...')}"/>
</div>
<div>
    <div id="logo"><a class="home" href="${createLink(uri: '/')}"><img src="${resource(dir:'images',file:'ef_logo_small.png')}" alt="EyesFirst" border="0" /></a></div>
    <sec:ifNotLoggedIn>
      <g:link controller="login" action="auth" params="['spring-security-redirect':'/doriweb/efid/']">Login</g:link>
    </sec:ifNotLoggedIn>
    <sec:ifLoggedIn>
      <g:link controller="logout">Logout</g:link>
    </sec:ifLoggedIn>
</div>

<g:layoutBody/>
</body>
</html>