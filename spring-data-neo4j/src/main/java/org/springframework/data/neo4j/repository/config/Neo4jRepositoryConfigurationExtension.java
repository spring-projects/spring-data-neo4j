/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.Neo4jAuditingBeanFactoryPostProcessor;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;
import org.springframework.data.neo4j.transaction.SharedSessionCreator;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.util.StringUtils;

/**
 * Neo4j specific configuration extension parsing custom attributes from the XML namespace and
 * {@link EnableNeo4jRepositories} annotation. Creates and registers {@link BeanDefinitionBuilder} for
 * {@link SharedSessionCreator}, {@link Neo4jMappingContextFactoryBean} and
 * {@link Neo4jAuditingBeanFactoryPostProcessor}.
 *
 * @author Vince Bickers
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Gerrit Meier
 */
public class Neo4jRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";
	private static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";
	private static final String NEO4J_MAPPING_CONTEXT_BEAN_NAME = "neo4jMappingContext";
	private static final String NEO4J_AUDITING_POST_PROCESSOR_NAME = "neo4jAuditionBeanFactoryPostProcessor";
	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";
	private static final String NEO4J_SHARED_SESSION_CREATOR_BEAN_NAME = "sharedSessionCreatorBean";
	private static final String MODULE_PREFIX = "neo4j";
	private static final String MODULE_NAME = "Neo4j";

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getModuleName()
	 */
	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}

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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getIdentifyingAnnotations()
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Arrays.asList(NodeEntity.class, RelationshipEntity.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getIdentifyingTypes()
	 */
	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.<Class<?>> singleton(Neo4jRepository.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		String transactionManagerRefPropertyName = "transactionManagerRef";
		String transactionManagerPropertyName = "transactionManager";
		String mappingContextPropertyName = "mappingContext";

		Optional<String> transactionManagerRef = source.getAttribute(transactionManagerRefPropertyName);

		builder.addPropertyValue(transactionManagerPropertyName,
				transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));
		builder.addPropertyReference(mappingContextPropertyName, NEO4J_MAPPING_CONTEXT_BEAN_NAME);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

		AnnotationAttributes attributes = config.getAttributes();

		builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE,
				attributes.getBoolean(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.XmlRepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

		Optional<String> enableDefaultTransactions = config.getAttribute(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE);

		if (enableDefaultTransactions.filter(StringUtils::hasText).isPresent()) {
			enableDefaultTransactions
					.ifPresent(value -> builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE, value));

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {

		super.registerBeansForRoot(registry, config);

		Object source = config.getSource();

		registerIfNotAlreadyRegistered(createSharedSessionCreatorBeanDefinition(config), registry,
				NEO4J_SHARED_SESSION_CREATOR_BEAN_NAME, source);

		registerIfNotAlreadyRegistered(new RootBeanDefinition(Neo4jMappingContextFactoryBean.class), registry,
				NEO4J_MAPPING_CONTEXT_BEAN_NAME, source);

		registerIfNotAlreadyRegistered(new RootBeanDefinition(Neo4jAuditingBeanFactoryPostProcessor.class), registry,
				NEO4J_AUDITING_POST_PROCESSOR_NAME, source);
	}

	private AbstractBeanDefinition createSharedSessionCreatorBeanDefinition(RepositoryConfigurationSource config) {

		String sessionFactoryRefPropertyName = "sessionFactoryRef";
		String sessionFactoryBeanName = config.getAttribute(sessionFactoryRefPropertyName)
				.orElse(DEFAULT_SESSION_FACTORY_BEAN_NAME);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SharedSessionCreator.class,
				"createSharedSession");
		builder.addConstructorArgReference(sessionFactoryBeanName);

		return builder.getBeanDefinition();

	}

}
