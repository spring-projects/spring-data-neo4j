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
package org.springframework.data.neo4j.integration.conversion_imperative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.util.AnnotationUtils;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.conversion.Neo4jConversionsITBase;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithAllAdditionalTypes;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAllCypherTypes;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAllSpatialTypes;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCompositeProperties;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCustomTypes;
import org.springframework.data.neo4j.integration.shared.common.ThingWithNonExistingPrimitives;
import org.springframework.data.neo4j.integration.shared.common.ThingWithUUIDID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ReflectionUtils;

/**
 * @author Michael J. Simons
 * @author Dennis Crissman
 * @soundtrack Tool - Fear Inoculum
 */
@Neo4jIntegrationTest
class TypeConversionIT extends Neo4jConversionsITBase {

	private final Driver driver;

	@Autowired CypherTypesRepository cypherTypesRepository;

	private final AdditionalTypesRepository additionalTypesRepository;

	private final SpatialTypesRepository spatialTypesRepository;

	private final CustomTypesRepository customTypesRepository;
	private final BookmarkCapture bookmarkCapture;

	private final DefaultConversionService defaultConversionService;

	@Autowired TypeConversionIT(Driver driver, CypherTypesRepository cypherTypesRepository,
			AdditionalTypesRepository additionalTypesRepository, SpatialTypesRepository spatialTypesRepository,
			CustomTypesRepository customTypesRepository,
			Neo4jConversions neo4jConversions, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.cypherTypesRepository = cypherTypesRepository;
		this.additionalTypesRepository = additionalTypesRepository;
		this.spatialTypesRepository = spatialTypesRepository;
		this.customTypesRepository = customTypesRepository;
		this.bookmarkCapture = bookmarkCapture;
		this.defaultConversionService = new DefaultConversionService();
		neo4jConversions.registerConvertersIn(defaultConversionService);
	}

	@Test
	void thereShallBeNoDefaultValuesForNonExistingAttributes(@Autowired NonExistingPrimitivesRepository repository) {

		assertThatExceptionOfType(MappingException.class)
				.isThrownBy(() -> repository.findById(ID_OF_NON_EXISTING_PRIMITIVES_NODE))
				.withMessageMatching(
						"Error mapping Record<\\{n: \\{__internalNeo4jId__: \\d+, id: NULL, someBoolean: NULL, __nodeLabels__: \\[\"NonExistingPrimitives\"\\]\\}\\}>")
				.withStackTraceContaining(
						"org.springframework.dao.TypeMismatchDataAccessException: Could not convert NULL into boolean; nested exception is org.springframework.core.convert.ConversionFailedException: Failed to convert from type [null] to type [boolean] for value 'null'; nested exception is java.lang.IllegalArgumentException: A null value cannot be assigned to a primitive type")
				.withRootCauseInstanceOf(IllegalArgumentException.class);
	}

	@TestFactory
	Stream<DynamicNode> conversionsShouldBeAppliedToEntities() {

		Map<String, Map<String, Object>> supportedTypes = new HashMap<>();
		supportedTypes.put("CypherTypes", CYPHER_TYPES);
		supportedTypes.put("AdditionalTypes", ADDITIONAL_TYPES);
		supportedTypes.put("SpatialTypes", SPATIAL_TYPES);
		supportedTypes.put("CustomTypes", CUSTOM_TYPES);

		return supportedTypes.entrySet().stream().map(entry -> {

			Object thing;
			Object copyOfThing;
			switch (entry.getKey()) {
				case "CypherTypes":
					ThingWithAllCypherTypes hlp = cypherTypesRepository.findById(ID_OF_CYPHER_TYPES_NODE).get();
					copyOfThing = cypherTypesRepository.save(hlp.withId(null));
					thing = hlp;
					break;
				case "AdditionalTypes":
					ThingWithAllAdditionalTypes hlp2 = additionalTypesRepository.findById(ID_OF_ADDITIONAL_TYPES_NODE)
							.get();
					copyOfThing = additionalTypesRepository.save(hlp2.withId(null));
					thing = hlp2;
					break;
				case "SpatialTypes":
					ThingWithAllSpatialTypes hlp3 = spatialTypesRepository.findById(ID_OF_SPATIAL_TYPES_NODE).get();
					copyOfThing = spatialTypesRepository.save(hlp3.withId(null));
					thing = hlp3;
					break;
				case "CustomTypes":
					ThingWithCustomTypes hlp4 = customTypesRepository.findById(ID_OF_CUSTOM_TYPE_NODE).get();
					copyOfThing = customTypesRepository.save(hlp4.withId(null));
					thing = hlp4;
					break;
				default:
					throw new UnsupportedOperationException("Unsupported types: " + entry.getKey());
			}

			DynamicContainer reads = DynamicContainer.dynamicContainer("read",
					entry.getValue().entrySet().stream().map(a -> DynamicTest.dynamicTest(a.getKey(),
							() -> assertThat(ReflectionTestUtils.getField(thing, a.getKey()))
									.isEqualTo(a.getValue()))));

			DynamicContainer writes = DynamicContainer.dynamicContainer("write", entry.getValue().entrySet().stream()
					.map(a -> DynamicTest
							.dynamicTest(a.getKey(),
									() -> assertWrite(copyOfThing, a.getKey(), defaultConversionService))));

			return DynamicContainer.dynamicContainer(entry.getKey(), Arrays.asList(reads, writes));
		});
	}

