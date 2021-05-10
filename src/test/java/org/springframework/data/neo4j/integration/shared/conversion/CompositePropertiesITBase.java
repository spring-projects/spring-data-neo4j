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
package org.springframework.data.neo4j.integration.shared.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.data.neo4j.integration.shared.common.Club;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;

/**
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Learning English, Lesson Two
 */
public abstract class CompositePropertiesITBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Function<Map.Entry<String, Object>, String> newKey = e -> e.getKey()
			.substring(e.getKey().indexOf(".") + 1);

	protected final Driver driver;

	private final BookmarkCapture bookmarkCapture;

	protected final Map<String, Object> nodeProperties;

	protected final Map<String, Object> relationshipProperties;

	protected CompositePropertiesITBase(Driver driver, BookmarkCapture bookmarkCapture) {

		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;

		Map<String, Object> properties = new HashMap<>();
		Map<String, LocalDate> someDates = new HashMap<>();
		someDates.put("someDates.a", LocalDate.of(2009, 12, 4));
		someDates.put("someDates.o", LocalDate.of(2013, 5, 6));

		Map<String, LocalDate> someOtherDates = Collections.singletonMap("in_another_time.t", LocalDate.of(1981, 7, 7));

		Map<String, String> someCustomThings = new HashMap<>();
		someCustomThings.put("customTypeMap.x", "c1");
		someCustomThings.put("customTypeMap.y", "c2");

		someDates.forEach(properties::put);
		someOtherDates.forEach(properties::put);
		someCustomThings.forEach(properties::put);
		properties.put("someDatesByEnumA.VALUE_AA", LocalDate.of(2020, 10, 1));
		properties.put("someDatesByEnumB.VALUE_BA", LocalDate.of(2020, 10, 2));
		properties.put("someDatesByEnumB.VALUE_BB", LocalDate.of(2020, 10, 3));

		properties.put("datesWithTransformedKey.test", LocalDate.of(1979, 9, 21));
		properties.put("datesWithTransformedKeyAndEnum.value_ba", LocalDate.of(1938, 9, 15));

		properties.put("dto.x", "A");
		properties.put("dto.y", 1L);
		properties.put("dto.z", 4.2);
		nodeProperties = Collections.unmodifiableMap(properties);

		properties = new HashMap<>();
		properties.put("someProperties.a", "B");
		properties.put("dto.x", "B");
		properties.put("dto.y", 10L);
		properties.put("dto.z", 42.0);
		relationshipProperties = Collections.unmodifiableMap(properties);
	}

	protected long createNodeWithCompositeProperties() {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long id = session.writeTransaction(
					tx -> tx.run("CREATE (t:CompositeProperties) SET t = $properties RETURN id(t)",
							Collections.singletonMap("properties", nodeProperties)).single().get(0)
							.asLong());
			bookmarkCapture.seedWith(session.lastBookmark());
			return id;
		}
	}

	protected long createRelationshipWithCompositeProperties() {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long id = session.writeTransaction(
					tx -> tx.run(
							"CREATE (t:CompositeProperties) -[r:IRRELEVANT_TYPE] -> (:Club) SET r = $properties RETURN id(t)",
							Collections.singletonMap("properties", relationshipProperties)).single().get(0)
							.asLong());
			bookmarkCapture.seedWith(session.lastBookmark());
			return id;
		}
	}

	protected ThingWithCompositeProperties newEntityWithCompositeProperties() {

		ThingWithCompositeProperties t = new ThingWithCompositeProperties();
		Map<String, LocalDate> someDates = new HashMap<>();
		nodeProperties.entrySet().stream().filter(e -> e.getKey().startsWith("someDates."))
				.forEach(e -> someDates.put(newKey.apply(e), (LocalDate) e.getValue()));
		t.setSomeDates(someDates);

		Map<String, LocalDate> someOtherDates = Collections
				.singletonMap("t", (LocalDate) nodeProperties.get("in_another_time.t"));
		t.setSomeOtherDates(someOtherDates);

		Map<String, ThingWithCustomTypes.CustomType> someCustomThings = new HashMap<>();
		nodeProperties.entrySet().stream().filter(e -> e.getKey().startsWith("customTypeMap."))
				.forEach(e -> someCustomThings
						.put(newKey.apply(e), ThingWithCustomTypes.CustomType.of((String) e.getValue())));
		t.setCustomTypeMap(someCustomThings);

		Map<ThingWithCompositeProperties.EnumA, LocalDate> someDatesByEnumA = new HashMap<>();
		nodeProperties.entrySet().stream().filter(e -> e.getKey().startsWith("someDatesByEnumA."))
				.forEach(e -> someDatesByEnumA
						.put(ThingWithCompositeProperties.EnumA.valueOf(newKey.apply(e)), (LocalDate) e.getValue()));
		t.setSomeDatesByEnumA(someDatesByEnumA);

		Map<ThingWithCompositeProperties.EnumB, LocalDate> someDatesByEnumB = new HashMap<>();
		nodeProperties.entrySet().stream().filter(e -> e.getKey().startsWith("someDatesByEnumB."))
				.forEach(e -> someDatesByEnumB
						.put(ThingWithCompositeProperties.EnumB.valueOf(newKey.apply(e)), (LocalDate) e.getValue()));
		t.setSomeDatesByEnumB(someDatesByEnumB);

		t.setDatesWithTransformedKey(Collections.singletonMap("TEST", (LocalDate) nodeProperties.get("datesWithTransformedKey.test")));
		t.setDatesWithTransformedKeyAndEnum(Collections.singletonMap(ThingWithCompositeProperties.EnumB.VALUE_BA, (LocalDate) nodeProperties.get("datesWithTransformedKeyAndEnum.value_ba")));

		t.setSomeOtherDTO(new ThingWithCompositeProperties.SomeOtherDTO(
				(String) nodeProperties.get("dto.x"),
				(Long) nodeProperties.get("dto.y"),
				(Double) nodeProperties.get("dto.z")
		));
		return t;
	}

	protected ThingWithCompositeProperties newEntityWithRelationshipWithCompositeProperties() {

		ThingWithCompositeProperties source = new ThingWithCompositeProperties();
		Club target = new Club();
		RelationshipWithCompositeProperties relationshipWithCompositeProperties = new RelationshipWithCompositeProperties(
				target);

		Map<String, String> setSomeProperties = new HashMap<>();
		nodeProperties.entrySet().stream().filter(e -> e.getKey().startsWith("someProperties."))
				.forEach(e -> setSomeProperties.put(newKey.apply(e), (String) e.getValue()));
		relationshipWithCompositeProperties.setSomeProperties(setSomeProperties);

		relationshipWithCompositeProperties.setSomeProperties(Collections.singletonMap("a", "B"));
		relationshipWithCompositeProperties.setSomeOtherDTO(new ThingWithCompositeProperties.SomeOtherDTO(
				(String) relationshipProperties.get("dto.x"),
				(Long) relationshipProperties.get("dto.y"),
				(Double) relationshipProperties.get("dto.z")
		));

		source.setRelationship(relationshipWithCompositeProperties);
		return source;
	}

	protected void assertNodePropertiesOn(ThingWithCompositeProperties t) {

		Map<String, Object> mergedProperties = new HashMap<>();

		t.getSomeDates().forEach((k, v) -> mergedProperties.put("someDates." + k, v));
		t.getSomeOtherDates().forEach((k, v) -> mergedProperties.put("in_another_time." + k, v));
		t.getCustomTypeMap().forEach((k, v) -> mergedProperties.put("customTypeMap." + k, v.getValue()));
		t.getSomeDatesByEnumA().forEach((k, v) -> mergedProperties.put("someDatesByEnumA." + k.name(), v));
		t.getSomeDatesByEnumB().forEach((k, v) -> mergedProperties.put("someDatesByEnumB." + k.name(), v));

		mergedProperties.put("datesWithTransformedKey.test", t.getDatesWithTransformedKey().get("TEST"));
		mergedProperties.put("datesWithTransformedKeyAndEnum.value_ba",
				t.getDatesWithTransformedKeyAndEnum().get(ThingWithCompositeProperties.EnumB.VALUE_BA));

		mergedProperties.put("dto.x", t.getSomeOtherDTO().x);
		mergedProperties.put("dto.y", t.getSomeOtherDTO().y);
		mergedProperties.put("dto.z", t.getSomeOtherDTO().z);

		assertThat(mergedProperties).containsExactlyInAnyOrderEntriesOf(nodeProperties);
	}

	protected void assertNodePropertiesInGraph(long id) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Record r = session.readTransaction(tx -> tx.run("MATCH (t:CompositeProperties) WHERE id(t) = $id RETURN t",
					Collections.singletonMap("id", id)).single());
			Node n = r.get("t").asNode();
			assertThat(n.asMap()).containsExactlyInAnyOrderEntriesOf(nodeProperties);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	protected void assertRelationshipPropertiesOn(ThingWithCompositeProperties t) {

		assertThat(t.getRelationship()).isNotNull();
		assertThat(t.getRelationship()).satisfies(rel -> {

			Map<String, Object> mergedProperties = new HashMap<>();

			rel.getSomeProperties().forEach((k, v) -> mergedProperties.put("someProperties." + k, v));

			mergedProperties.put("dto.x", rel.getSomeOtherDTO().x);
			mergedProperties.put("dto.y", rel.getSomeOtherDTO().y);
			mergedProperties.put("dto.z", rel.getSomeOtherDTO().z);
		});
	}

	protected void assertRelationshipPropertiesInGraph(long id) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Record r = session.readTransaction(
					tx -> tx.run("MATCH (t:CompositeProperties) - [r:IRRELEVANT_TYPE] -> () WHERE id(t) = $id RETURN r",
							Collections.singletonMap("id", id)).single());
			Relationship rel = r.get("r").asRelationship();
			assertThat(rel.asMap()).containsExactlyInAnyOrderEntriesOf(relationshipProperties);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}
}
