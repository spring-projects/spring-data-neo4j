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

import org.apiguardian.api.API;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.repository.event.ReactiveIdGeneratingBeforeBindCallback;
import org.springframework.data.neo4j.repository.event.ReactiveOptimisticLockingBeforeBindCallback;

/**
 * This brings in the default callbacks required for the default implementation of {@link Neo4jOperations} to work. The
 * offered support configuration class {@link AbstractNeo4jConfig} imports this and so does the Spring Boot
 * autoconfiguration.
 *
 * @author Michael J. Simons
 * @soundtrack AC/DC - High Voltage
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class Neo4jDefaultReactiveCallbacksRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator beanNameGenerator) {
		// Id Generator
		RootBeanDefinition beanDefinition = new RootBeanDefinition(ReactiveIdGeneratingBeforeBindCallback.class);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
		registry.registerBeanDefinition(beanName, beanDefinition);

		// Optimistic locking support
		beanDefinition = new RootBeanDefinition(ReactiveOptimisticLockingBeforeBindCallback.class);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}
}
