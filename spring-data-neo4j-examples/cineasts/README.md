![Cineasts.net Logo](https://github.com/jexp/cineasts/raw/master/cineasts.png)
Cineasts.net
============

This project is the demo/tutorial application for the [Spring Data Neo4j](https://github.com/SpringSource/spring-data-neo4j) library which provides convenient access to the [Neo4j](http://neo4j.org) graph database.

This tutorial creates a complete web application built on top of Spring Data Neo4j.

It uses a domain that should be familiar to most - movies. So for cineasts.net we decided to add a social
touch to the whole movie rating business, allowing friends to share their ratings and get recommendations
for new friends and movies.

The tutorial is presented as a colloquial description of the steps necessary to create the application.
It provides the configuration and code examples that are needed to understand what's happening.

The complete tutorial is contained in this projects [github wiki](https://github.com/jexp/cineasts/wiki).

To run it, start your local Neo4j server on localhost:7474 and run `mvn install jetty:run`
