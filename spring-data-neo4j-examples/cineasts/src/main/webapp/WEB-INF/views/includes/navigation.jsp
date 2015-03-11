<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="navigation">
    <c:choose>
        <c:when test="${empty user}">
            <a href="<c:url value="/auth/login" />">Login</a>
            <a href="<c:url value="/auth/registerpage" />">Register</a>
        </c:when>
        <c:otherwise>
            <a href="<c:url value="/user" />">${user.name}</a>
            <a href="#" onclick="document.logout.submit();return false;">Logout</a>
            <form name="logout" action="/j_spring_security_logout" method="post">
              <input type="submit" value="Log out" />
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            </form>
        </c:otherwise>
    </c:choose>
</div>
