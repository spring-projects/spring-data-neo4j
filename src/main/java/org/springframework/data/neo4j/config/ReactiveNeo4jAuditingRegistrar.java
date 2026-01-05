/*
 * Copyright 2011-present the original author or authors.
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
import org.springframework.data.auditing.ReactiveIsNewAwareAuditingHandler;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.auditing.config.AuditingConfiguration;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.neo4j.core.mapping.callback.ReactiveAuditingBeforeBindCallback;
import org.springframework.util.Assert;

/**
 * Registers all beans required for the auditing support.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
final class ReactiveNeo4jAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	private static final String AUDITING_HANDLER_BEAN_NAME = "reactiveNeo4jAuditingHandler";

	private static final String MAPPING_CONTEXT_BEAN_NAME = "neo4jMappingContext";

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableReactiveNeo4jAuditing.class;
	}

	@Override
	protected String getAuditingHandlerBeanName() {
		return AUDITING_HANDLER_BEAN_NAME;
	}

	@Override
	protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
			BeanDefinitionRegistry registry) {

		Assert.notNull(auditingHandlerDefinition, "BeanDefinition must not be null");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
			.rootBeanDefinition(ReactiveAuditingBeforeBindCallback.class);

		builder.addConstructorArgValue(
				ParsingUtils.getObjectFactoryBeanDefinition(getAuditingHandlerBeanName(), registry));
		builder.getRawBeanDefinition().setSource(auditingHandlerDefinition.getSource());

		registerInfrastructureBeanWithId(builder.getBeanDefinition(),
				ReactiveAuditingBeforeBindCallback.class.getName(), registry);
	}

	@Override
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AuditingConfiguration configuration) {

		Assert.notNull(configuration, "AuditingConfiguration must not be null");

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
			.rootBeanDefinition(ReactiveIsNewAwareAuditingHandler.class);

		return configureDefaultAuditHandlerAttributes(configuration, builder);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, AuditingConfiguration configuration,
			BeanDefinitionRegistry registry) {
		builder.setFactoryMethod("from").addConstructorArgReference(MAPPING_CONTEXT_BEAN_NAME);
	}

}
