<%@ include file="/jsp/include.jsp"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ include file="/jsp/head.jsp"%>

<title>IMDB powered by Neo4j</title>
</head>
<body>
<h1>IMDB powered by Neo4j</h1>
<form:form method="get" commandName="findMovie">
	<fieldset><legend class="movie">Find movie</legend> <label for="title">Enter
	title</label> <form:input path="title" cssClass="inputField" /> <br>
	<input type="submit" value="Search"></fieldset>
</form:form>
<%@ include file="/jsp/menu.jsp"%>
</body>
</html>
