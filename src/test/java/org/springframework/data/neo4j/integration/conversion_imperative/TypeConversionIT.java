/*
 * Copyright 2011-2022 the original author or authors.
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

import java.net.URL;
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
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.UserSelectionProvider;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.AllArgsCtorNoBuilder;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAllCypherTypes;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAllCypherTypes2;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAllSpatialTypes;
import org.springframework.data.neo4j.integration.shared.common.ThingWithUUIDID;
import org.springframework.data.neo4j.integration.shared.conversion.Neo4jConversionsITBase;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithAllAdditionalTypes;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCustomTypes;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @author Dennis Crissman
 * @soundtrack Tool - Fear Inoculum
 */
@Neo4jIntegrationTest
class TypeConversionIT extends Neo4jConversionsITBase {

	private final CypherTypesRepository cypherTypesRepository;

	private final AdditionalTypesRepository additionalTypesRepository;

	private final SpatialTypesRepository spatialTypesRepository;

	private final CustomTypesRepository customTypesRepository;

	private final DefaultConversionService defaultConversionService;

	@Autowired TypeConversionIT(CypherTypesRepository cypherTypesRepository,
			AdditionalTypesRepository additionalTypesRepository, SpatialTypesRepository spatialTypesRepository,
			CustomTypesRepository customTypesRepository,
			Neo4jConversions neo4jConversions) {
		this.cypherTypesRepository = cypherTypesRepository;
		this.additionalTypesRepository = additionalTypesRepository;
		this.spatialTypesRepository = spatialTypesRepository;
		this.customTypesRepository = customTypesRepository;
		this.defaultConversionService = new DefaultConversionService();
		neo4jConversions.registerConvertersIn(defaultConversionService);
	}

	@Test
	void thereShallBeNoDefaultValuesForNonExistingAttributes(@Autowired Neo4jTemplate template) {

		Long id;
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {

			id = session.executeWrite(tx -> tx.run("CREATE (n:AllArgsCtorNoBuilder) RETURN id(n)").single().get(0).asLong());
			bookmarkCapture.seedWith(session.lastBookmarks());
		}

		assertThatExceptionOfType(MappingException.class)
				.isThrownBy(() -> template.findById(id, AllArgsCtorNoBuilder.class))
				.withMessageMatching("Error mapping Record<\\{.+: .*>")
				.withRootCauseInstanceOf(IllegalArgumentException.class)
				.withStackTraceContaining("Parameter aBoolean must not be null");
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
				case "CypherTypes" -> {
					ThingWithAllCypherTypes hlp = cypherTypesRepository.findById(ID_OF_CYPHER_TYPES_NODE).get();
					copyOfThing = cypherTypesRepository.save(hlp.withId(null));
					thing = hlp;
				}
				case "AdditionalTypes" -> {
					ThingWithAllAdditionalTypes hlp2 = additionalTypesRepository.findById(ID_OF_ADDITIONAL_TYPES_NODE)
							.get();
					copyOfThing = additionalTypesRepository.save(hlp2.withId(null));
					thing = hlp2;
				}
				case "SpatialTypes" -> {
					ThingWithAllSpatialTypes hlp3 = spatialTypesRepository.findById(ID_OF_SPATIAL_TYPES_NODE).get();
					copyOfThing = spatialTypesRepository.save(hlp3.withId(null));
					thing = hlp3;
				}
				case "CustomTypes" -> {
					ThingWithCustomTypes hlp4 = customTypesRepository.findById(ID_OF_CUSTOM_TYPE_NODE).get();
					copyOfThing = customTypesRepository.save(hlp4.withId(null));
					thing = hlp4;
				}
				default -> throw new UnsupportedOperationException("Unsupported types: " + entry.getKey());
			}

			DynamicContainer reads = DynamicContainer.dynamicContainer("read",
					entry.getValue().entrySet().stream().map(a -> DynamicTest.dynamicTest(a.getKey(),
							() -> {
								Object actual = ReflectionTestUtils.getField(thing, a.getKey());
								Object expected = a.getValue();
								if (actual instanceof URL && expected instanceof URL) {
									// The host has been chosen to avoid interaction with the URLStreamHandler
									// Should be enough for our comparision.
									actual = ((URL) actual).getHost();
									expected = ((URL) expected).getHost();
								}
								assertThat(actual).isEqualTo(expected);
							})));

