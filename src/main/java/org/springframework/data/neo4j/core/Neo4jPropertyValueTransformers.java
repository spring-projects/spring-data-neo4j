/*
 * Copyright 2011-2025 the original author or authors.
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

import org.springframework.data.domain.ExampleMatcher;

/**
 * Contains some useful transformers for adding additional, supported transformations to {@link ExampleMatcher example matchers} via
 * {@link org.springframework.data.domain.ExampleMatcher#withTransformer(String, ExampleMatcher.PropertyValueTransformer)}.
 *
 * @author Michael J. Simons
 * @since 6.3.11
 * @soundtrack Subway To Sally - Herzblut
 */
public abstract class Neo4jPropertyValueTransformers {

	/**
	 * A transformer that will indicate that the generated condition for the specific property shall be negated, creating
	 * a {@code n.property != $property} for the equality operator for example.
	 *
	 * @return A value transformer negating values.
	 */
	public static ExampleMatcher.PropertyValueTransformer notMatching() {
		return o -> o.map(NegatedValue::new);
	}

	/**
	 * A wrapper indicating a negated value (will be used as {@code n.property != $parameter} (in case of string properties
	 * all operators and not only the equality operator are supported, such as {@code not (n.property contains 'x')}.
	 *
	 * @param value The value used in the negated condition.
	 */
	public record NegatedValue(Object value) {
	}

	private Neo4jPropertyValueTransformers() {
	}
}
