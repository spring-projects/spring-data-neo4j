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
package org.springframework.data.neo4j.repository.config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.Neo4jPersistenceExceptionTranslator;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;
import org.springframework.data.neo4j.transaction.SharedSessionCreator;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Neo4j specific configuration extension parsing custom attributes from the XML namespace and
 * {@link EnableNeo4jRepositories} annotation. Creates and registers {@link BeanDefinitionBuilder} for
 * {@link SharedSessionCreator} and {@link Neo4jMappingContextFactoryBean}. Also, it registers a bean definition for a
 * {@link PersistenceExceptionTranslationPostProcessor} to enable exception translation of persistence specific
 * exceptions into Spring's {@link DataAccessException} hierarchy.
 *
 * @author Vince Bickers
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class Neo4jRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

	/** See {@link AbstractBeanDefinition#INFER_METHOD}. */
	static final String GENERATE_BEAN_NAME = "(generated)";
	static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";
	static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";

	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";
	private static final String NEO4J_PERSISTENCE_EXCEPTION_TRANSLATOR_NAME = "neo4jPersistenceExceptionTranslator";
	private static final String MODULE_PREFIX = "neo4j";
	private static final String MODULE_NAME = "Neo4j";

	/**
	 * We use a generated name for every pair of {@code SessionFactory} and {@code Session} unless the user configures a
	 * session bean name with {@code @EnableNeo4jRepositories(sessionBeanName="someName")}.
	 */
	private String sessionBeanName;

	/**
	 * We use a generated name for every pair of {@code SessionFactory} and {@code Neo4jMappingContext} unless the user
	 * configures a session bean name with {@code @EnableNeo4jRepositories(mappingContextBeanName="someName")}.
	 */
	private String neo4jMappingContextBeanName;

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
		String sessionPropertyName = "session";

		Optional<String> transactionManagerRef = source.getAttribute(transactionManagerRefPropertyName);
		builder.addPropertyValue(transactionManagerPropertyName,
				transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));

		builder.addPropertyReference(sessionPropertyName, this.sessionBeanName);
		builder.addPropertyReference(mappingContextPropertyName, this.neo4jMappingContextBeanName);
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
		if (source == null) {
			return;
		}

		String configuredSessionBeanName = config.getAttribute("sessionBeanName").orElse(GENERATE_BEAN_NAME);
		this.sessionBeanName = registerWithGeneratedNameOrUseConfigured(createSharedSessionCreatorBeanDefinition(config),
				registry, configuredSessionBeanName, source);

		String configuredMappingContextBeanName = config.getAttribute("mappingContextBeanName").orElse(GENERATE_BEAN_NAME);
		this.neo4jMappingContextBeanName = registerWithGeneratedNameOrUseConfigured(
				createNeo4jMappingContextFactoryBeanDefinition(config), registry, configuredMappingContextBeanName, source);

		registerIfNotAlreadyRegistered(() -> new RootBeanDefinition(Neo4jPersistenceExceptionTranslator.class), registry,
				NEO4J_PERSISTENCE_EXCEPTION_TRANSLATOR_NAME, source);

		AbstractBeanDefinition rootBeanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(Neo4jOgmEntityInstantiatorConfigurationBean.class)
				.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
				.addConstructorArgReference(getSessionFactoryBeanName(config))
				.addConstructorArgReference(this.neo4jMappingContextBeanName).getBeanDefinition();
		registerWithGeneratedNameOrUseConfigured(rootBeanDefinition, registry, GENERATE_BEAN_NAME, source);
	}

	/**
	 * Uses a generated bean name if {@code configuredBeanName} is equal to {@link #GENERATE_BEAN_NAME}, otherwise uses
	 * the configured bean name to register the new bean. Does not check if there's already a bean under the configured
	 * name but throws a {@link org.springframework.beans.factory.BeanDefinitionStoreException}. Checks whether a bean is
	 * already registered under {@code configuredBeanName} in the given {@link BeanDefinitionRegistry} and uses a
	 * generated name for registering the bean instead. If not, the suggested bean name is used.
	 *
	 * @param bean must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 * @param configuredBeanName must not be {@literal null} or empty.
	 * @param source must not be {@literal null}.
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException if the BeanDefinition is invalid or if there
	 *           is already a BeanDefinition for the specified bean name * (and we are not allowed to override it)
	 * @return the bean name used for registering the given {@link AbstractBeanDefinition}
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

	private static AbstractBeanDefinition createSharedSessionCreatorBeanDefinition(RepositoryConfigurationSource config) {

		String sessionFactoryBeanName = getSessionFactoryBeanName(config);

		AbstractBeanDefinition sharedSessionCreatorBeanDefinition = BeanDefinitionBuilder //
				.rootBeanDefinition(SharedSessionCreator.class, "createSharedSession") //
				.addConstructorArgReference(sessionFactoryBeanName) //
				.getBeanDefinition();

		sharedSessionCreatorBeanDefinition
				.addQualifier(new AutowireCandidateQualifier(Qualifier.class, sessionFactoryBeanName));

		return sharedSessionCreatorBeanDefinition;
	}

	private static AbstractBeanDefinition createNeo4jMappingContextFactoryBeanDefinition(
			RepositoryConfigurationSource config) {

		return BeanDefinitionBuilder //
				.rootBeanDefinition(Neo4jMappingContextFactoryBean.class) //
				.addConstructorArgValue(getSessionFactoryBeanName(config)) //
				.getBeanDefinition();
	}

	private static String getSessionFactoryBeanName(RepositoryConfigurationSource config) {
		return Optional.of("sessionFactoryRef").flatMap(config::getAttribute).orElse(DEFAULT_SESSION_FACTORY_BEAN_NAME);
	}

}
