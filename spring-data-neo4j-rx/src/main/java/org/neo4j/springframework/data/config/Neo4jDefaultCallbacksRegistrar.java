/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.config;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.repository.event.IdGeneratingBeforeBindCallback;
import org.neo4j.springframework.data.repository.event.OptimisticLockingBeforeBindCallback;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * This brings in the default callbacks required for the default implementation of {@link org.neo4j.springframework.data.core.Neo4jOperations} to work.
 * The offered support configuration class {@link AbstractNeo4jConfig} imports this and so does the Spring Boot autoconfiguration.
 *
 * @author Michael J. Simons
 * @soundtrack AC/DC - High Voltage
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public final class Neo4jDefaultCallbacksRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(
		AnnotationMetadata importingClassMetadata,
		BeanDefinitionRegistry registry,
		BeanNameGenerator beanNameGenerator
	) {
		// Id Generator
		RootBeanDefinition beanDefinition = new RootBeanDefinition(IdGeneratingBeforeBindCallback.class);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
		registry.registerBeanDefinition(beanName, beanDefinition);

		// Optimistic locking support
		beanDefinition = new RootBeanDefinition(OptimisticLockingBeforeBindCallback.class);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}
}
