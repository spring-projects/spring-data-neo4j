<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="navigation">
    <c:choose>
        <c:when test="${empty user}">
			<c:set var="last" value="Login"/>
        	<c:if test="${not empty param.login_error}">
				<c:set var="last" value="${sessionScope['SPRING_SECURITY_LAST_USERNAME']}"/>
        	</c:if>
			<form action="/j_spring_security_check" method="post" >
	        <input id="j_username" name="j_username" type="text" value="${last}"
				onfocus="this.value = (this.value=='Login') ? '' : this.value;" onblur="this.value = (this.value=='') ? '${last}' : this.value;"
			/>
        	<input id="j_password" name="j_password" type="password" onfocus="this.value = (this.value=='*******') ? '' : this.value;" onblur="this.value = (this.value=='') ? '*******' : this.value;" value="*******"/>
			<input type="image" src="/images/rating-active.png" value="Login" style="max-width:0px;max-height:0px;"/>
	    	</form>
        </c:when>
        <c:otherwise>
            <a href="<c:url value="/user" />">${user.name}</a>
            <a href="<c:url value="/auth/logout" />">Logout</a>
        </c:otherwise>
    </c:choose>
</div>
