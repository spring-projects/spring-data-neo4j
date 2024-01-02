/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.neo4j.integration.shared.common.PersonWithRelationship;

/**
 * @author Michael J. Simons
 */
class PropertyFilterTest {

	@ParameterizedTest
	@CsvSource({ "id, foo", "hobbies, foo", "hobbies.name, hobbies.foo" })
	void toDotPathShouldWork(String value, String newDotPath) {

		PropertyFilter.RelaxedPropertyPath path = PropertyFilter.RelaxedPropertyPath.withRootType(PersonWithRelationship.class).append(value);
		String dotPath;
		dotPath = PropertyFilter.toDotPath(path, null);
		assertThat(dotPath).isEqualTo(path.toDotPath());
		dotPath = PropertyFilter.toDotPath(path, "foo");
		assertThat(dotPath).isEqualTo(newDotPath);
	}

	@Nested
	class RelaxedPropertyPathTest {

		@ParameterizedTest
		@ValueSource(strings = { "s1", "s1.s2" })
		void toDotPathShouldWork(String value) {

			PropertyFilter.RelaxedPropertyPath path = PropertyFilter.RelaxedPropertyPath.withRootType(Object.class)
					.append(value);
			assertThat(path.toDotPath()).isEqualTo(value);
		}

		@ParameterizedTest
		@CsvSource({ "id, foo", "hobbies, foo", "hobbies.name, hobbies.foo" })
		void toDotPathWithReplacementShouldWork(String value, String newDotPath) {

			PropertyFilter.RelaxedPropertyPath path = PropertyFilter.RelaxedPropertyPath.withRootType(Object.class)
					.append(value);
			String dotPath;
			dotPath = path.toDotPath(null);
			assertThat(dotPath).isEqualTo(path.toDotPath());
			dotPath = path.toDotPath("foo");
			assertThat(dotPath).isEqualTo(newDotPath);
		}

		@ParameterizedTest
		@CsvSource({ "s1, s1", "s1.s2, s1" })
		void getSegmentShouldWork(String value, String segment) {

			PropertyFilter.RelaxedPropertyPath path = PropertyFilter.RelaxedPropertyPath.withRootType(Object.class)
					.append(value);
			assertThat(path.getSegment()).isEqualTo(segment);
		}

		@ParameterizedTest
		@CsvSource({ "s1, s1", "s1.s2, s2", "a.b.c, c" })
		void leafSegmentShouldWork(String value, String segment) {

			PropertyFilter.RelaxedPropertyPath path = PropertyFilter.RelaxedPropertyPath.withRootType(Object.class)
					.append(value);
			PropertyFilter.RelaxedPropertyPath leafProperty = path.getLeafProperty();
			assertThat(leafProperty.getType()).isEqualTo(path.getType());
			assertThat(leafProperty.getSegment()).isEqualTo(segment);
		}
	}
}
