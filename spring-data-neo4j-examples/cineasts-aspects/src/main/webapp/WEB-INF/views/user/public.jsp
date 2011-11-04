<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--@elvariable id="user" type="org.neo4j.cineasts.domain.User"--%>
<c:set var="name" value="${profiled.name}"/>
<html>
  <head><title>Profile for ${name}</title></head>
  <body>

    <div class="span-5">
      <div class="profile-header">
        <div class="profile-image"><img src="<c:url value="/images/profile-placeholder.png" />" /></div>
        <div class="profile-header-details">          
          <h2>${name}</h2>
          <p>${profiled.info}</p>
        </div>
        <div class="break"></div>
      </div>

      <h2>${name}'s friends</h2>
      <c:choose>
        <c:when test="${isFriend}">
          <c:set var="friends" value="${profiled.friends}"/>
          <c:choose>
            <c:when test="${not empty friends}">
              <ul class="friends-list">
                <c:forEach items="${friends}" var="friend">
                  <li>
                    <a class="friend-image" href="<c:url value="/user/${friend.login}" />"><img src="<c:url value="/images/profile-placeholder-small.png" />" /></a>
                    <div class="friend-info">                    
                      <h3><a href="<c:url value="/user/${friend.login}" />"><c:out value="${friend.name}"/></a></h3>
                      <p>${friend.info}</p>
                    </div>
                    <div class="break"></div>
                  </li>
                </c:forEach>
              </ul>
            </c:when>
            <c:otherwise>
              ${name} has no friends.
            </c:otherwise>
          </c:choose>
        </c:when>
        <c:otherwise>
		  <form id="add_friend" action="/user/${profiled.login}/friends" method="post">
          	<a href="#" onClick="document.getElementById('add_friend').submit();return false;">Add ${name} as a friend.</a>
		  </form>
        </c:otherwise>
      </c:choose>
    </div>
    <c:set var="ratings" value="${profiled.ratings}"/>
    <div class="span-7 last">
      <div class="profile-feed">
        <div class="span-third">
          <h2>${name}'s rated movies</h2>
        </div>
        <div class="span-third last">
          <h2>${fn:length(ratings)}</h2>
        </div>


        <ul class="rated-movies-list span-all last">
            <c:choose>
                <c:when test="${not empty ratings}">
                    <c:forEach items="${ratings}" var="rating">
                        <c:set var="movie" value="${rating.movie}"/>
                        <c:set var="stars" value="${rating.stars}"/>
                        <li>
                            <h4><a href="<c:url value="/movies/${movie.id}" />"><c:out value="${movie.title}"/>
                                (${movie.year}) - &quot;${rating.comment}&quot;</a>
                            <img class="rating" src="/images/rated_${stars}.png" alt="${stars} stars"/>
                            </h4>
                        </li>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    ${name} has not rated any movies.
                </c:otherwise>
            </c:choose>
        </ul>
        <div class="break"></div>
      </div>
    </div>
  </body>
</html>
