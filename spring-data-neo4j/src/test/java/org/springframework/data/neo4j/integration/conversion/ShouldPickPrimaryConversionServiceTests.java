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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.neo4j.integration.conversion.domain.MonetaryAmount;
import org.springframework.data.neo4j.integration.conversion.domain.PensionPlan;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ShouldPickPrimaryConversionServiceTests.ConversionServicePersistenceContext.class)
public class ShouldPickPrimaryConversionServiceTests {

	@Autowired private GraphDatabaseService graphDatabaseService;

	@Autowired private PensionRepository pensionRepository;

	@Before
	public void setUp() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldWorkWithArbitraryPrimaryConversionBeans() {
		PensionPlan pension = new PensionPlan(new MonetaryAmount(16472, 81), "Tightfist Asset Management Ltd");
		pension = this.pensionRepository.save(pension);

		Result result = graphDatabaseService.execute("MATCH (p:PensionPlan) RETURN p.fundValue AS fv");
		assertThat(result.hasNext()).as("Nothing was saved").isTrue();
		assertThat(String.valueOf(result.next().get("fv")))
				.as("The amount wasn't converted and persisted correctly")
				.isEqualTo("42");
		result.close();

		PensionPlan reloadedPension = this.pensionRepository.findById(pension.getPensionPlanId()).get();
		assertThat(reloadedPension.getFundValue())
				.as("The amount was converted incorrectly")
				.isEqualTo(new MonetaryAmount(21, 0));
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.integration.conversion.domain",
			repositoryPackages = "org.springframework.data.neo4j.integration.conversion")
	static class ConversionServicePersistenceContext {

		/**
		 * @return A pretty bogus conversion, basically only half the truth.
		 */
		@Bean
		@Primary
		public ConversionService conversionService1() {
			GenericConversionService conversionService = new GenericConversionService();
			// Please don't replace with lambdas, otherwise Spring won't be able to determine source and target types
			conversionService.addConverter(new Converter<MonetaryAmount, Integer>() {
				@Override
				public Integer convert(MonetaryAmount source) {
					return 42;
				}
			});
			conversionService.addConverter(new Converter<Long, MonetaryAmount>() {
				@Override
				public MonetaryAmount convert(Long source) {
					return new MonetaryAmount(21, 0);
				}
			});
			return conversionService;
		}

		@Bean
		public ConversionService conversionService2() {
			return DefaultConversionService.getSharedInstance();
		}
	}
}
