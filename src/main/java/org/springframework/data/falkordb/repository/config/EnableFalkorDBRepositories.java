/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.springframework.data.falkordb.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;

/**
 * Annotation to enable FalkorDB repositories. Will scan the package of the annotated
 * configuration class for Spring Data repositories by default.
 *
 * @author Shahar Biron
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(FalkorDBRepositoriesRegistrar.class)
public @interface EnableFalkorDBRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
	 * declarations e.g.: {@code @EnableFalkorDBRepositories("org.my.pkg")} instead of
	 * {@code @EnableFalkorDBRepositories(basePackages="org.my.pkg")}.
	 * @return base packages to scan for repositories
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for
	 * (and mutually exclusive with) this attribute. Use {@link #basePackageClasses()} for
	 * a type-safe alternative to String-based package names.
	 * @return base packages to scan
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to
	 * scan for annotated components. The package of each class specified will be scanned.
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 * @return base package classes
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set
	 * of candidate components from everything in {@link #basePackages()} to everything in
	 * the base packages that matches the given filter or filters.
	 * @return include filters
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 * @return exclude filters
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used when looking up custom repository implementations.
	 * Defaults to {@literal Impl}. So for a repository named {@code PersonRepository} the
	 * corresponding implementation class will be looked up scanning for
	 * {@code PersonRepositoryImpl}.
	 * @return repository implementation postfix
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Configures the location of where to find the Spring Data named queries properties
	 * file. Will default to {@code META-INF/falkordb-named-queries.properties}.
	 * @return named queries location
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link QueryLookupStrategy} to be used for lookup queries
	 * for query methods. Defaults to {@link QueryLookupStrategy.Key#CREATE_IF_NOT_FOUND}.
	 * @return query lookup strategy key
	 */
	QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;

	/**
	 * Configure the repository base class to be used to create repository proxies for
	 * this particular configuration.
	 * @return repository base class
	 */
	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	/**
	 * Configures the name of the
	 * {@link org.springframework.data.falkordb.core.FalkorDBTemplate} bean to be used
	 * with the repositories detected.
	 * @return FalkorDB template bean name
	 */
	String falkorDBTemplateRef() default "falkorDBTemplate";

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes)
	 * should be discovered by the repositories infrastructure.
	 * @return whether to consider nested repositories
	 */
	boolean considerNestedRepositories() default false;

}
