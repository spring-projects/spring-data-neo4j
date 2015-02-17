Hilly Fields Technical College
==============================
![Logo](https://github.com/neo4j/neo4j-ogm/blob/master/neo4j-spring-examples/sdn-boot/src/main/webapp/assets/images/engineering-dept.JPG)

This project is a demo application for the [Spring Data Neo4j](https://github.com/SpringSource/spring-data-neo4j)
library which provides convenient access to the [Neo4j](http://neo4j.org) graph database.

This tutorial is a fully functioning micro-service based web-application built using the following components

- Spring Boot
- Spring Data Neo4j
- Angular.js
- Twitter Bootstrap UI

The application's domain is a fictitious educational institution - Hilly Fields Technical College - and the application
allows you to manage the College's Departments, Teaching Staff, Subjects, Students and Classes.

It leverages the power of Spring Data Neo4j/Spring Boot and in particular the new Neo4j Object Graph mapping technology to provide a RESTful interface with which the web client interacts. The application is entirely stateless: every interaction involves a call to a Neo4j server, hopefully demonstrating the speed of the new technology, even over the wire.

WARNING
-------
By default, the application will attempt to use a Neo4j instance running on the same machine as the application server, and on the standard port 7474. *IT WILL DESTROY ALL THE DATA IN THAT DATABASE AT STARTUP*. So if you don't want that to happen please back up any existing database first.

Start Neo4j
-----------

Now start your Neo4j server instance, if its not already running. 

**You should back up any data you want to keep because the application will purge any existing data first**

Installing SDN
--------------
If you have not already done so, you'll need to download the latest version of SDN from GitHub:

    git clone https://github.com/neo4j/neo4j-ogm.git
    cd neo4j-ogm
    mvn clean install -DskipTests=true

Once this is done, you can start the Spring-Boot application server.

Starting the application
------------------------

    cd neo4j-spring-examples/sdn-boot
    mvn spring-boot:run

Loading the initial dataset
---------------------------
The application can load a set of base data, to get you started. Please be aware that this will destroy
any existing data that may previously exist, so take a backup.

    http://localhost:8080/api/reload

This will pre-load the Neo4j database with a handful of departments, a dozen or so subjects and teachers,
and 200 students. You'll probably want to enrol them in classes...

Exploring the API:
-----------------
The JSON resources from the server can be inspected from the /api/... URLs, e.g.

    http://localhost:8080/api/departments/
    http://localhost:8080/api/subjects/
    http://localhost:8080/api/teachers/
    http://localhost:8080/api/students/
    http://localhost:8080/api/classes/

Running the Hilly Fields web application
----------------------------------------
Simply point your browser at the root URL:

    http://localhost:8080

Stopping the application server
-------------------------------
You can stop the application server at any time by pressing Ctrl-C in the console window from where you launched it.

Make it better!
---------------
If you'd like to develop this application further, you will need to install node.js if it is not already installed:

- Node.js v0.10x+
- npm (which comes bundled with Node) v2.1.0+

Visit the node.js website for details of installing node.js for your particular operating system.

Once node is installed you'll then need to grab the following npm packages:

    npm install --global bower grunt-cli






