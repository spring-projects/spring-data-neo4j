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

import static org.junit.Assert.*;

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
		assertTrue("Nothing was saved", result.hasNext());
		assertEquals("The amount wasn't converted and persisted correctly", "42", String.valueOf(result.next().get("fv")));
		result.close();

		PensionPlan reloadedPension = this.pensionRepository.findById(pension.getPensionPlanId()).get();
		assertEquals("The amount was converted incorrectly", new MonetaryAmount(21, 0), reloadedPension.getFundValue());
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
