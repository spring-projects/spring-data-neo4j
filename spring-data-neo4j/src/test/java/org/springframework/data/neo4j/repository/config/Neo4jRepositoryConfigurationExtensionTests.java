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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.neo4j.repository.support.SessionBeanDefinitionRegistrarPostProcessor;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

/**
 * Unit tests for {@link Neo4jRepositoryConfigurationExtension}.
 *
 * @author Mark Angrish
 * @author Mark Paluch
 */
public class Neo4jRepositoryConfigurationExtensionTests {

	BeanDefinitionRegistry registry = new DefaultListableBeanFactory();
	RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(new StandardAnnotationMetadata(Config.class, true), EnableNeo4jRepositories.class, new PathMatchingResourcePatternResolver(), new StandardEnvironment(), registry);

	public @Rule ExpectedException exception = ExpectedException.none();

	@Test
	public void registersDefaultBeanPostProcessorsByDefault() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

		RepositoryConfigurationExtension extension = new Neo4jRepositoryConfigurationExtension();
		extension.registerBeansForRoot(factory, configSource);

		Iterable<String> names = Arrays.asList(factory.getBeanDefinitionNames());

		assertThat(names, hasItems("sessionBeanDefinitionRegistrarPostProcessor"));
	}

	@Test
	public void doesNotRegisterProcessorIfAlreadyPresent() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition pabppDefinition = new RootBeanDefinition(SessionBeanDefinitionRegistrarPostProcessor.class);
		String beanName = BeanDefinitionReaderUtils.generateBeanName(pabppDefinition, factory);
		factory.registerBeanDefinition(beanName, pabppDefinition);

		assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(factory, beanName);
	}

	@Test
	public void doesNotRegisterProcessorIfAutoRegistered() {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition pabppDefinition = new RootBeanDefinition(SessionBeanDefinitionRegistrarPostProcessor.class);
		String beanName = "sessionBeanDefinitionRegistrarPostProcessor";
		factory.registerBeanDefinition(beanName, pabppDefinition);

		assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(factory, beanName);
	}

	@Test
	public void guardsAgainstNullJavaTypesReturnedFromNeo4jMetaData() throws Exception {

		ApplicationContext context = mock(ApplicationContext.class);
		SessionFactory sessionFactory = mock(SessionFactory.class);
		MetaData metaData = mock(MetaData.class);
		ClassInfo classInfo = mock(ClassInfo.class);

		Set<ClassInfo> managedTypes = Collections.singleton(classInfo);

		when(context.getBean(SessionFactory.class)).thenReturn(sessionFactory);
		when(sessionFactory.metaData()).thenReturn(metaData);
		when(metaData.persistentEntities()).thenReturn(managedTypes);
		when(classInfo.name()).thenReturn("Test");

		Neo4jMappingContextFactoryBean factoryBean = new Neo4jMappingContextFactoryBean();
		factoryBean.setApplicationContext(context);

		factoryBean.createInstance().afterPropertiesSet();
	}

	private void assertOnlyOnePersistenceAnnotationBeanPostProcessorRegistered(DefaultListableBeanFactory factory,
																			   String expectedBeanName) {

		RepositoryConfigurationExtension extension = new Neo4jRepositoryConfigurationExtension();
		extension.registerBeansForRoot(factory, configSource);

		assertThat(factory.getBean(expectedBeanName), is(notNullValue()));
		exception.expect(NoSuchBeanDefinitionException.class);
		factory.getBeanDefinition("org.springframework.data.neo4j.repository.support.SessionBeanDefinitionRegistrarPostProcessor#1");
	}

	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config {

	}
}
