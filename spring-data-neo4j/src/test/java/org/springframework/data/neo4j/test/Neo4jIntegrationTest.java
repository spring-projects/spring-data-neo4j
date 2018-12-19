/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;

/**
 * This is an internal annotation to provide the mininum setup for an integration test against a Neo4j instance running
 * through the Neo4j test harness. It is not meant to be used outside SDN. <br>
 * <br>
 * It turns on {@link EnableNeo4jRepositories Neo4j repositories} as well as
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement transaction management} and provides an
 * injectable {@link org.neo4j.graphdb.GraphDatabaseService graph database service} for manipulating data. <br>
 * If a test provides a custom {@link org.neo4j.ogm.config.Configuration configuration bean} it takes precedence over
 * the one that is created by default. <br>
 * The default is to test SDN via Bolt-Transport. That can be changed in a dedicaded test setup by providing a system
 * property name {@code integration-test-mode} with one of the following values:
 *
 * <pre>
 *     -Dintegration-test-mode=BOLT
 *     -Dintegration-test-mode=EMBEDDED
 *     -Dintegration-test-mode=HTTP
 * </pre>
 *
 * Be aware that those setup require the presence of the corresponding OGM transport on the classpath. Tests that test
 * features specific to a transport (like Bookmarks, only available for Bolt) or native types (Only available for Bolt
 * and Embedded) should create their own {@link org.neo4j.ogm.config.Configuration Configuration}-bean.
 *
 * @author Michael J. Simons
 * @since 5.2
 * @soundtrack Die Ärzte - Nach uns die Sintflut
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableNeo4jRepositories
@Import({ SessionFactoryRegistrar.class, Neo4jTestServerConfiguration.class })
public @interface Neo4jIntegrationTest {

	enum IntegrationTestMode {
		BOLT, HTTP, EMBEDDED
	}

	/**
	 * @return The list of domain packages to be passed on to the {@link org.neo4j.ogm.session.SessionFactory}.
	 */
	String[] domainPackages();

	/**
	 * @return The list of packages to scan for {@link org.springframework.data.neo4j.repository.Neo4jRepository Neo4j
	 *         repositories}.
	 */
	@AliasFor(annotation = EnableNeo4jRepositories.class, attribute = "basePackages")
	String[] repositoryPackages() default {};

	@AliasFor(annotation = EnableNeo4jRepositories.class, attribute = "repositoryBaseClass")
	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	@AliasFor(annotation = EnableNeo4jRepositories.class, attribute = "considerNestedRepositories")
	boolean considerNestedRepositories() default false;

	@AliasFor(annotation = EnableNeo4jRepositories.class, attribute = "transactionManagerRef")
	String transactionManagerRef() default "transactionManager";

	@AliasFor(annotation = EnableNeo4jRepositories.class, attribute = "enableDefaultTransactions")
	boolean enableDefaultTransactions() default true;
}
