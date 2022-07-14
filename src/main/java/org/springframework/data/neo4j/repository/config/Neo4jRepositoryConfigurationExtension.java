/*
 * Copyright 2011-2022 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apiguardian.api.API;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.Neo4jEvaluationContextExtension;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * This dedicated Neo4j repository extension will be registered via {@link Neo4jRepositoriesRegistrar} and then provide
 * all necessary beans to be registered in the application's context before the user's "business" beans gets registered.
 * <p>
 * While it is public, it is mainly used for internal API respectively for Spring Boots automatic configuration.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class Neo4jRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

	static final String MODULE_NAME = "Neo4j";
	static final String MODULE_PREFIX_ERROR_MSG = "This method has been deprecated and should not have been called";

	/**
	 * See {@link AbstractBeanDefinition#INFER_METHOD}.
	 */
	public static final String DEFAULT_NEO4J_CLIENT_BEAN_NAME = "neo4jClient";

	public static final String DEFAULT_NEO4J_TEMPLATE_BEAN_NAME = "neo4jTemplate";

	public static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";

	/**
	 * See {@link AbstractBeanDefinition#INFER_METHOD}.
	 */
	static final String DEFAULT_MAPPING_CONTEXT_BEAN_NAME = "neo4jMappingContext";

	public Neo4jRepositoryConfigurationExtension() {

		new StartupLogger(StartupLogger.Mode.IMPERATIVE).logStarting();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#getRepositoryFactoryBeanClassName()
	 */
	@Override
	public String getRepositoryFactoryBeanClassName() {
		return Neo4jRepositoryFactoryBean.class.getName();
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected String getModulePrefix() {
		throw new UnsupportedOperationException(MODULE_PREFIX_ERROR_MSG);
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Arrays.asList(Node.class, RelationshipProperties.class);
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		// configurationSource.getSource() might be null and registerIfNotAlreadyRegistered is non-null api,
		// but BeanMetadataAttributeAccessor will be eventually happy with a null value
		// noinspection ConstantConditions
		registerIfNotAlreadyRegistered(() -> BeanDefinitionBuilder
				.rootBeanDefinition(Neo4jEvaluationContextExtension.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition(),
				registry,
				Neo4jEvaluationContextExtension.class.getName(),
				configurationSource.getSource()
		);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(Neo4jRepository.class);
	}

	@Override
	protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {

		return !metadata.isReactiveRepository();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		builder.addPropertyValue("transactionManager",
				source.getAttribute("transactionManagerRef").orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));
		builder.addPropertyReference("neo4jOperations",
				source.getAttribute("neo4jTemplateRef").orElse(DEFAULT_NEO4J_TEMPLATE_BEAN_NAME));
		builder.addPropertyReference("neo4jMappingContext",
				source.getAttribute("neo4jMappingContextRef").orElse(DEFAULT_MAPPING_CONTEXT_BEAN_NAME));
	}

}
