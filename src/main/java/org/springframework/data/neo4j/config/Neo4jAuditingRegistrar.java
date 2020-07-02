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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.auditing.config.AuditingConfiguration;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.neo4j.repository.event.AuditingBeforeBindCallback;
import org.springframework.data.neo4j.repository.event.ReactiveAuditingBeforeBindCallback;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Michael J. Simons
 * @soundtrack Iron Maiden - Killers
 * @since 1.0
 */
final class Neo4jAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	private static final boolean PROJECT_REACTOR_AVAILABLE = ClassUtils.isPresent("reactor.core.publisher.Mono",
		Neo4jAuditingRegistrar.class.getClassLoader());

	private static final String AUDITING_HANDLER_BEAN_NAME = "neo4jAuditingHandler";
	private static final String MAPPING_CONTEXT_BEAN_NAME = "neo4jMappingContext";

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#getAnnotation()
	 */
	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableNeo4jAuditing.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#getAuditingHandlerBeanName()
	 */
	@Override
	protected String getAuditingHandlerBeanName() {
		return AUDITING_HANDLER_BEAN_NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

		Assert.notNull(annotationMetadata, "AnnotationMetadata must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

		super.registerBeanDefinitions(annotationMetadata, registry);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#registerAuditListener(org.springframework.beans.factory.config.BeanDefinition, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	@Override
	protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
		BeanDefinitionRegistry registry) {

		Assert.notNull(auditingHandlerDefinition, "BeanDefinition must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

		BeanDefinitionBuilder listenerBeanDefinitionBuilder = BeanDefinitionBuilder
			.rootBeanDefinition(AuditingBeforeBindCallback.class);
		listenerBeanDefinitionBuilder
			.addConstructorArgValue(
				ParsingUtils.getObjectFactoryBeanDefinition(getAuditingHandlerBeanName(), registry));

		registerInfrastructureBeanWithId(listenerBeanDefinitionBuilder.getBeanDefinition(),
			AuditingBeforeBindCallback.class.getName(), registry);

		if (PROJECT_REACTOR_AVAILABLE) {
			registerReactiveAuditingEntityCallback(registry, auditingHandlerDefinition.getSource());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#getAuditHandlerBeanDefinitionBuilder(org.springframework.data.auditing.config.AuditingConfiguration)
	 */
	@Override
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AuditingConfiguration configuration) {

		Assert.notNull(configuration, "AuditingConfiguration must not be null!");

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(IsNewAwareAuditingHandler.class);

		BeanDefinitionBuilder persistentEntities = BeanDefinitionBuilder
			.genericBeanDefinition(PersistentEntities.class)
			.setFactoryMethod("of");
		persistentEntities.addConstructorArgReference(MAPPING_CONTEXT_BEAN_NAME);

		builder.addConstructorArgValue(persistentEntities.getBeanDefinition());
		return configureDefaultAuditHandlerAttributes(configuration, builder);
	}

	private void registerReactiveAuditingEntityCallback(BeanDefinitionRegistry registry, Object source) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
			.rootBeanDefinition(ReactiveAuditingBeforeBindCallback.class);

		builder.addConstructorArgValue(
			ParsingUtils.getObjectFactoryBeanDefinition(getAuditingHandlerBeanName(), registry));
		builder.getRawBeanDefinition().setSource(source);

		registerInfrastructureBeanWithId(builder.getBeanDefinition(),
			ReactiveAuditingBeforeBindCallback.class.getName(), registry);
	}
}
