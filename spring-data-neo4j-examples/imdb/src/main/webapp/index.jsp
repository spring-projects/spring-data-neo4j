<%@ include file="/jsp/include.jsp"%>
<%@ include file="/jsp/head.jsp"%>

<title>IMDB powered by Neo</title>
</head>
<body>
<h1>IMDB powered by Neo4j</h1>

<ul>
<li>Go to the <a href="<c:url value="actor.html?name=Bacon, Kevin"/>">Kevin Bacon</a> page</li>
<li>Go to the <a href="<c:url value="movie.html?title=Matrix, The (1999)"/>">The Matrix</a> page</li>
<li>Search <a href="<c:url value="actor.html"/>">actors</a><li>
<li>Search <a href="<c:url value="movie.html"/>">movies</a></li>
</ul>

</body>
</html>
