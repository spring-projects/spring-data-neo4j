<%@ page session="false" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<h2>Results for importing movies: &quot;${ids}&quot;</h2>

<p>Imported of ${fn:length(movies)} movies took ${duration} seconds.</p>

<dl>
    <c:forEach items="${movies}" var="entry">
        <dt>${entry.key}</dt>
        <dd>${entry.value}</dd>
    </c:forEach>
</dl>
