<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--@elvariable id="user" type="org.neo4j.cineasts.domain.User"--%>
<%--@elvariable id="recommendations" type="java.util.List<org.neo4j.cineasts.domain.MovieRecommendation>"--%>
<%--@elvariable id="recommendation" type="org.neo4j.cineasts.domain.MovieRecommendation"--%>
<html>
  <head>
    <title>Profile</title>
  </head>
  <body>

    <div class="span-5">
      <div class="profile-header">
        <div class="profile-image"><img src="<c:url value="/images/profile-placeholder.png" />" /></div>
        <div class="profile-header-details">          
          <h2>Hi ${user.name}!</h2>
          <p>${user.info}</p>
        </div>
        <div class="break"></div>
      </div>
      
      <c:choose>
        <c:when test="${not empty user.friends}">
          <h2>Your friends</h2>
          <ul class="friends-list">
            <c:forEach items="${user.friends}" var="friend">
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
          <h2>You don't have any friends yet</h2>
        </c:otherwise>
      </c:choose>
    </div>
    <div class="span-7 last">
      <div class="profile-feed">
        <div class="span-third">
          <h2>Your rated movies</h2>
        </div>
          <c:set var="ratings" value="${user.ratings}"/>
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
                      You have not rated any movies.
                  </c:otherwise>
              </c:choose>
          </ul>
        <div class="break"></div>

        <div class="span-third">
          <h2>Your recommendations</h2>
        </div>
        <div class="span-third last">
          <h2>${fn:length(recommendations)}</h2>
        </div>
          <ul class="rated-movies-list span-all last">
              <c:choose>
                  <c:when test="${not empty recommendations}">
                      <c:forEach items="${recommendations}" var="recommendation">
                          <c:set var="movie" value="${recommendation.movie}"/>
                          <c:set var="stars" value="${recommendation.rating}"/>
                          <li>
                              <h4><a href="<c:url value="/movies/${movie.id}" />"><c:out value="${movie.title}"/>
                                  (${movie.year}) - &quot;${movie.tagline}&quot;</a>
                              <img class="rating" src="/images/rated_${stars}.png" alt="${stars} stars"/>
                              </h4>
                          </li>
                      </c:forEach>
                  </c:when>
                  <c:otherwise>
                      There are no recommendations for you, perhaps you have to add some friends?
                  </c:otherwise>
              </c:choose>
          </ul>
        <div class="break"></div>
      </div>
    </div>

  </body>
</html>
