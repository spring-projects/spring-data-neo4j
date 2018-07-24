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

package org.springframework.data.neo4j.repository.config;

import static org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension.*;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Annotation to enable Neo4j repositories. Will scan the package of the annotated configuration class for Spring Data
 * repositories by default.
 *
 * @author Vince Bickers
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(Neo4jRepositoriesRegistrar.class)
public @interface EnableNeo4jRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
	 * {@code @EnableExperimentalNeo4jRepositories("org.my.pkg")} instead of
	 * {@code @EnableExperimentalNeo4jRepositories(basePackages="org.my.pkg")}.
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
	 * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
	 * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
	 * each package that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
	 * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
	 */
	ComponentScan.Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 */
	ComponentScan.Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
	 * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
	 * for {@code PersonRepositoryImpl}.
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Configures the location of where to find the Spring Data named queries properties file. Will default to
	 * {@code META-INFO/neo4j-named-queries.properties}.
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link QueryLookupStrategy} to be used for lookup queries for query methods. Defaults to
	 * {@link org.springframework.data.repository.query.QueryLookupStrategy.Key#CREATE_IF_NOT_FOUND}.
	 */
	QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;

	/**
	 * Returns the {@link org.springframework.beans.factory.FactoryBean} class to be used for each repository instance.
	 * Defaults to {@link Neo4jRepositoryFactoryBean}.
	 */
	Class<?> repositoryFactoryBeanClass() default Neo4jRepositoryFactoryBean.class;

	/**
	 * Configure the repository base class to be used to create repository proxies for this particular configuration.
	 *
	 * @return
	 */
	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	/**
	 * Configures the name of the {@link org.neo4j.ogm.session.SessionFactory} bean definition to be used to create
	 * repositories discovered through this annotation. Defaults to {@code sessionFactory}.
	 */
	String sessionFactoryRef() default DEFAULT_SESSION_FACTORY_BEAN_NAME;

	/**
	 * Configures the name of the {@link org.neo4j.ogm.session.Session} bean definition created. Defaults to a generated
	 * name.
	 *
	 * @since 5.1.0
	 */
	String sessionBeanName() default GENERATE_BEAN_NAME;

	/**
	 * Configures the name of the {@link PlatformTransactionManager} bean definition to be used to create repositories
	 * discovered through this annotation. Defaults to {@code transactionManager}.
	 */
	String transactionManagerRef() default DEFAULT_TRANSACTION_MANAGER_BEAN_NAME;

	/**
	 * Configures the name of the {@link org.springframework.data.neo4j.mapping.Neo4jMappingContext} bean definition
	 * created. Defaults to a generated name.
	 *
	 * @since 5.1.0
	 */
	String mappingContextBeanName() default GENERATE_BEAN_NAME;

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
	 * repositories infrastructure.
	 */
	boolean considerNestedRepositories() default false;

	/**
	 * Configures whether to enable default transactions for Spring Data Neo4j repositories. Defaults to {@literal true}.
	 * If disabled, repositories must be used behind a facade that's configuring transactions (e.g. using Spring's
	 * annotation driven transaction facilities) or repository methods have to be used to demarcate transactions.
	 *
	 * @return whether to enable default transactions, defaults to {@literal true}.
	 */
	boolean enableDefaultTransactions() default true;
}
