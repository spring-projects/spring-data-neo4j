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
package org.springframework.data.neo4j.integration.conversion;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
		classes = ShouldGracefullyHandleMultipleConversionServicesTests.ConversionServicePersistenceContext.class)
public class ShouldGracefullyHandleMultipleConversionServicesTests {

	@Test
	public void contextLoads() {}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.integration.conversion.domain",
			repositoryPackages = "org.springframework.data.neo4j.integration.conversion")
	static class ConversionServicePersistenceContext {

		@Bean
		public ConversionService conversionService1(SessionFactory sessionFactory) {
			return new MetaDataDrivenConversionService(sessionFactory.metaData());
		}

		@Bean
		public ConversionService conversionService2() {
			return DefaultConversionService.getSharedInstance();
		}
	}
}
