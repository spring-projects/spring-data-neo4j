Getting started with data-graph-neo4j
=====================================

It provides an annotation-driven object-graph mapping library. 
Data-graph-neo4j is to Neo4j what Hibernate is to an RDBMS. It requires 
AspectJ and Spring Framework.


Standalone setup of project using data-graph-neo4j
==================================================

1. Include data-graph-neo4j in your pom.xml:

  <dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-neo4j</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
  </dependency> 

2. Add the following plugin XML to your project's <plugins> config in pom.xml
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


3. This is a basic Spring XML context configuration to get started. It creates 
   all dependencies required by the library. After this is set up, you can just
   use the annotated POJOs and they will automatically be backed by Neo4j. The 
   one thing that the user has to do is wrap any data-graph-neo4j usage 
   in Neo4j transactions.

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
     <bean class="org.neo4j.index.lucene.LuceneIndexService" destroy-method="shutdown" id="indexService">
       <constructor-arg index="0" ref="graphDbService"/>
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
         <bean class="org.neo4j.kernel.impl.transaction.SpringTransactionManager" id="neo4jTransactionManagerService">
           <constructor-arg index="0" ref="graphDbService"/>
         </bean>
       </property>
     <property name="userTransaction">
       <bean class="org.neo4j.kernel.impl.transaction.UserTransactionImpl" id="neo4jUserTransactionService">
         <constructor-arg index="0" ref="graphDbService"/>
       </bean>
     </property>
     </bean>
     <bean class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean" id="entityManagerFactory">
       <property name="persistenceUnitName" value="NEO4J"/>
       <property name="persistenceProviderClass" value="org.springframework.data.graph.neo4j.jpa.Neo4jPersistenceProvider"/>
       <property name="jpaDialect">
         <bean class="org.springframework.data.graph.neo4j.jpa.Neo4jJpaDialect"/>
       </property>
     </bean>
     <bean class="org.springframework.context.support.ConversionServiceFactoryBean" id="conversionService"/>
   </beans>

You should now be set up with the AspectJ configuration in your pom.xml, and 
the necessary Spring configuration setting up the library with its
dependencies.
