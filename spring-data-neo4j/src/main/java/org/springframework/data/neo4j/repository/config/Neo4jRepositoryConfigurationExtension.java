/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

/**
 * This dedicated Neo4j repository extension will be registered via {@link Neo4jRepositoriesRegistrar} and then provide
 * all necessary beans to be registered in the application's context before the user's "business" beans gets registered.
 * <p>
 * While it is public, it is mainly used for internal API respectively for Spring Boots automatic configuration.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
public class Neo4jRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

	private static final String MODULE_PREFIX = "neo4j";

	/**
	 * See {@link AbstractBeanDefinition#INFER_METHOD}.
	 */
	static final String DEFAULT_NEO4J_CLIENT_NAME = "neo4jClient";

	/**
	 * See {@link AbstractBeanDefinition#INFER_METHOD}.
	 */
	static final String DEFAULT_MAPPING_CONTEXT_BEAN_NAME = "neo4jMappingContext";

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config14.RepositoryConfigurationExtension#getRepositoryFactoryBeanClassName()
	 */
	@Override
	public String getRepositoryFactoryBeanClassName() {
		return Neo4jRepositoryFactoryBean.class.getName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config14.RepositoryConfigurationExtensionSupport#getModulePrefix()
	 */
	@Override
	protected String getModulePrefix() {
		return MODULE_PREFIX;
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Collections.singleton(Node.class);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(Neo4jRepository.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		builder.addPropertyReference("neo4jClient",
			source.getAttribute("neo4jClientRef").orElse(DEFAULT_NEO4J_CLIENT_NAME));
		builder.addPropertyReference("neo4jMappingContext",
			source.getAttribute("neo4jMappingContextRef").orElse(DEFAULT_MAPPING_CONTEXT_BEAN_NAME));
	}
}
