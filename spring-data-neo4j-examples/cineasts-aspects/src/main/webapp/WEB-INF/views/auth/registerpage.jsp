<%@ page import="org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>Login</title>
	</head>
	<body>
	
		<h1>Login</h1>
		<div class="error">${error}</div>
		
		<form action="/auth/register" method="post" >
      <p>
        <label for="j_username">Login:</label>
        <input id="j_username" name="j_username" type="text" value="${j_username}"/>
      </p>
      <p>
        <label for="j_displayname">Name:</label>
        <input id="j_displayname" name="j_displayname" type="text" value="${j_displayname}"/>
      </p>
      <p>
        <label for="j_password">Password:</label>
        <input id="j_password" name="j_password" type="password" />
      </p>
      <input  type="submit" value="Register"/>
    </form>
    <br/>
    <a href="/auth/login">Login</a>
	</body>
</html>
