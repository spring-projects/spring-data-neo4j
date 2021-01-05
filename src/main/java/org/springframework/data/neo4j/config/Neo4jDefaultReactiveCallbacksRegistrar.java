/*
 * Copyright 2011-2021 the original author or authors.
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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.neo4j.core.Neo4jOperations;

/**
 * This brings in the default callbacks required for the default implementation of {@link Neo4jOperations} to work. The
 * offered support configuration class {@link AbstractNeo4jConfig} imports this and so does the Spring Boot
 * autoconfiguration.
 *
 * @author Michael J. Simons
 * @soundtrack AC/DC - High Voltage
 * @since 6.0
 * @deprecated since 6.0.2, now an empty implementation, not needed anymore and our default callbacks will be added directly via our
 * infrastructure.
 */
@API(status = API.Status.DEPRECATED, since = "6.0")
@Deprecated
public final class Neo4jDefaultReactiveCallbacksRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator beanNameGenerator) {
	}
}
