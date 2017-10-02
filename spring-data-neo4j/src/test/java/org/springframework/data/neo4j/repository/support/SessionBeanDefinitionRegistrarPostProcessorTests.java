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
package org.springframework.data.neo4j.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Mark Angrish
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SessionBeanDefinitionRegistrarPostProcessorTests.Config.class)
public class SessionBeanDefinitionRegistrarPostProcessorTests extends MultiDriverTestClass {

	@Configuration
	@ComponentScan(includeFilters = @ComponentScan.Filter(TestComponent.class), useDefaultFilters = false)
	static class Config {

		@Bean
		public static SessionBeanDefinitionRegistrarPostProcessor processor() {
			return new SessionBeanDefinitionRegistrarPostProcessor();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.examples.friends.domain");
		}
	}

	@Autowired SessionInjectionTarget target;

	/**
	 */
	@Test
	public void injectsSessionIntoConstructors() {

		assertThat(target, is(notNullValue()));
		assertThat(target.session, is(notNullValue()));
	}

	@TestComponent
	static class SessionInjectionTarget {

		private final Session session;

		@Autowired
		public SessionInjectionTarget(Session session) {
			this.session = session;
		}
	}

	/**
	 * Annotation to demarcate test components.
	 */
	@Component
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface TestComponent {

	}
}
