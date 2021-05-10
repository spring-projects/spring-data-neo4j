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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.ConverterBuilder;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.integration.shared.conversion.Neo4jConversionsITBase;
import org.springframework.data.neo4j.test.Neo4jExtension;

/**
 * @author Michael J. Simons
 */
@ExtendWith(Neo4jExtension.class)
class Neo4jConversionsIT extends Neo4jConversionsITBase {

	private static final TypeDescriptor TYPE_DESCRIPTOR_OF_VALUE = TypeDescriptor.valueOf(Value.class);
	private static final DefaultConversionService DEFAULT_CONVERSION_SERVICE = new DefaultConversionService();

	@BeforeAll
	static void prepareDefaultConversionService() {
		new Neo4jConversions().registerConvertersIn(DEFAULT_CONVERSION_SERVICE);
	}

	@TestFactory
	@DisplayName("Objects")
	Stream<DynamicNode> objects() {
		Map<String, Map<String, Object>> supportedTypes = new HashMap<>();
		supportedTypes.put("CypherTypes", CYPHER_TYPES);
		supportedTypes.put("AdditionalTypes", ADDITIONAL_TYPES);
		supportedTypes.put("SpatialTypes", SPATIAL_TYPES);

		return supportedTypes.entrySet().stream().map(types -> {

			DynamicContainer reads = DynamicContainer.dynamicContainer("read", types.getValue().entrySet().stream().map(
					a -> DynamicTest
							.dynamicTest(a.getKey(), () -> Neo4jConversionsIT.assertRead(types.getKey(), a.getKey(), a.getValue()))));

			DynamicContainer writes = DynamicContainer.dynamicContainer("write",
					types.getValue().entrySet().stream().map(a -> DynamicTest.dynamicTest(a.getKey(),
							() -> Neo4jConversionsIT.assertWrite(types.getKey(), a.getKey(), a.getValue()))));

			return DynamicContainer.dynamicContainer(types.getKey(), Arrays.asList(reads, writes));
		});
	}

	@TestFactory
	@DisplayName("Custom conversions")
	Stream<DynamicTest> customConversions() {
		final DefaultConversionService customConversionService = new DefaultConversionService();

		ConverterBuilder.ConverterAware converterAware = ConverterBuilder.reading(Value.class, LocalDate.class, v -> {
			String s = v.asString();
			switch (s) {
				case "gestern":
					return LocalDate.now().minusDays(1);
				case "heute":
					return LocalDate.now();
				case "morgen":
					return LocalDate.now().plusDays(1);
				default:
					throw new IllegalArgumentException();
			}
		}).andWriting(d -> {
			if (d.isBefore(LocalDate.now())) {
				return Values.value("gestern");
			} else if (d.isAfter(LocalDate.now())) {
				return Values.value("morgen");
			} else {
				return Values.value("heute");
			}
		});
		new Neo4jConversions(converterAware.getConverters()).registerConvertersIn(customConversionService);

		return Stream.of(
				DynamicTest.dynamicTest("read",
						() -> assertThat(customConversionService.convert(Values.value("gestern"), LocalDate.class))
								.isEqualTo(LocalDate.now().minusDays(1))),
				DynamicTest.dynamicTest("write",
						() -> assertThat(customConversionService.convert(LocalDate.now().plusDays(1), TYPE_DESCRIPTOR_OF_VALUE))
								.isEqualTo(Values.value("morgen"))));
	}

	@Nested
	class Primitives {

		@Test
		void cypherTypes() {
			boolean b = DEFAULT_CONVERSION_SERVICE.convert(Values.value(true), boolean.class);
			assertThat(b).isEqualTo(true);

			long l = DEFAULT_CONVERSION_SERVICE.convert(Values.value(Long.MAX_VALUE), long.class);
			assertThat(l).isEqualTo(Long.MAX_VALUE);

			double d = DEFAULT_CONVERSION_SERVICE.convert(Values.value(1.7976931348), double.class);
			assertThat(d).isEqualTo(1.7976931348);
		}

		@Test
		void additionalTypes() {

			byte b = DEFAULT_CONVERSION_SERVICE.convert(Values.value(new byte[] { 6 }), byte.class);
			assertThat(b).isEqualTo((byte) 6);

			char c = DEFAULT_CONVERSION_SERVICE.convert(Values.value("x"), char.class);
			assertThat(c).isEqualTo('x');

			float f = DEFAULT_CONVERSION_SERVICE.convert(Values.value("23.42"), float.class);
			assertThat(f).isEqualTo(23.42F);

			int i = DEFAULT_CONVERSION_SERVICE.convert(Values.value(42), int.class);
			assertThat(i).isEqualTo(42);

			short s = DEFAULT_CONVERSION_SERVICE.convert(Values.value((short) 127), short.class);
			assertThat(s).isEqualTo((short) 127);
		}
	}

	static void assertRead(String label, String attribute, Object t) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			Value v = session.run("MATCH (n) WHERE labels(n) = [$label] RETURN n[$attribute] as r",
					Values.parameters("label", label, "attribute", attribute)).single().get("r");

			TypeDescriptor typeDescriptor = TypeDescriptor.forObject(t);
			if (typeDescriptor.isCollection()) {
				Collection<?> collection = (Collection<?>) t;
				Class<?> targetType = collection.stream().map(Object::getClass).findFirst().get();
				List<Object> convertedObjects = v.asList(elem -> DEFAULT_CONVERSION_SERVICE.convert(elem, targetType));
				assertThat(convertedObjects).containsAll(collection);
			} else {
				Object converted = DEFAULT_CONVERSION_SERVICE.convert(v, typeDescriptor.getType());
				assertThat(converted).isEqualTo(t);
			}
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	static void assertWrite(String label, String attribute, Object t) {

		Value driverValue;
		if (t != null && Collection.class.isAssignableFrom(t.getClass())) {
			Collection<?> sourceCollection = (Collection<?>) t;
			Object[] targetCollection = (sourceCollection).stream()
					.map(element -> DEFAULT_CONVERSION_SERVICE.convert(element, Value.class)).toArray();
			driverValue = Values.value(targetCollection);
		} else {
			driverValue = DEFAULT_CONVERSION_SERVICE.convert(t, Value.class);
		}

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("label", label);
			parameters.put("attribute", attribute);
			parameters.put("v", driverValue);

			long cnt = session
					.run("MATCH (n) WHERE labels(n) = [$label]  AND n[$attribute] = $v RETURN COUNT(n) AS cnt", parameters)
					.single().get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}
}