	void assertWrite(Object thing, String fieldName, ConversionService conversionService) {

		long id = (long) ReflectionTestUtils.getField(thing, "id");
		Object domainValue = ReflectionTestUtils.getField(thing, fieldName);

		Field field = ReflectionUtils.findField(thing.getClass(), fieldName);
		Optional<ConvertWith> annotation = AnnotationUtils.findAnnotation(field, ConvertWith.class);
		Function<Object, Value> conversion;
		if (fieldName.equals("dateAsLong")) {
			conversion = o -> Values.value(((Date) o).getTime());
		} else if (fieldName.equals("dateAsString")) {
			conversion = o -> Values.value(new SimpleDateFormat("yyyy-MM-dd").format(o));
		} else {
			conversion = o -> conversionService.convert(o, Value.class);
		}
		Value driverValue;
		if (domainValue != null && Collection.class.isAssignableFrom(domainValue.getClass())) {
			Collection<?> sourceCollection = (Collection<?>) domainValue;
			Object[] targetCollection = (sourceCollection).stream()
					.map(element -> conversion.apply(element)).toArray();
			driverValue = Values.value(targetCollection);
		} else {
			driverValue = conversion.apply(domainValue);
		}

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("id", id);
			parameters.put("attribute", fieldName);
			parameters.put("v", driverValue);

			long cnt = session
					.run("MATCH (n) WHERE id(n) = $id  AND n[$attribute] = $v RETURN COUNT(n) AS cnt", parameters)
					.single().get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test
	void idsShouldBeConverted(@Autowired ConvertedIDsRepository repository) {

		ThingWithUUIDID thing = repository.save(new ThingWithUUIDID("a thing"));
		assertThat(thing.getId()).isNotNull();

		Assertions.assertThat(repository.findById(thing.getId())).isPresent();
	}

	@Test
	void relatedIdsShouldBeConverted(@Autowired ConvertedIDsRepository repository) {

		ThingWithUUIDID aThing = new ThingWithUUIDID("a thing");
		aThing.setAnotherThing(new ThingWithUUIDID("Another thing"));

		ThingWithUUIDID savedThing = repository.save(aThing);

		assertThat(savedThing.getId()).isNotNull();
		Assertions.assertThat(repository.findById(savedThing.getId())).isPresent();
		assertThat(savedThing.getAnotherThing().getId()).isNotNull();
		Assertions.assertThat(repository.findById(savedThing.getAnotherThing().getId())).isPresent();
	}

	@Test
	void parametersTargetingConvertedAttributesMustBeConverted(@Autowired CustomTypesRepository repository) {

		assertThat(repository.findAllByDateAsString(Date.from(ZonedDateTime.of(2013, 5, 6,
				12, 0, 0, 0, ZoneId.of("Europe/Berlin")).toInstant().truncatedTo(ChronoUnit.DAYS))))
				.hasSizeGreaterThan(0);
	}

	public interface ConvertedIDsRepository extends Neo4jRepository<ThingWithUUIDID, UUID> {
	}

	public interface CypherTypesRepository extends Neo4jRepository<ThingWithAllCypherTypes, Long> {
	}

	public interface AdditionalTypesRepository extends Neo4jRepository<ThingWithAllAdditionalTypes, Long> {
	}

	public interface SpatialTypesRepository extends Neo4jRepository<ThingWithAllSpatialTypes, Long> {
	}

	public interface NonExistingPrimitivesRepository extends Neo4jRepository<ThingWithNonExistingPrimitives, Long> {
	}

	public interface CustomTypesRepository extends Neo4jRepository<ThingWithCustomTypes, Long> {

		List<ThingWithCustomTypes> findAllByDateAsString(Date theDate);
	}

	public interface ThingWithCompositePropertiesRepository
			extends Neo4jRepository<ThingWithCompositeProperties, Long> {
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
		public Neo4jConversions neo4jConversions() {
			return new Neo4jConversions(Collections.singleton(new ThingWithCustomTypes.CustomTypeConverter()));
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
