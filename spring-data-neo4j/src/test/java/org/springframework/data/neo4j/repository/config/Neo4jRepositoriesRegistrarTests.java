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
package org.springframework.data.neo4j.repository.config;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.neo4j.repository.sample.repo.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link Neo4jRepositoriesRegistrar}.
 *
 * @author Mark Angrish
 */
public class Neo4jRepositoriesRegistrarTests {

	BeanDefinitionRegistry registry;
	AnnotationMetadata metadata;

	@Before
	public void setUp() {

		metadata = AnnotationMetadata.introspect(Config.class);
		registry = new DefaultListableBeanFactory();
	}

	@Test
	public void configuresRepositoriesCorrectly() {

		Neo4jRepositoriesRegistrar registrar = new Neo4jRepositoriesRegistrar();
		registrar.setResourceLoader(new DefaultResourceLoader());
		registrar.setEnvironment(new StandardEnvironment());
		registrar.registerBeanDefinitions(metadata, registry, ConfigurationClassPostProcessor.IMPORT_BEAN_NAME_GENERATOR);

		Iterable<String> names = Arrays.asList(registry.getBeanDefinitionNames());
		assertThat(names).contains("userRepository");
	}

	@EnableNeo4jRepositories(basePackageClasses = UserRepository.class)
	class Config {

	}
}
