/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.imperative;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.core.convert.Neo4jConversions;
import org.neo4j.springframework.data.integration.shared.Neo4jConversionsITBase;
import org.neo4j.springframework.data.integration.shared.ThingWithAllAdditionalTypes;
import org.neo4j.springframework.data.integration.shared.ThingWithAllCypherTypes;
import org.neo4j.springframework.data.integration.shared.ThingWithAllSpatialTypes;
import org.neo4j.springframework.data.repository.Neo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Tool - Fear Inoculum
 */
@Neo4jIntegrationTest
class TypeConversionIT extends Neo4jConversionsITBase {

	private final Driver driver;

	private final CypherTypesRepository cypherTypesRepository;

	private final AdditionalTypesRepository additionalTypesRepository;

	private final SpatialTypesRepository spatialTypesRepository;

	private final DefaultConversionService defaultConversionService;

	@Autowired TypeConversionIT(
		Driver driver,
		CypherTypesRepository cypherTypesRepository,
		AdditionalTypesRepository additionalTypesRepository,
		SpatialTypesRepository spatialTypesRepository,
		Neo4jConversions neo4jConversions
	) {
		this.driver = driver;
		this.cypherTypesRepository = cypherTypesRepository;
		this.additionalTypesRepository = additionalTypesRepository;
		this.spatialTypesRepository = spatialTypesRepository;
		this.defaultConversionService = new DefaultConversionService();
		neo4jConversions.registerConvertersIn(defaultConversionService);
	}

	@TestFactory
	Stream<DynamicNode> conversionsShouldBeAppliedToEntities() {

		Map<String, Map<String, Object>> supportedTypes = new HashMap<>();
		supportedTypes.put("CypherTypes", CYPHER_TYPES);
		supportedTypes.put("AdditionalTypes", ADDITIONAL_TYPES);
		supportedTypes.put("SpatialTypes", SPATIAL_TYPES);

		return supportedTypes.entrySet().stream()
			.map(entry -> {

				Object thing;
				Object copyOfThing;
				switch (entry.getKey()) {
					case "CypherTypes":
						ThingWithAllCypherTypes hlp = cypherTypesRepository.findById(ID_OF_CYPHER_TYPES_NODE).get();
						copyOfThing = cypherTypesRepository.save(hlp.withId(null));
						thing = hlp;
						break;
					case "AdditionalTypes":
						ThingWithAllAdditionalTypes hlp2 = additionalTypesRepository
							.findById(ID_OF_ADDITIONAL_TYPES_NODE).get();
						copyOfThing = additionalTypesRepository.save(hlp2.withId(null));
						thing = hlp2;
						break;
					case "SpatialTypes":
						ThingWithAllSpatialTypes hlp3 = spatialTypesRepository.findById(ID_OF_SPATIAL_TYPES_NODE)
							.get();
						copyOfThing = spatialTypesRepository.save(hlp3.withId(null));
						thing = hlp3;
						break;
					default:
						throw new UnsupportedOperationException("Unsupported types: " + entry.getKey());
				}

				DynamicContainer reads = DynamicContainer.dynamicContainer("read", entry.getValue().entrySet().stream()
					.map(a -> dynamicTest(a.getKey(),
						() -> assertThat(ReflectionTestUtils.getField(thing, a.getKey())).isEqualTo(a.getValue()))));

				DynamicContainer writes = DynamicContainer
					.dynamicContainer("write", entry.getValue().entrySet().stream()
						.map(a -> dynamicTest(a.getKey(),
							() -> assertWrite(copyOfThing, a.getKey(), defaultConversionService))));

				return DynamicContainer.dynamicContainer(entry.getKey(), Arrays.asList(reads, writes));
			});
	}

	void assertWrite(Object thing, String fieldName, ConversionService conversionService) {

		long id = (long) ReflectionTestUtils.getField(thing, "id");
		Object domainValue = ReflectionTestUtils.getField(thing, fieldName);

		Value driverValue;
		if (domainValue != null && Collection.class.isAssignableFrom(domainValue.getClass())) {
			Collection<?> sourceCollection = (Collection<?>) domainValue;
			Object[] targetCollection = (sourceCollection).stream().map(element ->
				conversionService.convert(element, Value.class)).toArray();
			driverValue = Values.value(targetCollection);
		} else {
			driverValue = conversionService.convert(domainValue, Value.class);
		}

		try (Session session = neo4jConnectionSupport.getDriver().session()) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("id", id);
			parameters.put("attribute", fieldName);
			parameters.put("v", driverValue);

			long cnt = session
				.run("MATCH (n) WHERE id(n) = $id  AND n[$attribute] = $v RETURN COUNT(n) AS cnt",
					parameters)
				.single().get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	public interface CypherTypesRepository
		extends Neo4jRepository<ThingWithAllCypherTypes, Long> {
	}

	public interface AdditionalTypesRepository
		extends Neo4jRepository<ThingWithAllAdditionalTypes, Long> {
	}

	public interface SpatialTypesRepository
		extends Neo4jRepository<ThingWithAllSpatialTypes, Long> {
	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(ThingWithAllCypherTypes.class.getPackage().getName());
		}
	}
}
