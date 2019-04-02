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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;
import org.springframework.data.neo4j.repository.support.NodeManagerFactoryBean;
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
	static final String GENERATE_BEAN_NAME = "(generated)";

	static final String DEFAULT_NODE_MANAGER_FACTORY_BEAN_NAME = "nodeManagerFactory";
	static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";

	/**
	 * Holds the name of the shared NodeManagerBean created from the factory with the configured name.
	 */
	private String generatedNodeManagerBeanName;

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

		builder.addPropertyValue("transactionManager",
			source.getAttribute("transactionManagerRef").orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));

		builder.addPropertyReference("nodeManager", this.generatedNodeManagerBeanName);
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry,
		RepositoryConfigurationSource config) {

		// Mapping context
		AbstractBeanDefinition neo4jMappingContextBeanDefinition = BeanDefinitionBuilder
			.rootBeanDefinition(Neo4jMappingContext.class)
			.getBeanDefinition();
		String nameOfMappingContextBean = registerWithSourceAndGeneratedBeanName(
			neo4jMappingContextBeanDefinition, registry, config);

		// Augmented node manager factory (creating injectable, shared instances of NodeManager)
		String nameOfNodeManagerFactory = config.getAttribute("nodeManagerFactoryRef")
			.orElse(DEFAULT_NODE_MANAGER_FACTORY_BEAN_NAME);
		AbstractBeanDefinition sharedSessionCreatorBeanDefinition = BeanDefinitionBuilder
			.rootBeanDefinition(NodeManagerFactoryBean.class)
			.addConstructorArgReference(nameOfNodeManagerFactory)
			.addConstructorArgReference(nameOfMappingContextBean)
			.getBeanDefinition();
		this.generatedNodeManagerBeanName = registerWithSourceAndGeneratedBeanName(
			sharedSessionCreatorBeanDefinition, registry, config);
	}

	/**
	 * Uses a generated bean name if {@code configuredBeanName} is equal to {@link #GENERATE_BEAN_NAME}, otherwise uses
	 * the configured bean name to register the new bean. Does not check if there's already a bean under the configured
	 * name but throws a {@link org.springframework.beans.factory.BeanDefinitionStoreException}. Checks whether a bean is
	 * already registered under {@code configuredBeanName} in the given {@link BeanDefinitionRegistry} and uses a
	 * generated name for registering the bean instead. If not, the suggested bean name is used.
	 *
	 * @param bean               must not be {@literal null}.
	 * @param registry           must not be {@literal null}.
	 * @param configuredBeanName must not be {@literal null} or empty.
	 * @param source             must not be {@literal null}.
	 * @return the bean name used for registering the given {@link AbstractBeanDefinition}
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException if the BeanDefinition is invalid or if there
	 *                                                                        is already a BeanDefinition for the specified bean name * (and we are not allowed to override it)
	 */
	private static String registerWithGeneratedNameOrUseConfigured(AbstractBeanDefinition bean,
		BeanDefinitionRegistry registry, String configuredBeanName, Object source) {

		String registeredBeanName = configuredBeanName;
		if (GENERATE_BEAN_NAME.equals(configuredBeanName)) {
			registeredBeanName = registerWithSourceAndGeneratedBeanName(bean, registry, source);
		} else {
			bean.setSource(source);
			registry.registerBeanDefinition(configuredBeanName, bean);
		}
		return registeredBeanName;
	}
}
