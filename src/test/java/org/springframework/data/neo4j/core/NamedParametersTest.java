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
package org.springframework.data.neo4j.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class NamedParametersTest {

	@Test
	void shouldConvertCorrectListOfParametersIntoMap() {

		NamedParameters namedParameters = new NamedParameters();

		namedParameters.add("a", 1);
		namedParameters.add("b", "Something");
		namedParameters.add("c", null);

		assertThat(namedParameters.get()).containsEntry("a", 1).containsEntry("b", "Something")
				.containsEntry("c", null);
	}

	@Test
	void shouldNotAllowDuplicateParameters() {

		assertThatIllegalArgumentException().isThrownBy(() -> {
			NamedParameters namedParameters = new NamedParameters();

			namedParameters.add("a", 1);
			namedParameters.add("b", 1);
			namedParameters.add("a", 2);
		}).withMessage(
				"Duplicate parameter name: 'a' already in the list of named parameters with value '1'. New value would be '2'");

		assertThatIllegalArgumentException().isThrownBy(() -> {
			NamedParameters namedParameters = new NamedParameters();
			namedParameters.add("a", 1);

			Map<String, Object> newValues = new HashMap<>();
			newValues.put("b", 1);
			newValues.put("a", 2);

			namedParameters.addAll(newValues);
		}).withMessage(
				"Duplicate parameter name: 'a' already in the list of named parameters with value '1'. New value would be '2'");

		assertThatIllegalArgumentException().isThrownBy(() -> {
			NamedParameters namedParameters = new NamedParameters();

			namedParameters.add("a", null);
			namedParameters.add("a", 2);
		}).withMessage(
				"Duplicate parameter name: 'a' already in the list of named parameters with value 'null'. New value would be '2'");

		assertThatIllegalArgumentException().isThrownBy(() -> {
			NamedParameters namedParameters = new NamedParameters();

			namedParameters.add("a", 1);
			namedParameters.add("a", null);
		}).withMessage(
				"Duplicate parameter name: 'a' already in the list of named parameters with value '1'. New value would be 'null'");
	}

	@Test
	void shouldDealWithEmptyParameterList() {

		assertThat(new NamedParameters().get()).isEmpty();
	}

	@Nested
	class ToString {

		@Test
		void shouldEscapeStrings() {

			NamedParameters p = new NamedParameters();
			p.add("aKey", "A fancy\\ value");

			assertThat(p.toString()).isEqualTo(":param aKey => 'A fancy\\\\ value'");
		}

		@Test
		void shouldDealWithNullValues() {

			NamedParameters p = new NamedParameters();
			p.add("aKey", null);

			assertThat(p.toString()).isEqualTo(":param aKey => null");
		}

		@Test
		void shouldDealWithMaps() {

			Map<String, Object> outer = new TreeMap<>();
			outer.put("oma", "Something");
			outer.put("omb", Collections.singletonMap("ims", "Something else"));

			NamedParameters p = new NamedParameters();
			p.add("aKey", outer);

			String[] output = p.toString().split(System.lineSeparator());
			assertThat(output)
					.containsExactly(
							":param aKey => {oma: 'Something', omb: {ims: 'Something else'}}"
					);
		}

		@Test
		void shouldDealWithNestedMaps() {

			Map<String, Object> outer = new TreeMap<>();
			outer.put("oma", "Something");
			outer.put("omb", Collections.singletonMap("ims", Collections.singletonMap("imi", "Embedded Thing")));
			outer.put("omc", Collections.singletonMap("ims", "Something else"));

			NamedParameters p = new NamedParameters();
			p.add("aKey", outer);

			String[] output = p.toString().split(System.lineSeparator());
			assertThat(output)
					.containsExactly(
							":param aKey => {oma: 'Something', omb: {ims: {imi: 'Embedded Thing'}}, omc: {ims: 'Something else'}}"
					);
		}

		@Test
		void shouldDealWithLists() {

			NamedParameters p = new NamedParameters();
			p.add("a", Arrays.asList("Something", "Else"));
			p.add("l", Arrays.asList(1L, 2L, 3L));
			p.add("m", Arrays.asList(
					Collections.singletonMap("a", "av"), Collections.singletonMap("b", Arrays.asList("A", "b"))));

			String[] output = p.toString().split(System.lineSeparator());
			assertThat(output)
					.containsExactly(
							":param a => ['Something', 'Else']",
							":param l => [1, 2, 3]",
							":param m => [{a: 'av'}, {b: ['A', 'b']}]"
					);
		}
	}
}
