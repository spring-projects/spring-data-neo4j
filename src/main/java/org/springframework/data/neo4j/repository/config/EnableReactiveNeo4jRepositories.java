/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.repository.support.ReactiveNeo4jRepositoryFactoryBean;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;

/**
 * Annotation to activate reactive Neo4j repositories. If no base package is configured through either {@link #value()},
 * {@link #basePackages()} or {@link #basePackageClasses()} it will trigger scanning of the package of annotated
 * configuration class.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ReactiveNeo4jRepositoriesRegistrar.class)
public @interface EnableReactiveNeo4jRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
	 * {@code @EnableReactiveNeo4jRepositories("org.my.pkg")} instead of
	 * {@code @EnableReactiveNeo4jRepositories(basePackages="org.my.pkg")}.
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
	 * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
	 * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
	 * each package that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Returns the {@link FactoryBean} class to be used for each repository instance. Defaults to
	 * {@link ReactiveNeo4jRepositoryFactoryBean}.
	 */
	Class<?> repositoryFactoryBeanClass() default ReactiveNeo4jRepositoryFactoryBean.class;

	/**
	 * Configure the repository base class to be used to create repository proxies for this particular configuration.
	 *
	 * @return The base class to be used when creating repository proxies.
	 */
	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	/**
	 * Configures the name of the {@link Neo4jMappingContext} bean to be used with the repositories detected.
	 */
	String neo4jMappingContextRef() default ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_MAPPING_CONTEXT_BEAN_NAME;

	/**
	 * Configures the name of the {@link ReactiveNeo4jTemplate} bean to be used with the repositories detected.
	 */
	String neo4jTemplateRef() default ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME;

	/**
	 * Configures the name of the {@link ReactiveNeo4jTransactionManager} bean to be used with the repositories detected.
	 */
	String transactionManagerRef() default ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME;

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
	 * Configures the location of where to find the Spring Data named queries properties file. Will default to
	 * {@code META-INFO/neo4j-named-queries.properties}.
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
	 * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
	 * for {@code PersonRepositoryImpl}.
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
	 * repositories infrastructure.
	 */
	boolean considerNestedRepositories() default false;
}
