<!DOCTYPE html>
<!--[if lt IE 7 ]><html class="ie ie6" lang="en"> <![endif]-->
<!--[if IE 7 ]><html class="ie ie7" lang="en"> <![endif]-->
<!--[if IE 8 ]><html class="ie ie8" lang="en"> <![endif]-->
<!--[if (gte IE 9)|!(IE)]><!--><html lang="en"> <!--<![endif]-->
<head>

	<!-- Basic Page Needs
  ================================================== -->
	<meta charset="utf-8">
	<title>SDN Todos</title>
	<meta name="description" content="An SDN Demo Application">
	<meta name="author" content="Andreas Kollegger">
	<!--[if lt IE 9]>
		<script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
	<![endif]-->

	<!-- Mobile Specific Metas
  ================================================== -->
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">

	<!-- CSS
  ================================================== -->
	<link rel="stylesheet" href="stylesheets/base.css">
	<link rel="stylesheet" href="stylesheets/skeleton.css">
	<link rel="stylesheet" href="stylesheets/layout.css">

	<!-- Favicons
	================================================== -->
	<link rel="shortcut icon" href="images/favicon.ico">
	<link rel="apple-touch-icon" href="images/apple-touch-icon.png">
	<link rel="apple-touch-icon" sizes="72x72" href="images/apple-touch-icon-72x72.png">
	<link rel="apple-touch-icon" sizes="114x114" href="images/apple-touch-icon-114x114.png">

</head>
<body>



	<!-- Primary Page Layout
	================================================== -->

	<!-- Delete everything in this .container and get started on your own site! -->

	<div class="container">
		<div class="sixteen columns">
			<h1 class="remove-bottom" style="margin-top: 40px">Spring Data Neo4j: Todos</h1>
			<h5>heroku-deploy</h5>
			<hr />
		</div>
		<div class="row">
			<div class="one-third column">
				<h3>About SDN Todos</h3>
				<p>SDN Todos is a small demonstration application of Spring Data Neo4j. SDN is used to define a simple 'Todo' domain entity, exposed with a JSON endpoint.
				Inspired by the SproutCore (now <a href="http://www.emberjs.com/">ember.js</a>) demo application, this was originally intended as a server-side 
				companion using Neo4j. Now, it is a headless baseline implementation with a simple CLI. 
				</p>
			</div>
			<div class="one-third column">
				<h3>Highlights</h3>
				<p>SDN Todos is small, yet demonstrative:</p>
				<ul class="square">
					<li><strong>Spring Data Neo4j simplicity</strong>: annotated POJO, companion Repository and MVC controller</li>
                    <li><strong>Heroku ready</strong>: run locally, or easily deploy into the cloud</li>
                    <li><strong>CLI clarity</strong>: no distracting UI, just simple tooling</li>
				</ul>
			</div>
			<div class="one-third column">
				<h3>Docs &amp; Support</h3>
				<p>To learn more about developing with Spring Data Neo4j, visit <a href="https://spring.neo4j.org">spring.neo4j.org</a>.
				For questions or comments, join us on the <a href="http://groups.google.com/group/neo4j">Neo4j google group</a>.
			</div>
		</div>
				
		<!-- UI -->
		<div class="row">
			<h5>Todos (a simply jquery pull from the json endpoint):</h5>
			
			<div id="todos">
			</div>
		
			<hr />
		</div>
		
		<!--  footer  -->
		<div class="row">
			<div class="eight columns">
			<a href="http://neo4j.org"><image src="images/icon_neo4j.png"/></a>
			</div>
			<div class="eight columns">
			<a href="http://spring.neo4j.org"><image style="float:right" src="images/icon_sdn.png"/></a>
			</div>
		</div>
		
	</div><!-- container -->



	<!-- JS
	================================================== -->
	<script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
	<script src="javascripts/sdn-todos.js"></script>

<!-- End Document
================================================== -->
</body>
</html>
