/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A utility class providing a way to discover an initial entity set for a {@link org.springframework.data.neo4j.core.mapping.Neo4jMappingContext}.
 *
 * @author Michael J. Simons
 * @soundtrack Kelis - Tasty
 * @since 6.0.2
 */
@API(status = API.Status.STABLE, since = "6.0.2")
public final class Neo4jEntityScanner {

	private @Nullable final ApplicationContext context;

	public static Neo4jEntityScanner get() {

		return new Neo4jEntityScanner(null);
	}

	public static Neo4jEntityScanner get(@Nullable ApplicationContext context) {

		return new Neo4jEntityScanner(context);
	}

	/**
	 * Create a new {@link Neo4jEntityScanner} instance.
	 *
	 * @param context the source application context
	 */
	private Neo4jEntityScanner(@Nullable ApplicationContext context) {

		this.context = context;
	}

	/**
	 * Scan for entities with the specified annotations.
	 *
	 * @param basePackages the list of base packages to scan.
	 * @return a set of entity classes
	 * @throws ClassNotFoundException if an entity class cannot be loaded
	 */
	@SafeVarargs
	public final Set<Class<?>> scan(String... basePackages) throws ClassNotFoundException {
		return scan(Arrays.stream(basePackages).collect(Collectors.toList()));
	}

	/**
	 * Scan for entities with the specified annotations.
	 *
	 * @param packages the list of base packages to scan.
	 * @return a set of entity classes
	 * @throws ClassNotFoundException if an entity class cannot be loaded
	 * @see #scan(String...)
	 */
	public Set<Class<?>> scan(Collection<String> packages) throws ClassNotFoundException {

		if (packages.isEmpty()) {
			return Collections.emptySet();
		}

		ClassPathScanningCandidateComponentProvider scanner = createClassPathScanningCandidateComponentProvider(
				this.context);

		ClassLoader classLoader =
				this.context == null ? Neo4jConfigurationSupport.class.getClassLoader() : this.context.getClassLoader();

		Set<Class<?>> entitySet = new HashSet<>();
		for (String basePackage : packages) {
			if (StringUtils.hasText(basePackage)) {
				for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
					entitySet.add(ClassUtils.forName(candidate.getBeanClassName(), classLoader));
				}
			}
		}
		return entitySet;
	}

	/**
	 * Create a {@link ClassPathScanningCandidateComponentProvider} to scan entities based
	 * on the specified {@link ApplicationContext}.
	 *
	 * @param context the optional {@link ApplicationContext} to use
	 * @return a {@link ClassPathScanningCandidateComponentProvider} suitable to scan for Neo4j entities
	 */
	private static ClassPathScanningCandidateComponentProvider createClassPathScanningCandidateComponentProvider(
			ApplicationContext context) {

		ClassPathScanningCandidateComponentProvider delegate = new ClassPathScanningCandidateComponentProvider(false);
		if (context != null) {
			delegate.setEnvironment(context.getEnvironment());
			delegate.setResourceLoader(context);
		}

		delegate.addIncludeFilter(new AnnotationTypeFilter(Node.class));
		delegate.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));
		delegate.addIncludeFilter(new AnnotationTypeFilter(RelationshipProperties.class));

		return delegate;
	}
}
