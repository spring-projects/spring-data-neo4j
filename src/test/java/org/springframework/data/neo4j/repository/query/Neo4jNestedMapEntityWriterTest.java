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
package org.springframework.data.neo4j.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.springframework.data.annotation.Version;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.integration.issues.gh2323.Knows;
import org.springframework.data.neo4j.integration.issues.gh2323.Language;
import org.springframework.data.neo4j.integration.issues.gh2323.Person;
import org.springframework.util.ReflectionUtils;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Neo4jNestedMapEntityWriterTest {

	private final Neo4jMappingContext mappingContext;
	private final Condition<Object> isAMap = new Condition<>(Map.class::isInstance, "a map");
	private final Condition<Object> isAMapValue = new Condition<>(
			o -> o instanceof Value && InternalTypeSystem.TYPE_SYSTEM.MAP().isTypeOf((Value) o), "a map value");
	private final Condition<Object> isAListValue = new Condition<>(
			o -> o instanceof Value && InternalTypeSystem.TYPE_SYSTEM.LIST().isTypeOf((Value) o), "a list value");

	Neo4jNestedMapEntityWriterTest() {

		this.mappingContext = new Neo4jMappingContext(new Neo4jConversions(), InternalTypeSystem.TYPE_SYSTEM);
		this.mappingContext.setInitialEntitySet(new HashSet<>(
				Arrays.asList(
						FlatEntity.class,
						FlatEntityWithAdditionalTypes.class,
						FlatEntityWithDynamicLabels.class,
						GraphPropertyNamesShouldBeUsed.class,
						A.class, B.class,
						A2.class, B2.class,
						A3.class, A4.class, A5.class, A6.class, A7.class, A8.class,
						Person.class, Knows.class, Language.class
				)
		));
		this.mappingContext.initialize();
	}

	@Test // DATAGRAPH-1452
	void shouldFailGracefully() {

		FlatEntity entity = new FlatEntity(4711L, 47.11, "4711");

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(new Neo4jMappingContext());
		assertThatExceptionOfType(MappingException.class)
				.isThrownBy(() -> writer.write(entity, new HashMap<>()))
				.withMessageMatching("Cannot write unknown entity of type '.+' into a map\\.");
	}

	@Test // GH-2323
	void propertiesOfRelationshipsShouldStillWorkAfterHandlingFirstResultDifferent() {

		Knows knows = new Knows("Some description", new Language("German"));
		Person p = new Person("F");
		ReflectionUtils.setField(ReflectionUtils.findField(Person.class, "id"), p, "xxx");
		p.getKnownLanguages().add(knows);

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(this.mappingContext);
		Map<String, Object> result = toMap(writer, p);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString())).containsExactlyInAnyOrder("Person");
		assertThat(result).containsEntry("__id__", Values.value("xxx"));

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Value> properties = (Map<String, Value>) result.get("__properties__");

		assertThat(properties).hasEntrySatisfying("KNOWS", isAListValue);
		List<Value> rels = properties.get("KNOWS").asList(Function.identity());
		properties = rels.get(0).get("__properties__").asMap(Function.identity());
		assertThat(properties).containsEntry("description", Values.value("Some description"));

		properties = rels.get(0).get("__target__").asMap(Function.identity());
		assertThat(properties).containsEntry("__id__", Values.value("German"));
		assertThat(properties).hasEntrySatisfying("__properties__", isAMapValue);
	}

	@Test // GH-2323
	void relationshipPropertiesShouldBeSerializedWithTargetNodeWhenPassedFirstToWriterToo() {

		Knows knows = new Knows("Some description", new Language("German"));
		ReflectionUtils.setField(ReflectionUtils.findField(Knows.class, "id"), knows, 4711L);

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(this.mappingContext);
		Map<String, Object> result = toMap(writer, knows);

		assertThat(result).containsEntry("__id__", Values.value(4711L));

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Value> properties = (Map<String, Value>) result.get("__properties__");
		assertThat(properties).containsEntry("description", Values.value("Some description"));

		assertThat(properties).hasEntrySatisfying("__target__", isAMapValue);
		properties = properties.get("__target__").asMap(Function.identity());
		assertThat(properties).containsEntry("__id__", Values.value("German"));
		assertThat(properties).hasEntrySatisfying("__properties__", isAMapValue);
	}

	@Test // DATAGRAPH-1452
	void flatEntityShouldWork() {

		FlatEntity entity = new FlatEntity(4711L, 47.11, "4711");

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, entity);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString())).containsExactlyInAnyOrder("FlatEntity");

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Object> properties = (Map<String, Object>) result.get("__properties__");
		assertThat(result).containsEntry("__id__", Values.value(4711L));
		assertThat(properties).containsEntry("aDouble", Values.value(47.11));
		assertThat(properties).containsEntry("aString", Values.value("4711"));
	}

	@Test // DATAGRAPH-1452
	void additionalTypesShouldWork() {

		FlatEntityWithAdditionalTypes entity = new FlatEntityWithAdditionalTypes("TheId", 123L, Locale.FRENCH,
				URI.create("https://info.michael-simons.eu"), SomeEnum.A,
				Collections.singletonList(47.11));

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, entity);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString())).containsExactlyInAnyOrder("FlatEntityWithAdditionalTypes");

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Object> properties = (Map<String, Object>) result.get("__properties__");
		assertThat(result).containsEntry("__id__", Values.value("TheId"));
		assertThat(properties).containsEntry("aLocale", Values.value("fr"));
		assertThat(properties).containsEntry("aURI", Values.value("https://info.michael-simons.eu"));
		assertThat(properties).containsEntry("someEnum", Values.value("A"));
		assertThat(properties).containsEntry("listOfDoubles", Values.value(Collections.singletonList(47.11)));
	}

	@Test // DATAGRAPH-1452
	void dynamicLabelsShouldWork() {

		FlatEntityWithDynamicLabels entity = new FlatEntityWithDynamicLabels("TheId",
				Arrays.asList("Label1", "Label2"));

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, entity);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString()))
				.containsExactlyInAnyOrder("FlatEntityWithDynamicLabels", "Label1", "Label2");

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Object> properties = (Map<String, Object>) result.get("__properties__");
		assertThat(result).containsEntry("__id__", Values.value("TheId"));
		assertThat(properties).isEmpty();
	}

	@Test // DATAGRAPH-1452
	void simpleRelationsShouldWork() {

		B b1 = new B("bI");
		B b2 = new B("bII");
		B b3 = new B("bIII");

		A entity = new A("a", b1, Arrays.asList(b2, b3));

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, entity);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString()))
				.containsExactlyInAnyOrder("A");

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Object> properties = (Map<String, Object>) result.get("__properties__");

		assertThat(result).containsEntry("__id__", Values.value("a"));

		assertThat(properties).hasEntrySatisfying("HAS_B", isAListValue);
		Map<String, Value> hasBMap = ((Value) properties.get("HAS_B")).get(0).asMap(Function.identity());
		assertThat(hasBMap.get("__id__")).isEqualTo(Values.value("bI"));

		assertThat(properties).hasEntrySatisfying("HAS_MORE_B", isAListValue);
		List<Value> hasMoreBs = ((Value) properties.get("HAS_MORE_B")).asList(Function.identity());
		assertThat(hasMoreBs).extracting(v -> v.get("__id__").asString()).containsExactly("bII", "bIII");
	}

	@Test // DATAGRAPH-1452
	void dynamicRelationshipsShouldWork() {

		B bI = new B("bI");
		B bII = new B("bII");
		B2 b2I = new B2("b2I");
		B2 b2II = new B2("b2II");
		B2 b2III = new B2("b2III");

		Map<String, B> rels = new LinkedHashMap<>();
		rels.put("brel1a", bI);
		rels.put("brel1b", bI);
		rels.put("brel2", bII);

		Map<String, List<B2>> rels2 = new LinkedHashMap<>();
		rels2.put("b2rel1", Arrays.asList(b2I, b2II));
		rels2.put("b2rel2", Arrays.asList(b2III));
		A3 entity = new A3("a", rels, rels2);

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, entity);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString()))
				.containsExactlyInAnyOrder("A3");

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Object> properties = (Map<String, Object>) result.get("__properties__");

		assertThat(result).containsEntry("__id__", Values.value("a"));

		assertThat(properties).hasEntrySatisfying("brel1a", isAListValue);
		Map<String, Value> nested = ((Value) properties.get("brel1a")).get(0).asMap(Function.identity());
		assertThat(nested).hasEntrySatisfying("__properties__", isAMapValue);
		assertThat(nested.get("__id__")).isEqualTo(Values.value("bI"));

		assertThat(properties).hasEntrySatisfying("brel1b", isAListValue);
		nested = ((Value) properties.get("brel1b")).get(0).asMap(Function.identity());
		assertThat(nested.get("__ref__")).isEqualTo(Values.value("bI"));

		assertThat(properties).hasEntrySatisfying("brel2", isAListValue);
		nested = ((Value) properties.get("brel2")).get(0).asMap(Function.identity());
		assertThat(nested).hasEntrySatisfying("__properties__", isAMapValue);
		assertThat(nested.get("__id__")).isEqualTo(Values.value("bII"));

		assertThat(properties).hasEntrySatisfying("b2rel1", isAListValue);
		nested = ((Value) properties.get("b2rel1")).get(0).asMap(Function.identity());
		assertThat(nested).hasEntrySatisfying("__properties__", isAMapValue);
		assertThat(nested.get("__id__")).isEqualTo(Values.value("b2I"));

		nested = ((Value) properties.get("b2rel1")).get(1).asMap(Function.identity());
		assertThat(nested).hasEntrySatisfying("__properties__", isAMapValue);
		assertThat(nested.get("__id__")).isEqualTo(Values.value("b2II"));

		assertThat(properties).hasEntrySatisfying("b2rel2", isAListValue);
		nested = ((Value) properties.get("b2rel2")).get(0).asMap(Function.identity());
		assertThat(nested).hasEntrySatisfying("__properties__", isAMapValue);
		assertThat(nested.get("__id__")).isEqualTo(Values.value("b2III"));
	}

	@Test // DATAGRAPH-1452
	void shouldStopAtSeenObjects() {

		B2 b1 = new B2("b2I");

		A2 entity = new A2("a2I", b1);
		b1.belongsToA2 = entity;
		A2 a2 = new A2("a2II", new B2("b2II"));
		b1.knows = Arrays.asList(entity, a2);

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, entity);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString()))
				.containsExactlyInAnyOrder("A2");

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, ?> properties = (Map<String, Object>) result.get("__properties__");

		assertThat(result).containsEntry("__id__", Values.value("a2I"));

		assertThat(properties).hasEntrySatisfying("HAS_B", isAListValue);
		List<Value> hasBs = ((Value) properties.get("HAS_B")).asList(Function.identity());
		assertThat(hasBs).hasSize(1);
		Map<String, Value> hasBMap = hasBs.get(0).asMap(Function.identity());
		assertThat(hasBMap).containsEntry("__id__", Values.value("b2I"));
		assertThat(hasBMap).hasEntrySatisfying("__properties__", isAMapValue);

		properties = hasBMap.get("__properties__").asMap(Function.identity());
		assertThat(properties).hasEntrySatisfying("KNOWS", isAListValue);
		List<Value> rels = ((Value) properties.get("KNOWS")).asList(Function.identity());
		assertThat(rels).first().satisfies(v -> assertThat(v.get("__ref__").asString()).isEqualTo("a2I"));
		assertThat(rels).last().satisfies(v -> {
			Map<String, Value> nestedProperties = v.get("__properties__").asMap(Function.identity());
			assertThat(nestedProperties).containsOnlyKeys("HAS_B");
			assertThat(nestedProperties.get("HAS_B").asList(Function.identity()).get(0).asMap(Function.identity()))
					.doesNotContainKey("KNOWS");
		});
	}

	@Test // DATAGRAPH-1452
	void oneToOneRelationshipWithPropertiesShouldWork() {

		final A4 a = new A4("a4I");
		final B3 b = new B3("b3I");
		a.p1 = new P1("v1", "v2", b);

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, a);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString())).containsExactlyInAnyOrder("A4");

		assertThat(result).containsEntry("__id__", Values.value("a4I"));

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Value> properties = (Map<String, Value>) result.get("__properties__");

		assertThat(properties).hasEntrySatisfying("HAS_P1", isAListValue);
		List<Value> rels = properties.get("HAS_P1").asList(Function.identity());

		properties = rels.get(0).get("__properties__").asMap(Function.identity());
		assertThat(properties).containsEntry("prop1", Values.value("v1"));
		assertThat(properties).containsEntry("prop2", Values.value("v2"));

		properties = rels.get(0).get("__target__").asMap(Function.identity());
		assertThat(properties).containsEntry("__id__", Values.value("b3I"));
		assertThat(properties).hasEntrySatisfying("__properties__", isAMapValue);
	}

	@Test // DATAGRAPH-1452
	void oneToManyRelationshipWithPropertiesShouldWork() {

		final A5 a = new A5("a5I");
		a.p1 = Arrays.asList(
				new P1("v0", "v1", new B3("b3I")),
				new P1("v2", "v3", new B3("b3II"))
		);

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, a);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString())).containsExactlyInAnyOrder("A5");

		assertThat(result).containsEntry("__id__", Values.value("a5I"));

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Value> properties = (Map<String, Value>) result.get("__properties__");

		assertThat(properties).hasEntrySatisfying("HAS_P1", isAListValue);
		List<Value> rels = properties.get("HAS_P1").asList(Function.identity());
		String[] suffix = new String[] { "I", "II" };
		for (int i = 0; i < suffix.length; ++i) {
			properties = rels.get(i).get("__properties__").asMap(Function.identity());
			assertThat(properties).containsEntry("prop1", Values.value("v" + (i * 2 + 0)));
			assertThat(properties).containsEntry("prop2", Values.value("v" + (i * 2 + 1)));

			properties = rels.get(i).get("__target__").asMap(Function.identity());
			assertThat(properties).containsEntry("__id__", Values.value("b3" + suffix[i]));
			assertThat(properties).hasEntrySatisfying("__properties__", isAMapValue);
		}
	}

	@Test // DATAGRAPH-1452
	void dynamicRelationshipWithProperties() {

		final A6 a = new A6("a6I");
		a.p1 = new HashMap<>();
		a.p1.put("rel1", new P1("v0", "v1", new B3("b3I")));
		a.p1.put("rel2", new P1("v2", "v3", new B3("b3II")));

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, a);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString())).containsExactlyInAnyOrder("A6");

		assertThat(result).containsEntry("__id__", Values.value("a6I"));

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Value> properties = (Map<String, Value>) result.get("__properties__");

		String[] rels = new String[] { "rel1", "rel2" };
		for (int i = 0; i < rels.length; i++) {
			String rel = rels[i];

			assertThat(properties).hasEntrySatisfying(rel, isAListValue);
			List<Value> relValue = properties.get(rel).asList(Function.identity());

			assertThat(relValue).hasSize(1);
			Map<String, Value> nestedProperties = relValue.get(0).get("__properties__").asMap(Function.identity());
			assertThat(nestedProperties).containsEntry("prop1", Values.value("v" + (i * 2 + 0)));
			assertThat(nestedProperties).containsEntry("prop2", Values.value("v" + (i * 2 + 1)));
		}
	}

	@Test // DATAGRAPH-1452
	void dynamicRelationshipWithProperties2() {

		final A7 a = new A7("a7I");
		a.p1 = new HashMap<>();
		a.p1.put("rel1", Arrays.asList(new P1("v0", "v1", new B3("b3I"))));
		a.p1.put("rel2", Arrays.asList(
				new P1("v2", "v3", new B3("b3II")),
				new P1("v4", "v5", new B3("b3III"))
		));

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, a);

		assertThat(result).hasEntrySatisfying("__labels__", isAListValue);
		Value labels = (Value) result.get("__labels__");
		assertThat(labels.asList(v -> v.asString())).containsExactlyInAnyOrder("A7");

		assertThat(result).containsEntry("__id__", Values.value("a7I"));

		assertThat(result).hasEntrySatisfying("__properties__", isAMap);
		Map<String, Value> properties = (Map<String, Value>) result.get("__properties__");

		assertThat(properties).hasEntrySatisfying("rel1", isAListValue);
		assertThat(properties).hasEntrySatisfying("rel2", isAListValue);
		List<Value> rels = new ArrayList<>();
		rels.addAll(properties.get("rel1").asList(Function.identity()));
		rels.addAll(properties.get("rel2").asList(Function.identity()));
		String[] suffix = new String[] { "I", "II", "III" };
		for (int i = 0; i < suffix.length; ++i) {
			properties = rels.get(i).get("__properties__").asMap(Function.identity());
			assertThat(properties).containsEntry("prop1", Values.value("v" + (i * 2 + 0)));
			assertThat(properties).containsEntry("prop2", Values.value("v" + (i * 2 + 1)));

			properties = rels.get(i).get("__target__").asMap(Function.identity());
			assertThat(properties).containsEntry("__id__", Values.value("b3" + suffix[i]));
			assertThat(properties).hasEntrySatisfying("__properties__", isAMapValue);
		}
	}

	@Test // DATAGRAPH-1452
	void relationshipsWithSameTypeShouldAllBePresent() {

		final A8 a = new A8("a8I");

		a.b = new B("bI");
		a.b2 = new B2("b2I");
		a.b3 = Arrays.asList(new B3("b3I"), new B3("b3II"));

		EntityWriter<Object, Map<String, Object>> writer = Neo4jNestedMapEntityWriter.forContext(mappingContext);
		Map<String, Object> result = toMap(writer, a);

		assertThat(((Map<String, Value>) result.get("__properties__")).get("HAS").size()).isEqualTo(4);
	}

	public Map<String, Object> toMap(EntityWriter<Object, Map<String, Object>> writer, Object source) {

		if (source == null) {
			return Collections.emptyMap();
		}

		final Map<String, Object> result = new HashMap<>();
		writer.write(source, result);
		return result;
	}

	@Node
	@AllArgsConstructor
	static class FlatEntity {

		@Id @GeneratedValue
		private Long id;

		private double aDouble;

		private String aString;
	}

	@Node
	@AllArgsConstructor
	static class FlatEntityWithAdditionalTypes {

		@Id
		private String id;

		@Version
		private Long version;

		private Locale aLocale;

		private URI aURI;

		private SomeEnum someEnum;

		private List<Double> listOfDoubles;
	}

	enum SomeEnum { A, B }

	static class SomeIdGeneratory implements IdGenerator<String> {

		@Override
		public String generateId(String primaryLabel, Object entity) {
			return "abc";
		}
	}

	@Node
	@AllArgsConstructor
	static class FlatEntityWithDynamicLabels {

		@Id @GeneratedValue(generatorClass = SomeIdGeneratory.class)
		private String id;

		@DynamicLabels
		private List<String> dynamicLabels;
	}

	@Node
	@AllArgsConstructor
	static class GraphPropertyNamesShouldBeUsed {

		@Id
		@Property("myFineAssignedId")
		private String id;

		@Property("a_field")
		private String aField;
	}

	@Node
	@RequiredArgsConstructor
	static class A {

		@Id
		private final String id;

		private final B hasB;

		@Relationship("HAS_MORE_B")
		private final List<B> hasMoreBs;
	}

	@Node
	@RequiredArgsConstructor
	static class B {

		@Id
		private final String id;
	}

	@Node
	@RequiredArgsConstructor
	static class A2 {

		@Id
		private final String id;

		private final B2 hasB;
	}

	@Node
	@RequiredArgsConstructor
	static class B2 {

		@Id
		private final String id;

		private A2 belongsToA2;

		private List<A2> knows;
	}

	@Node
	@RequiredArgsConstructor
	static class A3 {

		@Id
		private final String id;

		@Relationship
		private final Map<String, B> hasB;

		@Relationship
		private final Map<String, List<B2>> hasMoreBs;
	}

	@Node
	@RequiredArgsConstructor
	static class A4 {

		@Id
		private final String id;

		@Relationship("HAS_P1")
		private P1 p1;
	}

	@RelationshipProperties
	@RequiredArgsConstructor
	@AllArgsConstructor
	static class P1 {

		@RelationshipId
		@With
		private Long id;

		private final String prop1;

		private final String prop2;

		@TargetNode
		private final B3 b3;

		@Override public String toString() {
			return "P1{" +
				   "prop1='" + prop1 + '\'' +
				   ", prop2='" + prop2 + '\'' +
				   '}';
		}
	}

	@Node
	@RequiredArgsConstructor
	static class B3 {

		@Id
		private final String id;
	}

	@Node
	@RequiredArgsConstructor
	static class A5 {

		@Id
		private final String id;

		@Relationship("HAS_P1")
		private List<P1> p1;
	}

	@Node
	@RequiredArgsConstructor
	static class A6 {

		@Id
		private final String id;

		@Relationship
		private Map<String, P1> p1;
	}

	@Node
	@RequiredArgsConstructor
	static class A7 {

		@Id
		private final String id;

		@Relationship
		private Map<String, List<P1>> p1;
	}

	@Node
	@RequiredArgsConstructor
	static class A8 {

		@Id
		private final String id;

		@Relationship("HAS")
		private B b;

		@Relationship("HAS")
		private B2 b2;

		@Relationship("HAS")
		private List<B3> b3;

	}
}
