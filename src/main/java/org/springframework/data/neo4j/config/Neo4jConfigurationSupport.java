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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apiguardian.api.API;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Internal support class for basic configuration. The support infrastructure here is basically all around finding out
 * about which classes are to be mapped and which not. The driver needs to be configured from a class either extending
 * {@link AbstractNeo4jConfig} for imperative or {@link AbstractReactiveNeo4jConfig} for reactive programming model.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
abstract class Neo4jConfigurationSupport {

	@Bean
	public Neo4jConversions neo4jConversions() {
		return new Neo4jConversions();
	}

	/**
	 * Creates a {@link Neo4jMappingContext} equipped with entity classes scanned from the mapping base package.
	 *
	 * @return A new {@link Neo4jMappingContext} with initial classes to scan for entities set.
	 * @see #getMappingBasePackages()
	 */
	@Bean
	public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions) throws ClassNotFoundException {

		Neo4jMappingContext mappingContext = new Neo4jMappingContext(neo4JConversions);
		mappingContext.setInitialEntitySet(getInitialEntitySet());

		return mappingContext;
	}

	/**
	 * Returns the base packages to scan for Neo4j mapped entities at startup. Will return the package name of the
	 * configuration class' (the concrete class, not this one here) by default. So if you have a
	 * {@code com.acme.AppConfig} extending {@link Neo4jConfigurationSupport} the base package will be considered
	 * {@code com.acme} unless the method is overridden to implement alternate behavior.
	 *
	 * @return the base packages to scan for mapped {@link Node} classes or an empty collection to not enable scanning for
	 *         entities.
	 */
	protected Collection<String> getMappingBasePackages() {

		Package mappingBasePackage = getClass().getPackage();
		return Collections.singleton(mappingBasePackage == null ? null : mappingBasePackage.getName());
	}

	/**
	 * Scans the mapping base package for classes annotated with {@link Node}. By default, it scans for entities in all
	 * packages returned by {@link #getMappingBasePackages()}.
	 *
	 * @return initial set of domain classes
	 * @throws ClassNotFoundException if the given class cannot be found in the class path.
	 * @see #getMappingBasePackages()
	 */
	protected final Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {

		Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();

		for (String basePackage : getMappingBasePackages()) {
			initialEntitySet.addAll(scanForEntities(basePackage));
		}

		return initialEntitySet;
	}

	/**
	 * Scans the given base package for entities, i.e. Neo4j specific types annotated with {@link Node}.
	 *
	 * @param basePackage must not be {@literal null}.
	 * @return found entities in the package to scan.
	 * @throws ClassNotFoundException if the given class cannot be loaded by the class loader.
	 */
	protected final Set<Class<?>> scanForEntities(String basePackage) throws ClassNotFoundException {

		if (!StringUtils.hasText(basePackage)) {
			return Collections.emptySet();
		}

		Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();

		ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
				false);
		componentProvider.addIncludeFilter(new AnnotationTypeFilter(Node.class));

		ClassLoader classLoader = Neo4jConfigurationSupport.class.getClassLoader();
		for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
			initialEntitySet.add(ClassUtils.forName(candidate.getBeanClassName(), classLoader));
		}

		return initialEntitySet;
	}
}
