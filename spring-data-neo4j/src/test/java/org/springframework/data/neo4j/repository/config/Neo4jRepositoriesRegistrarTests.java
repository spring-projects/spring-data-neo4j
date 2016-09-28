/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.neo4j.repository.sample.UserRepository;

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

		metadata = new StandardAnnotationMetadata(Config.class, true);
		registry = new DefaultListableBeanFactory();
	}

	@Test
	public void configuresRepositoriesCorrectly() {

		Neo4jRepositoriesRegistrar registrar = new Neo4jRepositoriesRegistrar();
		registrar.setResourceLoader(new DefaultResourceLoader());
		registrar.setEnvironment(new StandardEnvironment());
		registrar.registerBeanDefinitions(metadata, registry);

		Iterable<String> names = Arrays.asList(registry.getBeanDefinitionNames());
		assertThat(names, hasItems("userRepository"));
	}

	@EnableNeo4jRepositories(basePackageClasses = UserRepository.class)
	class Config {

	}
}
