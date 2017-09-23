<%@page import="java.util.UUID"%>
<%@page import="java.util.Collection"%>
<%@page import="java.util.Collections"%>
<%@page session="false"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"%><!DOCTYPE html>
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="keywords" content="online, copy-paste, clipboard"> 
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>CrossClipboard: Login</title>
<style type="text/css">
text {
    width: 100%;
    padding: 5px;
}

#wrapper {
	width: 90%;
	max-width: 420px;
	margin: 5% auto 15px auto;
}

#loginForm{
	width: 100%;
	max-width: 350px;
	text-align: center;
	margin: 0 auto;
}

input.login {
  display: block;
  width: 95%;
  height: 30px;
  margin: 5px auto;
  padding: 2px 5px;
}

.btn {
  padding: 10px 30px;
  background-color: #600;
  color: white;
  border: 0px;
  margin: 5px;
  border-radius: 5px;
}

.btn:hover {
  background-color: #e00;
}

.error {
  color: #990000;
  text-align: center;
  margin-bottom: 20px;
}
input::-webkit-input-placeholder {
    font-style: italic;
}
input:-moz-placeholder {
    font-style: italic;
}
</style>
</head>
<body style="margin: 0px; font-family: Arial">
  <%
  String captchaId = UUID.randomUUID().toString();
  %>
  <div id="wrapper">
    <%
    Collection<String> errors = (Collection<String>) request.getAttribute("errors");
    if (errors == null) {
      errors = Collections.emptyList();
    }
    for (String error : errors) {
  %>
    <div class="error"><%=error%></div>
    <%
    }
  %>
	<form id="loginForm" method="post" action="">
		<input name="login" type="text" id="login" placeholder="login" required="" autofocus="" autocapitalize="off" autocorrect="off" class="login"> 
		<input name="psw" type="password" id="password" placeholder="password" required="" autocapitalize="off" autocorrect="off" class="login"> 
		<img src="?captcha&id=<%= captchaId %>">
		<input name="captcha" type="text" value="" placeholder="captcha" required="required" autocapitalize="off" autocorrect="off" autocomplete="off" class="login">
		<input class="btn" type="submit" value="enter">
		<input type="hidden" name="action" value="login">
		<input type="hidden" name="captchaId" value="<%= captchaId %>">
	</form>
    <div>
      <p style="font-weight: 100;"><strong>CrossClipboard</strong> is an online copy-paste tool</p>
      <ul style="font-size: 0.8em">
	      <li>easily exchange any moderately-sized text between your devices</li>
	      <li>text size limit is 64KB</li>
	      <li>the text is guaranteed to be stored for one month, maybe more</li>
	      <li>there is no registration: enter with any login and password...</li>
	      <li>...just make sure to enter them correctly in all browsers</li>
      </ul>
    </div>
  </div>
  </div>
</body>
</html>