			DynamicContainer writes = DynamicContainer.dynamicContainer("write", entry.getValue().keySet().stream()
					.map(o -> DynamicTest
							.dynamicTest(o,
									() -> assertWrite(copyOfThing, o, defaultConversionService))));

			return DynamicContainer.dynamicContainer(entry.getKey(), Arrays.asList(reads, writes));
		});
	}

	void assertWrite(Object thing, String fieldName, ConversionService conversionService) {

		long id = (long) ReflectionTestUtils.getField(thing, "id");
		Object domainValue = ReflectionTestUtils.getField(thing, fieldName);

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
			Object[] targetCollection = (sourceCollection).stream().map(conversion).toArray();
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

	@Test // GH-2348
	void nonExistingPrimitivesShouldNotFailWithFieldAccess(@Autowired Neo4jTemplate template) {
		Long id;
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {

			id = session.executeWrite(tx -> tx.run("CREATE (n:ThingWithAllCypherTypes2) RETURN id(n)").single().get(0).asLong());
			bookmarkCapture.seedWith(session.lastBookmarks());
		}

		Optional<ThingWithAllCypherTypes2> optionalResult = template.findById(id, ThingWithAllCypherTypes2.class);
		assertThat(optionalResult).hasValueSatisfying(result -> {
			assertThat(result.isABoolean()).isFalse();
			assertThat(result.getALong()).isEqualTo(0L);
			assertThat(result.getAnInt()).isEqualTo(0);
			assertThat(result.getADouble()).isEqualTo(0.0);
			assertThat(result.getAString()).isNull();
			assertThat(result.getAByteArray()).isNull();
			assertThat(result.getALocalDate()).isNull();
			assertThat(result.getAnOffsetTime()).isNull();
			assertThat(result.getALocalTime()).isNull();
			assertThat(result.getAZoneDateTime()).isNull();
			assertThat(result.getALocalDateTime()).isNull();
			assertThat(result.getAnIsoDuration()).isNull();
			assertThat(result.getAPoint()).isNull();
			assertThat(result.getAZeroPeriod()).isNull();
			assertThat(result.getAZeroDuration()).isNull();
		});
	}

	@Test // GH-2594
	void clientShouldUseCustomType(@Autowired Neo4jClient client) {

		Optional<ThingWithCustomTypes.CustomType> value = client.query("RETURN 'whatever'")
				.fetchAs(ThingWithCustomTypes.CustomType.class).first();
		assertThat(value).map(ThingWithCustomTypes.CustomType::getValue).hasValue("whatever");
	}

	public interface ConvertedIDsRepository extends Neo4jRepository<ThingWithUUIDID, UUID> {
	}

	public interface CypherTypesRepository extends Neo4jRepository<ThingWithAllCypherTypes, Long> {
	}

	public interface AdditionalTypesRepository extends Neo4jRepository<ThingWithAllAdditionalTypes, Long> {
	}

	public interface SpatialTypesRepository extends Neo4jRepository<ThingWithAllSpatialTypes, Long> {
	}

	public interface CustomTypesRepository extends Neo4jRepository<ThingWithCustomTypes, Long> {

		List<ThingWithCustomTypes> findAllByDateAsString(Date theDate);
	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends Neo4jImperativeTestConfiguration {

		@Autowired
		private ObjectProvider<UserSelectionProvider> userSelectionProviders;

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public Neo4jConversions neo4jConversions() {
			return new Neo4jConversions(Collections.singleton(new ThingWithCustomTypes.CustomTypeConverter()));
		}

		@Override
		public Neo4jClient neo4jClient(Driver driver, DatabaseSelectionProvider databaseSelectionProvider) {

			return Neo4jClient.with(driver)
					.withDatabaseSelectionProvider(databaseSelectionProvider)
					.withUserSelectionProvider(userSelectionProviders.getIfUnique())
					.withNeo4jConversions(neo4jConversions())
					.build();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return Neo4jConversionsITBase.bookmarkCapture;
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
