<%@ include file="/jsp/include.jsp"%>
<%@ include file="/jsp/head.jsp"%>

<title><c:out value="${model.actorName}" /> : IMDB powered by Neo4j</title>
</head>
<body>
<h1><c:out value="${model.actorName}" /></h1>
<c:if test="${!empty model.kevinBaconNumber}">
	<p>Kevin Bacon number: <b><c:out
		value="${model.kevinBaconNumber}" /></b></p>
	<h3>Bacon path</h3>
	<ul>
		<c:forEach items="${model.baconPath}" var="pathElement"
			varStatus="row">
			<c:choose>
				<c:when test="${row.count % 2 == 0}">
					<c:set var="urlBase" value="movie.html" />
                    <c:set var="cssClass" value="movie" />
					<c:set var="paramName" value="title" />
				</c:when>
				<c:otherwise>
					<c:set var="urlBase" value="actor.html" />
                    <c:set var="cssClass" value="actor" />
					<c:set var="paramName" value="name" />
				</c:otherwise>
			</c:choose>
			<c:url value="${urlBase}" var="pathElementURL">
				<c:param name="${paramName}" value="${pathElement}" />
			</c:url>
			<li class="${cssClass}"><a href='<c:out value="${pathElementURL}"/>'>${pathElement}</a></li>
		</c:forEach>
	</ul>
	<h3>Movies</h3>
	<ul class="movies">
		<c:forEach items="${model.movieInfo}" var="movieInfo">
			<c:url value="movie.html" var="movieURL">
				<c:param name="title" value="${movieInfo.title}" />
			</c:url>
			<li class="movie"><em><c:out value="${movieInfo.role}" /></em> in <a
				href='<c:out value="${movieURL}"/>'><c:out
				value="${movieInfo.title}" /></a></li>
		</c:forEach>
	</ul>
</c:if>
<%@ include file="/jsp/menu.jsp"%>
</body>
</html>
