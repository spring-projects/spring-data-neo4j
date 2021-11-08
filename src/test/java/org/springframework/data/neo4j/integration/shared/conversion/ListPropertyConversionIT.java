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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test around collections to be converted as a whole and not as individual elements.
 *
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ListPropertyConversionIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static Long existingNodeId;

	@BeforeAll
	static void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			existingNodeId = session.run("CREATE (n:DomainObjectWithListOfConvertables {"
										 + "someprefixA_0: '1', someprefixB_0: '2', someprefixA_1: '3', someprefixB_1: '4', "
										 + "`moreCollectedData.A_0`: '11', `moreCollectedData.B_0`: '22', `moreCollectedData.A_1`: '33', `moreCollectedData.B_1`: '44', "
										 + "anotherSet: '1;2,3;4'"
										 + "}) RETURN id(n)")
					.single().get(0).asLong();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2408
	void writingDecomposedListsShouldWork(@Autowired Neo4jTemplate template,
			@Autowired BookmarkCapture bookmarkCapture) {

		DomainObjectWithListOfConvertables object = new DomainObjectWithListOfConvertables();
		object.collectedData = Arrays.asList(
				new SomeConvertableClass(new BigDecimal("523.6"), new BigDecimal("67689.7")),
				new SomeConvertableClass(new BigDecimal("4456.3"), new BigDecimal("3109.6")),
				new SomeConvertableClass(new BigDecimal("100.6"), new BigDecimal("3050.6"))
		);

		object = template.save(object);

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			org.neo4j.driver.types.Node node =
					session.run("MATCH (n:DomainObjectWithListOfConvertables) WHERE id(n) = $id RETURN n",
							Collections.singletonMap("id", object.id)).single().get(0).asNode();

			assertThat(node.get("someprefixA_0").asString()).isEqualTo("523.6");
			assertThat(node.get("someprefixA_1").asString()).isEqualTo("4456.3");
			assertThat(node.get("someprefixA_2").asString()).isEqualTo("100.6");

			assertThat(node.get("someprefixB_0").asString()).isEqualTo("67689.7");
			assertThat(node.get("someprefixB_1").asString()).isEqualTo("3109.6");
			assertThat(node.get("someprefixB_2").asString()).isEqualTo("3050.6");
		}
	}

	@Test // GH-2430
	void writingDecomposedListsWithBeansShouldWork(@Autowired Neo4jTemplate template,
			@Autowired BookmarkCapture bookmarkCapture) {

		DomainObjectWithListOfConvertables object = new DomainObjectWithListOfConvertables();
		object.moreCollectedData = Arrays.asList(
				new SomeConvertableClass(new BigDecimal("42"), new BigDecimal("23")),
				new SomeConvertableClass(new BigDecimal("666"), new BigDecimal("665"))
		);

		object = template.save(object);

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			org.neo4j.driver.types.Node node =
					session.run("MATCH (n:DomainObjectWithListOfConvertables) WHERE id(n) = $id RETURN n",
							Collections.singletonMap("id", object.id)).single().get(0).asNode();

			assertThat(node.get("moreCollectedData.A_0").asString()).isEqualTo("42");
			assertThat(node.get("moreCollectedData.A_1").asString()).isEqualTo("666");

			assertThat(node.get("moreCollectedData.B_0").asString()).isEqualTo("23");
			assertThat(node.get("moreCollectedData.B_1").asString()).isEqualTo("665");
		}
	}

	@Test // GH-2408
	void writingCompressedListsShouldWork(@Autowired Neo4jTemplate template,
			@Autowired BookmarkCapture bookmarkCapture) {

		DomainObjectWithListOfConvertables object = new DomainObjectWithListOfConvertables();
		object.anotherSet = Arrays.asList(
				new SomeConvertableClass(new BigDecimal("523.6"), new BigDecimal("67689.7")),
				new SomeConvertableClass(new BigDecimal("4456.3"), new BigDecimal("3109.6")),
				new SomeConvertableClass(new BigDecimal("100.6"), new BigDecimal("3050.6"))
		);

		object = template.save(object);
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			org.neo4j.driver.types.Node node =
					session.run("MATCH (n:DomainObjectWithListOfConvertables) WHERE id(n) = $id RETURN n",
							Collections.singletonMap("id", object.id)).single().get(0).asNode();
			assertThat(node.get("anotherSet").asString()).isEqualTo("523.6;67689.7,4456.3;3109.6,100.6;3050.6");
		}
	}

	@Test // GH-2408
	void readingDecomposedListsWithBeansShouldWork(@Autowired Neo4jTemplate template) {

		Optional<DomainObjectWithListOfConvertables> optionalResult = template.findById(existingNodeId,
				DomainObjectWithListOfConvertables.class);

		assertThat(optionalResult).hasValueSatisfying(object -> {
			assertThat(object.collectedData).hasSize(2);
			assertThat(object.collectedData).containsExactlyInAnyOrder(
					new SomeConvertableClass(new BigDecimal("1"), new BigDecimal("2")),
					new SomeConvertableClass(new BigDecimal("3"), new BigDecimal("4"))
			);
		});
	}

	@Test // GH-2430
	void readingDecomposedListsShouldWork(@Autowired Neo4jTemplate template) {

		Optional<DomainObjectWithListOfConvertables> optionalResult = template.findById(existingNodeId,
				DomainObjectWithListOfConvertables.class);

		assertThat(optionalResult).hasValueSatisfying(object -> {
			assertThat(object.moreCollectedData).hasSize(2);
			assertThat(object.moreCollectedData).containsExactlyInAnyOrder(
					new SomeConvertableClass(new BigDecimal("11"), new BigDecimal("22")),
					new SomeConvertableClass(new BigDecimal("33"), new BigDecimal("44"))
			);
		});
	}

	@Test // GH-2408
	void readingCompressedListsShouldWork(@Autowired Neo4jTemplate template) {

		Optional<DomainObjectWithListOfConvertables> optionalResult = template.findById(existingNodeId,
				DomainObjectWithListOfConvertables.class);

		assertThat(optionalResult).hasValueSatisfying(object -> {
			assertThat(object.anotherSet).hasSize(2);
			assertThat(object.anotherSet).containsExactlyInAnyOrder(
					new SomeConvertableClass(new BigDecimal("1"), new BigDecimal("2")),
					new SomeConvertableClass(new BigDecimal("3"), new BigDecimal("4"))
			);
		});
	}

	@Node
	static class DomainObjectWithListOfConvertables {

		@Id @GeneratedValue
		private Long id;

		@CompositeProperty(converter = ListDecomposingConverter.class, delimiter = "", prefix = "someprefix")
		private List<SomeConvertableClass> collectedData;

		@ConvertWith(converter = SomeconvertableClassConverter.class)
		private List<SomeConvertableClass> anotherSet;

		@CompositeProperty(converterRef = "listDecomposingConverterBean")
		private List<SomeConvertableClass> moreCollectedData;
	}

	static class SomeConvertableClass {

		private final BigDecimal x;
		private final BigDecimal y;

		SomeConvertableClass(BigDecimal x, BigDecimal y) {
			this.x = x;
			this.y = y;
		}

		public BigDecimal getX() {
			return x;
		}

		public BigDecimal getY() {
			return y;
		}

		@Override public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			SomeConvertableClass that = (SomeConvertableClass) o;
			return x.equals(that.x) && y.equals(that.y);
		}

		@Override public int hashCode() {
			return Objects.hash(x, y);
		}
	}

	static class ListDecomposingConverter implements
			Neo4jPersistentPropertyToMapConverter<String, List<SomeConvertableClass>> {

		@Override
		public Map<String, Value> decompose(List<SomeConvertableClass> property,
				Neo4jConversionService neo4jConversionService) {

			if (property == null) {
				return Collections.emptyMap();
			}

			Map<String, Value> properties = new HashMap<>();
			for (int i = 0; i < property.size(); i++) {
				SomeConvertableClass some = property.get(i);
				if (some != null) {
					properties.put("A_" + i, Values.value(some.getX().toString()));
					properties.put("B_" + i, Values.value(some.getY().toString()));
				}
			}

			return properties;
		}

		@Override
		public List<SomeConvertableClass> compose(Map<String, Value> source,
				Neo4jConversionService neo4jConversionService) {

			if (source.isEmpty()) {
				return Collections.emptyList();
			}

			List<SomeConvertableClass> result = new ArrayList<>(source.size() / 2);
			for (int i = 0; i < source.size() / 2; ++i) {
				result.add(new SomeConvertableClass(
						new BigDecimal(source.get("A_" + i).asString()),
						new BigDecimal(source.get("B_" + i).asString())
				));
			}

			return result;
		}
	}

	static class SomeconvertableClassConverter implements Neo4jPersistentPropertyConverter<List<SomeConvertableClass>> {

		@Override public Value write(List<SomeConvertableClass> source) {

			if (source == null) {
				return Values.NULL;
			}

			return Values.value(source.stream().map(v ->
							String.format("%s;%s", v.x.toString(), v.y.toString()))
					.collect(Collectors.joining(",")));
		}

		@Override
		public List<SomeConvertableClass> read(Value source) {

			return Arrays.stream(source.asString().split(",")).map(v -> {
				String[] pair = v.split(";");
				return new SomeConvertableClass(new BigDecimal(pair[0]), new BigDecimal(pair[1]));
			}).collect(Collectors.toList());
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions)
				throws ClassNotFoundException {

			Neo4jMappingContext ctx = new Neo4jMappingContext(neo4JConversions);
			ctx.setInitialEntitySet(Collections.singleton(DomainObjectWithListOfConvertables.class));
			return ctx;
		}

		@Bean
		public ListDecomposingConverter listDecomposingConverterBean() {
			return new ListDecomposingConverter();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
