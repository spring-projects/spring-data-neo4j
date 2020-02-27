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
package org.springframework.data.neo4j.repository.query.filter;

import static org.neo4j.ogm.cypher.ComparisonOperator.*;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.PropertyValueTransformer;
import org.neo4j.ogm.cypher.function.FilterFunction;

/**
 * This is a specialised filter function taking care of filtering native id properties.
 *
 * @author Michael J. Simons
 * @soundtrack Freddie Mercury - Never Boring
 */
final class NativeIdFilterFunction implements FilterFunction<Object> {

	// This function belongs somewhat more into OGM than SDN. The reason having it here is simple: The filter is build
	// explicitly and not via reflection and we don't want to have yet another shim managing separate possible versions
	// OGM like we already have with the embedded support, entity instantiator and some other things. ^ms

	protected static final String PARAMETER_NAME = "idProperty";

	private final ComparisonOperator operator;
	private final Object value;
	private Filter filter;

	NativeIdFilterFunction(ComparisonOperator operator, Object value) {
		this.operator = operator;
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public String expression(String nodeIdentifier, String filteredProperty,
			UnaryOperator<String> createUniqueParameterName) {

		switch (operator) {
			case EQUALS:
			case GREATER_THAN:
			case GREATER_THAN_EQUAL:
			case LESS_THAN:
			case LESS_THAN_EQUAL:
			case IN:
				return String.format("id(%s) %s $`%s` ", nodeIdentifier, operator.getValue(),
						createUniqueParameterName.apply(PARAMETER_NAME));
			default:
				throw new IllegalArgumentException("Unsupported comparision operator for an ID attribute.");
		}
	}

	@Override
	public Map<String, Object> parameters(UnaryOperator<String> createUniqueParameterName,
			PropertyValueTransformer valueTransformer) {

		Map<String, Object> map = Collections.emptyMap();
		if (EnumSet.of(EQUALS, GREATER_THAN, GREATER_THAN_EQUAL, LESS_THAN, LESS_THAN_EQUAL, IN).contains(operator)) {
			map = Collections.singletonMap(createUniqueParameterName.apply(PARAMETER_NAME),
					valueTransformer.transformPropertyValue(this.value));
		}
		return map;
	}

	public Map<String, Object> parameters() {
		throw new UnsupportedOperationException(
				"This method has been kept here for backward compatiblity and should never be called.");
	}

	public String expression(String s) {
		throw new UnsupportedOperationException(
				"This method has been kept here for backward compatiblity and should never be called.");
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}
}
