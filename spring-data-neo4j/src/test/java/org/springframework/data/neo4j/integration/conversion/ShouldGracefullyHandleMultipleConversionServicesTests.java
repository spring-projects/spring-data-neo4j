/*
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.integration.conversion;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
		classes = { ShouldGracefullyHandleMultipleConversionServicesTests.ConversionServicePersistenceContext.class })
public class ShouldGracefullyHandleMultipleConversionServicesTests extends MultiDriverTestClass {

	@Test
	public void contextLoads() {}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = { SiteMemberRepository.class })
	@EnableTransactionManagement
	static class ConversionServicePersistenceContext {

		@Bean
		public ConversionService conversionService1() {
			return new MetaDataDrivenConversionService(sessionFactory().metaData());
		}

		@Bean
		public ConversionService conversionService2() {
			return DefaultConversionService.getSharedInstance();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.integration.conversion.domain");
		}
	}
}
