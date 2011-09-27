<%@ include file="/jsp/include.jsp"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ include file="/jsp/head.jsp"%>

<title>IMDB powered by Neo4j</title>
</head>
<body>
<h1>IMDB powered by Neo4j</h1>
<form:form method="get" commandName="findActor">
	<fieldset><legend class="actor">Find actor</legend> <label for="name">Enter
	name</label> <form:input path="name" cssClass="inputField" /> <br>
	<input type="submit" value="Search"></fieldset>
</form:form>
<%@ include file="/jsp/menu.jsp"%>
</body>
</html>
