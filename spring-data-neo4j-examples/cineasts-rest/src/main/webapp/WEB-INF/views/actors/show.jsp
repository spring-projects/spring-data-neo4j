<%@ page session="false" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--@elvariable id="actor" type="org.neo4j.cineasts.domain.Actor"--%>
<c:choose>
    <c:when test="${actor != null}">
      <div class="span-4">
        <div class="actor-framed-image" style="background-image:url(${actor.profileImageUrl})">
        </div>
        <div class="actor-sidebar">  
          <h2>${actor.name}</h2>
          <p>Born <fmt:formatDate value="${actor.birthday}" pattern="yyyy/dd/MM"/> in ${actor.birthplace}.</p>
        </div>
        <div class="break"></div>
      </div>
      <div class="span-8 last">
        <h2>Biography</h2>
        <p>${actor.biography}</p>
        <h2>Roles</h2>
        <c:if test="${not empty actor.roles}">
          <ul>
            <c:forEach items="${actor.roles}" var="role">
              <li>
                <a href="/movies/${role.movie.id}"><c:out value="${role.movie.title}" /> as <c:out value="${role.name}" /> in <c:out value="${role.movie.year}"/></a><br/>
              </li>
            </c:forEach>
          </ul>
        </c:if>
      </div>
    </c:when>
    <c:otherwise>
      <h2>Actor cannot be found.</h2>
    </c:otherwise>
</c:choose>
