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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.integration.shared.Neo4jConversionsITBase;
import org.springframework.data.neo4j.integration.shared.ThingWithAllAdditionalTypes;
import org.springframework.data.neo4j.integration.shared.ThingWithAllCypherTypes;
import org.springframework.data.neo4j.integration.shared.ThingWithAllSpatialTypes;
import org.springframework.data.neo4j.integration.shared.ThingWithCustomTypes;
import org.springframework.data.neo4j.integration.shared.ThingWithMapProperties;
import org.springframework.data.neo4j.integration.shared.ThingWithNonExistingPrimitives;
import org.springframework.data.neo4j.integration.shared.ThingWithUUIDID;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.util.ReflectionTestUtils;
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

	private final DefaultConversionService defaultConversionService;

	private final ThingWithMapPropertiesRepository thingWithMapPropertiesRepository;

	@Autowired TypeConversionIT(Driver driver, CypherTypesRepository cypherTypesRepository,
			AdditionalTypesRepository additionalTypesRepository, SpatialTypesRepository spatialTypesRepository,
			CustomTypesRepository customTypesRepository,
			ThingWithMapPropertiesRepository thingWithMapPropertiesRepository,
			Neo4jConversions neo4jConversions) {
		this.driver = driver;
		this.cypherTypesRepository = cypherTypesRepository;
		this.additionalTypesRepository = additionalTypesRepository;
		this.spatialTypesRepository = spatialTypesRepository;
		this.customTypesRepository = customTypesRepository;
		this.thingWithMapPropertiesRepository = thingWithMapPropertiesRepository;
		this.defaultConversionService = new DefaultConversionService();
		neo4jConversions.registerConvertersIn(defaultConversionService);
	}

	@Test
	void customMapPropertiesShouldBeWritten() {

		ThingWithMapProperties t = new ThingWithMapProperties();
		Map<String, LocalDate> someDates = new HashMap<>();
		someDates.put("a", LocalDate.of(2009, 12, 4));
		someDates.put("o", LocalDate.of(2013, 5, 6));
		t.setSomeDates(someDates);

		Map<String, LocalDate> someOtherDates = Collections.singletonMap("t", LocalDate.of(1981, 7, 7));
		t.setSomeOtherDates(someOtherDates);

		Map<String, ThingWithCustomTypes.CustomType> someCustomThings = new HashMap<>();
		someCustomThings.put("x", ThingWithCustomTypes.CustomType.of("c1"));
		someCustomThings.put("y", ThingWithCustomTypes.CustomType.of("c2"));
		t.setCustomTypeMap(someCustomThings);

		t.setSomeDatesByEnumA(Collections.singletonMap(ThingWithMapProperties.EnumA.VALUE_AA, LocalDate.of(2020, 10, 1)));
		Map<ThingWithMapProperties.EnumB, LocalDate> someDatesByEnumB = new HashMap<>();
		someDatesByEnumB.put(ThingWithMapProperties.EnumB.VALUE_BA, LocalDate.of(2020, 10, 2));
		someDatesByEnumB.put(ThingWithMapProperties.EnumB.VALUE_BB, LocalDate.of(2020, 10, 3));
		t.setSomeDatesByEnumB(someDatesByEnumB);

		t.setDatesWithTransformedKey(Collections.singletonMap("TEST", LocalDate.of(1979, 9, 21)));
		t.setDatesWithTransformedKeyAndEnum(Collections.singletonMap(ThingWithMapProperties.EnumB.VALUE_BA, LocalDate.of(1938, 9, 15)));

		long id = thingWithMapPropertiesRepository.save(t).getId();

		try (Session session = driver.session()) {
			Record r = session.readTransaction(tx -> tx.run("MATCH (t:MapProperties) WHERE id(t) = $id RETURN t",
					Collections.singletonMap("id", id)).single());
			Node n = r.get("t").asNode();
			assertThat(n.get("someDates.a").asObject()).isEqualTo(someDates.get("a"));
			assertThat(n.get("someDates.o").asObject()).isEqualTo(someDates.get("o"));
			assertThat(n.get("in_another_time.t").asObject()).isEqualTo(someOtherDates.get("t"));
			assertThat(n.get("customTypeMap.x").asString()).isEqualTo("c1");
			assertThat(n.get("someDatesByEnumA.VALUE_AA").asObject()).isEqualTo(LocalDate.of(2020, 10, 1));
			assertThat(n.get("someDatesByEnumB.VALUE_BA").asObject()).isEqualTo(LocalDate.of(2020, 10, 2));
			assertThat(n.get("someDatesByEnumB.VALUE_BB").asObject()).isEqualTo(LocalDate.of(2020, 10, 3));

			assertThat(n.get("datesWithTransformedKey.test").asObject()).isEqualTo(LocalDate.of(1979, 9, 21));
			assertThat(n.get("datesWithTransformedKeyAndEnum.value_ba").asObject()).isEqualTo(LocalDate.of(1938, 9, 15));
		}
	}

	@Test
	void customMapPropertiesShouldBeRead() {

		Map<String, LocalDate> someDates = new HashMap<>();
		someDates.put("someDates.a", LocalDate.of(2009, 12, 4));
		someDates.put("someDates.o", LocalDate.of(2013, 5, 6));

		Map<String, LocalDate> someOtherDates = Collections.singletonMap("in_another_time.t", LocalDate.of(1981, 7, 7));

		Map<String, String> someCustomThings = new HashMap<>();
		someCustomThings.put("customTypeMap.x", "c1");
		someCustomThings.put("customTypeMap.y", "c2");

		Map<String, Object> properties = new HashMap<>();
		someDates.forEach(properties::put);
		someOtherDates.forEach(properties::put);
		someCustomThings.forEach(properties::put);
		properties.put("someDatesByEnumA.VALUE_AA", LocalDate.of(2020, 10, 1));
		properties.put("someDatesByEnumB.VALUE_BA", LocalDate.of(2020, 10, 2));
		properties.put("someDatesByEnumB.VALUE_BB", LocalDate.of(2020, 10, 3));

		properties.put("datesWithTransformedKey.test", LocalDate.of(1979, 9, 21));
		properties.put("datesWithTransformedKeyAndEnum.value_ba", LocalDate.of(1938, 9, 15));

		Long id;
		try (Session session = driver.session()) {
			id = session.writeTransaction(
					tx -> tx.run("CREATE (t:MapProperties) SET t = $properties RETURN id(t)", Collections.singletonMap("properties", properties)).single().get(0)
							.asLong());
		}

		ThingWithMapProperties t = thingWithMapPropertiesRepository.findById(id).get();
		assertThat(t.getSomeDates()).containsOnlyKeys("a", "o");
		assertThat(t.getSomeDates())
				.containsValues(someDates.get("someDates.a"), someDates.get("someDates.o"));

		assertThat(t.getSomeOtherDates()).containsOnlyKeys("t");
		assertThat(t.getSomeOtherDates()).containsValues(LocalDate.of(1981, 7, 7));

		assertThat(t.getCustomTypeMap()).containsOnlyKeys("x", "y");
		assertThat(t.getCustomTypeMap().values()).extracting(ThingWithCustomTypes.CustomType::getValue).containsExactlyInAnyOrder("c1", "c2");

		assertThat(t.getSomeDatesByEnumA()).containsOnlyKeys(ThingWithMapProperties.EnumA.VALUE_AA);
		assertThat(t.getSomeDatesByEnumA()).containsValue(LocalDate.of(2020,10,1));

		assertThat(t.getSomeDatesByEnumA()).containsOnlyKeys(ThingWithMapProperties.EnumA.VALUE_AA);
		assertThat(t.getSomeDatesByEnumB().get(ThingWithMapProperties.EnumB.VALUE_BA)).isEqualTo(LocalDate.of(2020,10,2));
		assertThat(t.getSomeDatesByEnumB().get(ThingWithMapProperties.EnumB.VALUE_BB)).isEqualTo(LocalDate.of(2020,10,3));

		assertThat(t.getDatesWithTransformedKey()).containsEntry("TEST", LocalDate.of(1979, 9, 21));
		assertThat(t.getDatesWithTransformedKeyAndEnum()).containsEntry(ThingWithMapProperties.EnumB.VALUE_BA, LocalDate.of(1938, 9, 15));
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
					ThingWithAllAdditionalTypes hlp2 = additionalTypesRepository.findById(ID_OF_ADDITIONAL_TYPES_NODE).get();
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
							() -> assertThat(ReflectionTestUtils.getField(thing, a.getKey())).isEqualTo(a.getValue()))));

			DynamicContainer writes = DynamicContainer.dynamicContainer("write", entry.getValue().entrySet().stream()
					.map(a -> DynamicTest
							.dynamicTest(a.getKey(), () -> assertWrite(copyOfThing, a.getKey(), defaultConversionService))));

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

		try (Session session = neo4jConnectionSupport.getDriver().session()) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("id", id);
			parameters.put("attribute", fieldName);
			parameters.put("v", driverValue);

			long cnt = session.run("MATCH (n) WHERE id(n) = $id  AND n[$attribute] = $v RETURN COUNT(n) AS cnt", parameters)
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
				12, 0, 0, 0, ZoneId.of("Europe/Berlin")).toInstant().truncatedTo(ChronoUnit.DAYS)))).hasSizeGreaterThan(0);
	}

	public interface ConvertedIDsRepository extends Neo4jRepository<ThingWithUUIDID, UUID> {}

	public interface CypherTypesRepository extends Neo4jRepository<ThingWithAllCypherTypes, Long> {}

	public interface AdditionalTypesRepository extends Neo4jRepository<ThingWithAllAdditionalTypes, Long> {}

	public interface SpatialTypesRepository extends Neo4jRepository<ThingWithAllSpatialTypes, Long> {}

	public interface NonExistingPrimitivesRepository extends Neo4jRepository<ThingWithNonExistingPrimitives, Long> {}

	public interface CustomTypesRepository extends Neo4jRepository<ThingWithCustomTypes, Long> {

		List<ThingWithCustomTypes> findAllByDateAsString(Date theDate);
	}

	public interface ThingWithMapPropertiesRepository extends Neo4jRepository<ThingWithMapProperties, Long> {
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
	}
}
