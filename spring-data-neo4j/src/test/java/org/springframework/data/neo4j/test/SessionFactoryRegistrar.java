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
package org.springframework.data.neo4j.test;

import java.util.Map;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Helper for programmatically creating a {@link SessionFactory session factory} using a well defined
 * {@link org.neo4j.ogm.config.Configuration configuration} and a list of domain packages from the
 * {@link Neo4jIntegrationTest}-annotation.
 *
 * @author Michael J. Simons
 * @since 5.2
 * @soundtrack Die Ärzte - Nach uns die Sintflut
 */
class SessionFactoryRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata,
			BeanDefinitionRegistry beanDefinitionRegistry) {

		Map<String, Object> t = annotationMetadata.getAnnotationAttributes(Neo4jIntegrationTest.class.getName());

		AbstractBeanDefinition sessionFactoryDefinition = BeanDefinitionBuilder //
				.rootBeanDefinition(SessionFactory.class) //
				.addConstructorArgReference("neo4jOGMConfiguration") //
				.addConstructorArgValue(t.get("domainPackages")) //
				.getBeanDefinition();

		beanDefinitionRegistry.registerBeanDefinition("sessionFactory", sessionFactoryDefinition);
	}
}
