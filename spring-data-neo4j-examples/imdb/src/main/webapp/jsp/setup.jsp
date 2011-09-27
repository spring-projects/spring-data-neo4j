<%@ include file="/jsp/include.jsp"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ include file="/jsp/head.jsp"%>

<title>IMDB powered by Neo4j</title>
</head>
<body>
<h1>IMDB powered by Neo4j</h1>
<form:form method="post" commandName="neoSetup">
	<fieldset><legend class="setup">Inject data into the graph</legend>
	<input type="submit" value="Yes, inject the data!"></fieldset>
</form:form>
<%@ include file="/jsp/menu.jsp"%>
</body>
</html>
