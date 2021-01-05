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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Unit tests for {@link Neo4jRepositoryConfigurationExtension}.
 *
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class Neo4jRepositoryConfigurationExtensionTests {

	private static final String CUSTOM_SESSION_FACTORY_BEAN_NAME = "mySessionFactory";
	private static final String GENERATED_SESSION_BEAN_NAME = "org.springframework.data.neo4j.transaction.SharedSessionCreator#0";
	private static final String CUSTOM_SESSION_BEAN_NAME = "mySession";
	private static final String CUSTOM_MAPPING_CONTEXT_BEAN_NAME = "myMappingContext";

	public @Rule ExpectedException exception = ExpectedException.none();

	@Test
	public void shouldRegisterSessionFactoryUnderDefaultName() {

		DefaultListableBeanFactory factory = getBeanRegistry(new StandardAnnotationMetadata(Config.class, true));

		Iterable<String> names = Arrays.asList(factory.getBeanDefinitionNames());

		assertThat(names, hasItems(DEFAULT_SESSION_FACTORY_BEAN_NAME));
	}

	@Test // DATAGRAPH-1094
	public void shouldRegisterSharedSessionUnderDefaultName() {

		DefaultListableBeanFactory factory = getBeanRegistry(new StandardAnnotationMetadata(Config.class, true));

		Iterable<String> names = Arrays.asList(factory.getBeanDefinitionNames());

		assertThat(names, hasItems(GENERATED_SESSION_BEAN_NAME));
	}

	@Test // DATAGRAPH-1094
	public void shouldUseConfiguredSessionBeanName() {
		assertTrue(getBeanRegistry(new StandardAnnotationMetadata(CustomSessionBeanConfig.class, true))
				.containsBeanDefinition(CUSTOM_SESSION_BEAN_NAME));
	}

	@Test // DATAGRAPH-1094
	public void shouldUseConfiguredMappingContextBeanName() {
		assertTrue(getBeanRegistry(new StandardAnnotationMetadata(CustomMappingContextBeanConfig.class, true))
				.containsBeanDefinition(CUSTOM_MAPPING_CONTEXT_BEAN_NAME));
	}

	@Test
	public void sessionFactoryBeanNameDefaultsToSessionFactory() {

		BeanDefinition beanDefinition = getBeanRegistry(new StandardAnnotationMetadata(Config.class, true))
				.getBeanDefinition(GENERATED_SESSION_BEAN_NAME);

		RuntimeBeanReference sessionFactoryBean = (RuntimeBeanReference) beanDefinition.getConstructorArgumentValues()
				.getIndexedArgumentValue(0, RuntimeBeanReference.class).getValue();

		String sessionFactoryBeanName = sessionFactoryBean.getBeanName();

		assertThat(sessionFactoryBeanName, equalTo(DEFAULT_SESSION_FACTORY_BEAN_NAME));
	}

	@Test
	public void customSessionFactoryBeanNameWillGetUsed() {

		BeanDefinition beanDefinition = getBeanRegistry(new StandardAnnotationMetadata(CustomSessionRefConfig.class, true))
				.getBeanDefinition(GENERATED_SESSION_BEAN_NAME);

		RuntimeBeanReference sessionFactoryBean = (RuntimeBeanReference) beanDefinition.getConstructorArgumentValues()
				.getIndexedArgumentValue(0, RuntimeBeanReference.class).getValue();

		String sessionFactoryBeanName = sessionFactoryBean.getBeanName();

		assertThat(sessionFactoryBeanName, equalTo(CUSTOM_SESSION_FACTORY_BEAN_NAME));
	}

	@Test
	public void guardsAgainstNullJavaTypesReturnedFromNeo4jMetaData() throws Exception {

		ApplicationContext context = mock(ApplicationContext.class);
		SessionFactory sessionFactory = mock(SessionFactory.class);
		MetaData metaData = mock(MetaData.class);
		ClassInfo classInfo = mock(ClassInfo.class);

		Set<ClassInfo> managedTypes = Collections.singleton(classInfo);

		when(context.getBean(any(String.class), eq(SessionFactory.class))).thenReturn(sessionFactory);
		when(sessionFactory.metaData()).thenReturn(metaData);
		when(metaData.persistentEntities()).thenReturn(managedTypes);
		when(classInfo.name()).thenReturn("Test");

		Neo4jMappingContextFactoryBean factoryBean = new Neo4jMappingContextFactoryBean(
				Neo4jRepositoryConfigurationExtension.DEFAULT_SESSION_FACTORY_BEAN_NAME);
		factoryBean.setApplicationContext(context);

		factoryBean.createInstance().afterPropertiesSet();
	}

	private DefaultListableBeanFactory getBeanRegistry(final StandardAnnotationMetadata metadata) {

		AnnotationConfigApplicationContext factory = new AnnotationConfigApplicationContext();

		factory.registerBean(DEFAULT_SESSION_FACTORY_BEAN_NAME, SessionFactory.class, "dummypackage");

		RepositoryConfigurationExtension extension = new Neo4jRepositoryConfigurationExtension();
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableNeo4jRepositories.class, new PathMatchingResourcePatternResolver(), new StandardEnvironment(), factory);
		extension.registerBeansForRoot(factory, configurationSource);

		return (DefaultListableBeanFactory) factory.getBeanFactory();
	}

	@EnableNeo4jRepositories(considerNestedRepositories = true)
	private static class Config {

	}

	@EnableNeo4jRepositories(sessionFactoryRef = CUSTOM_SESSION_FACTORY_BEAN_NAME)
	private static class CustomSessionRefConfig {

	}

	@EnableNeo4jRepositories(sessionBeanName = CUSTOM_SESSION_BEAN_NAME)
	private static class CustomSessionBeanConfig {

	}

	@EnableNeo4jRepositories(mappingContextBeanName = CUSTOM_MAPPING_CONTEXT_BEAN_NAME)
	private static class CustomMappingContextBeanConfig {

	}
}
