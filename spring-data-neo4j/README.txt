Getting started with data-graph-neo4j
=====================================

It provides an annotation-driven object-graph mapping library. 
Data-graph-neo4j is to Neo4j what Hibernate is to an RDBMS. It requires 
AspectJ and Spring Framework.

More examples using data-graph-neo4j can be found in this repository:
http://github.com/SpringSource/spring-data-graph-examples

Standalone setup of project using data-graph-neo4j
==================================================


1. Maven Dependency
-------------------

Include data-graph-neo4j in your pom.xml:

  <dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-neo4j</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
  </dependency> 

2. Aspect-J plugin build & library dependency
---------------------------------------------

Add the following plugin XML to your project's <plugins> config in pom.xml 
to hook AspectJ into the build process:

   <plugin>
     <groupId>org.codehaus.mojo</groupId>
     <artifactId>aspectj-maven-plugin</artifactId>
     <version>1.0</version>
     <configuration>
       <outxml>true</outxml>
       <aspectLibraries>
         <aspectLibrary>
           <groupId>org.springframework</groupId>
           <artifactId>spring-aspects</artifactId>
         </aspectLibrary>
         <aspectLibrary>
           <groupId>org.springframework.data</groupId>
           <artifactId>spring-data-neo4j</artifactId>
         </aspectLibrary>
       </aspectLibraries>
       <source>1.6</source>
       <target>1.6</target>
     </configuration>
     <executions>
       <execution>
         <goals>
           <goal>compile</goal>
           <goal>test-compile</goal>
         </goals>
       </execution>
     </executions>
     <dependencies>
       <dependency>
         <groupId>org.aspectj</groupId>
         <artifactId>aspectjrt</artifactId>
         <version>${aspectj.version}</version>
       </dependency>
       <dependency>
         <groupId>org.aspectj</groupId>
         <artifactId>aspectjtools</artifactId>
         <version>${aspectj.version}</version>
       </dependency>
     </dependencies>
   </plugin>


3. Spring configuration
-----------------------

3.1. Spring XML Config
----------------------

This is a the full Spring XML context configuration to get started. It creates 
all dependencies required by the library. 

A simpler configuration approach is provided below the full config:

   <?xml version="1.0" encoding="UTF-8" standalone="no"?>
   <beans xmlns="http://www.springframework.org/schema/beans" 
       xmlns:aop="http://www.springframework.org/schema/aop" 
       xmlns:context="http://www.springframework.org/schema/context" 
       xmlns:jee="http://www.springframework.org/schema/jee" 
       xmlns:tx="http://www.springframework.org/schema/tx" 
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
       xsi:schemaLocation="http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-3.0.xsd
         http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
         http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-3.0.xsd
         http://www.springframework.org/schema/jee http://www.springframework.org/schema/jee/spring-jee-3.0.xsd
         http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx-3.0.xsd">
   
     <!-- Configure property placeholders -->
     <context:property-placeholder location="classpath*:META-INF/spring/*.properties"/>
   
     <!-- Enable AspectJ @Configurable support -->
     <context:spring-configured/>
   
     <!-- Enable @Repository, @Service, @Component, @Autowired, etc. -->
     <context:component-scan base-package="com.springone.petclinic">
       <context:exclude-filter expression=".*_Roo_.*" type="regex"/>
       <context:exclude-filter expression="org.springframework.stereotype.Controller" type="annotation"/>
     </context:component-scan>
     <tx:annotation-driven mode="aspectj" transaction-manager="transactionManager"/>
   
     <bean class="org.springframework.data.graph.neo4j.spi.node.Neo4jConstructorGraphEntityInstantiator" id="graphEntityInstantiator"/>
     <bean class="org.springframework.data.graph.neo4j.spi.relationship.ConstructorBypassingGraphRelationshipInstantiator" id="relationshipEntityInstantiator"/>
     <bean class="org.springframework.data.graph.neo4j.spi.node.Neo4jNodeBacking" factory-method="aspectOf" id="neo4jNodeBacking"/>
     <bean class="org.springframework.data.graph.neo4j.spi.relationship.Neo4jRelationshipBacking" factory-method="aspectOf" id="neo4jRelationshipBacking"/>
     <bean class="org.neo4j.kernel.EmbeddedGraphDatabase" destroy-method="shutdown" id="graphDbService" scope="singleton">
       <constructor-arg index="0" value="${neo4j.location}"/>
     </bean>
     <bean class="org.springframework.data.graph.neo4j.support.GraphDatabaseContext" id="graphDatabaseContext"/>
     <bean class="org.springframework.data.graph.neo4j.support.SubReferenceNodeTypeStrategy" id="nodeTypeStrategy">
       <constructor-arg index="0" ref="graphDatabaseContext"/>
     </bean>
     <bean class="org.springframework.data.graph.neo4j.fieldaccess.NodeEntityStateAccessorsFactory" id="nodeEntityStateAccessorsFactory"/>
     <bean class="org.springframework.data.graph.neo4j.fieldaccess.RelationshipEntityStateAccessorsFactory" id="relationshipEntityStateAccessorsFactory"/>
     <bean class="org.springframework.data.graph.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory" id="nodeDelegatingFieldAccessorFactory">
       <constructor-arg index="0" ref="graphDatabaseContext"/>
     </bean>
     <bean class="org.springframework.data.graph.neo4j.finder.FinderFactory" id="finderFactory">
       <constructor-arg index="0" ref="graphDatabaseContext"/>
     </bean>
     <bean class="org.springframework.transaction.jta.JtaTransactionManager" id="transactionManager">
       <property name="transactionManager">
         <bean class="org.neo4j.kernel.impl.transaction.SpringTransactionManager">
           <constructor-arg index="0" ref="graphDbService"/>
         </bean>
       </property>
		<property name="userTransaction">
       		<bean class="org.neo4j.kernel.impl.transaction.UserTransactionImpl">
       			<constructor-arg index="0" ref="graphDbService"/>
       		</bean>
     	</property>
     </bean>
     <bean class="org.springframework.context.support.ConversionServiceFactoryBean" id="conversionService"/>
   </beans>


3.2 Spring-Java-Config
----------------------

You can also derive from a provided abstract spring configuration class (spring-javaconfig) that already encapsulates all this
configuration and just provide a directory for the graph database:

public class MyConfig extends AbstractNeo4jConfiguration {
    @Override
    public boolean isUsingCrossStorePersistence() {
        return false;
    }

    @Bean(destroyMethod = "shutDown")
    public GraphDatabaseService graphDatabaseService() {
        return new EmbeddedGraphDatabase("target/neo4j-db");
    }
}

Then your spring xml configuration file gets much simpler:
<beans>
...

	<tx:annotation-driven mode="aspectj" transaction-manager="transactionManager"/>
	<bean class="com.example.config.MyConfig"/>
    <bean class="org.springframework.context.annotation.ConfigurationClassPostProcessor"/>
...
</beans>    

4. Setup done
-------------

After this is set up, you can just use the annotated POJOs and they will automatically be backed by Neo4j. 
The one thing that the user has to do is wrap any data-graph-neo4j usage in Neo4j transactions. (or just use @Transactional)

You should now be set up with the AspectJ configuration in your pom.xml, and 
the necessary Spring configuration setting up the library with its
dependencies.
