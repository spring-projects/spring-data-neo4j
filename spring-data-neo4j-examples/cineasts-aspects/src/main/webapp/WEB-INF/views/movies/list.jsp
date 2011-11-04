<%@ page session="false" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<h2>Results for &quot;${query}&quot;</h2>

<c:choose>
  <c:when test="${not empty movies}">
    <ul class="search-results">
      <c:forEach items="${movies}" var="movie">
        <li>
          <div class="search-result-details">
          <c:set var="image" value="${movie.imageUrl}"/>
          <c:if test="${empty image}"><c:set var="image" value="/images/movie-placeholder.png"/></c:if>
          <a class="thumbnail" href="<c:url value="/movies/${movie.id}" />"> <img src="<c:url value="${image}" />" /></a>
            <a href="/movies/${movie.id}">${movie.title}</a> <img alt="${movie.stars} stars" src="/images/rated_${movie.stars}.png"/>
            <p><c:out value="${movie.tagline}" escapeXml="true" /></p>
          </div>
        </li>
      </c:forEach>
    </ul>
  </c:when>
  <c:otherwise>
    <h2>No movies found for query &quot;${query}&quot;.</h2>
  </c:otherwise>
</c:choose>
