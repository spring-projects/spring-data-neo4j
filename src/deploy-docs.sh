#!/bin/bash

# mvn pre-site has to be run before running this script

set -x
set -e

version=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }'`

filename="spring-data-neo4j-docs-${version}.tar.gz"

cd target/site/reference
tar -czf ../../$filename *
cd ../../../

cat<<EOF >target/docpom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>org.springframework.data</groupId>
	<artifactId>spring-data-neo4j-docs</artifactId>
	<name>Spring Data Neo4j Documents Distribution</name>
	<version>${version}</version>
	<packaging>pom</packaging>
</project>
EOF


mvn install:install-file -f pomFile=target/docpom.xml -Dfile=target/$filename -DgroupId=org.springframework.data -DartifactId=spring-data-neo4j-docs -Dversion=$version -Dpackaging=tar.gz -DpomFile=target/docpom.xml
