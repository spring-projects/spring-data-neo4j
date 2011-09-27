<%@ include file="/jsp/include.jsp"%>
<%@ include file="/jsp/head.jsp"%>

<title><c:out value="${model.movieTitle}" /> : IMDB powered by Neo4j</title>
</head>
<body>
<h1><c:out value="${model.movieTitle}" /></h1>
<h3>Actors</h3>
<ul class="actors">
	<c:forEach items="${model.actorInfo}" var="actorInfo">
		<c:url value="actor.html" var="actorURL">
			<c:param name="name" value="${actorInfo.name}" />
		</c:url>
		<li class="actor"><a href='<c:out value="${actorURL}"/>'><c:out
			value="${actorInfo.name}" /></a> as <em><c:out
			value="${actorInfo.role}" /></em></li>
	</c:forEach>
</ul>
<%@ include file="/jsp/menu.jsp"%>
</body>
</html>
