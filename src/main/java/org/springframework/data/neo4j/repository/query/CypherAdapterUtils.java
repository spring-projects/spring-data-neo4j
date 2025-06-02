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
package org.springframework.data.neo4j.repository.query;

import static org.neo4j.cypherdsl.core.Cypher.property;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.neo4j.driver.Value;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition.Direction;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.NodeDescription;

/**
 * Bridging between Spring Data domain Objects and Cypher constructs.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class CypherAdapterUtils {

	/**
	 * Maps Spring Data's {@link org.springframework.data.domain.Sort.Order} to a {@link SortItem}. See {@link #toSortItems(NodeDescription, Sort)}.
	 *
	 * @param nodeDescription {@link NodeDescription} to get properties for sorting from.
	 * @return A stream if sort items. Will be empty when sort is unsorted.
	 */
	public static Function<Sort.Order, SortItem> sortAdapterFor(NodeDescription<?> nodeDescription) {
		return order -> {

			String domainProperty = order.getProperty();
			boolean propertyIsQualifiedOrComposite = domainProperty.contains(".");
			SymbolicName root;
			if (!propertyIsQualifiedOrComposite) {
				root = Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription);
			} else {
				// need to check first if this is really a qualified name or the "qualifier" is a composite property
				if (nodeDescription.getGraphProperty(domainProperty.split("\\.")[0]).isEmpty()) {
					int indexOfSeparator = domainProperty.indexOf(".");
					root = Cypher.name(domainProperty.substring(0, indexOfSeparator));
					domainProperty = domainProperty.substring(indexOfSeparator + 1);
				} else {
					root = Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription);
				}
			}

			var optionalGraphProperty = nodeDescription.getGraphProperty(domainProperty);
			// try to resolve if this is a composite property
			if (optionalGraphProperty.isEmpty()) {
				var domainPropertyPrefix = domainProperty.split("\\.")[0];
				optionalGraphProperty = nodeDescription.getGraphProperty(domainPropertyPrefix);
			}
			if (optionalGraphProperty.isEmpty()) {
				throw new IllegalStateException(String.format("Cannot order by the unknown graph property: '%s'", domainProperty));
			}
			var graphProperty = optionalGraphProperty.get();
			Expression expression;
			if (graphProperty.isInternalIdProperty()) {
				// Not using the id expression here, as the root will be referring to the constructed map being returned.
				expression = property(root, Constants.NAME_OF_INTERNAL_ID);
			} else if (graphProperty.isComposite() && !domainProperty.contains(".")) {
				throw new IllegalStateException(String.format("Cannot order by composite property: '%s'. Only ordering by its nested fields is allowed.", domainProperty));
			} else if (graphProperty.isComposite()) {
				if (nodeDescription.containsPossibleCircles(rpp -> true)) {
					expression = property(root, domainProperty);
				} else {
					expression = property(root, Constants.NAME_OF_ALL_PROPERTIES, domainProperty);
				}
			} else {
				expression = property(root, graphProperty.getPropertyName());
				if (order.isIgnoreCase()) {
					expression = Cypher.toLower(expression);
				}
			}
			SortItem sortItem = Cypher.sort(expression);

			// Spring's Sort.Order defaults to ascending, so we just need to change this if we have descending order.
			if (order.isDescending()) {
				sortItem = sortItem.descending();
			}
			return sortItem;
		};
	}

	public static Condition combineKeysetIntoCondition(Neo4jPersistentEntity<?> entity, KeysetScrollPosition scrollPosition, Sort sort, Neo4jConversionService conversionService) {

		var incomingKeys = scrollPosition.getKeys();
		var orderedKeys = new LinkedHashMap<String, Object>();

		record PropertyAndOrder(Neo4jPersistentProperty property, Sort.Order order) {
		}
		var propertyAndDirection = new HashMap<String, PropertyAndOrder>();

		sort.forEach(order -> {
			var property = entity.getRequiredPersistentProperty(order.getProperty());
			var propertyName = property.getPropertyName();
			propertyAndDirection.put(propertyName, new PropertyAndOrder(property, order));

			if (incomingKeys.containsKey(propertyName)) {
				orderedKeys.put(propertyName, incomingKeys.get(propertyName));
			}
		});
		if (incomingKeys.containsKey(Constants.NAME_OF_ADDITIONAL_SORT)) {
			orderedKeys.put(Constants.NAME_OF_ADDITIONAL_SORT, incomingKeys.get(Constants.NAME_OF_ADDITIONAL_SORT));
		}

		var root = Constants.NAME_OF_TYPED_ROOT_NODE.apply(entity);

		var resultingCondition = Cypher.noCondition();
		// This is the next equality pair if previous sort key was equal
		var nextEquals = Cypher.noCondition();
		// This is the condition for when all the sort orderedKeys are equal, and we must filter via id
		var allEqualsWithArtificialSort = Cypher.noCondition();

		for (Map.Entry<String, Object> entry : orderedKeys.entrySet()) {

			var k = entry.getKey();
			var v = entry.getValue();
			if (v == null || (v instanceof Value value && value.isNull())) {
				throw new IllegalStateException("Cannot resume from KeysetScrollPosition. Offending key: '%s' is 'null'".formatted(k));
			}
			var parameter = Cypher.anonParameter(conversionService.convert(v, Value.class));

			Expression expression;

			var scrollDirection = scrollPosition.getDirection();
			if (Constants.NAME_OF_ADDITIONAL_SORT.equals(k)) {
				expression = entity.getIdExpression();
				var comparatorFunction = getComparatorFunction(scrollPosition.scrollsForward() ? Sort.Direction.ASC : Sort.Direction.DESC, scrollDirection);
				allEqualsWithArtificialSort = allEqualsWithArtificialSort.and(comparatorFunction.apply(expression, parameter));
			} else if (propertyAndDirection.containsKey(k)) {
				var p = propertyAndDirection.get(k);
				expression = p.property.isIdProperty() ? entity.getIdExpression() : root.property(k);

				var comparatorFunction = getComparatorFunction(p.order.getDirection(), scrollDirection);
				resultingCondition = resultingCondition.or(nextEquals.and(comparatorFunction.apply(expression, parameter)));
				nextEquals = expression.eq(parameter);
				allEqualsWithArtificialSort = allEqualsWithArtificialSort.and(nextEquals);
			}
		}
		return resultingCondition.or(allEqualsWithArtificialSort);
	}

	private static BiFunction<Expression, Expression, Condition> getComparatorFunction(Sort.Direction sortDirection, KeysetScrollPosition.Direction scrollDirection) {
		if (scrollDirection == Direction.BACKWARD) {
			return sortDirection.isAscending() ? Expression::lte : Expression::gte;
		}
		return sortDirection.isAscending() ? Expression::gt : Expression::lt;
	}

	/**
	 * Converts a Spring Data sort to an equivalent list of {@link SortItem sort items}.
	 *
	 * @param nodeDescription The node description to map the properties
	 * @param sort The sort object to convert
	 * @return An of sort items. It will be empty when sort is unsorted.
	 */
	public static Collection<SortItem> toSortItems(@Nullable NodeDescription<?> nodeDescription, Sort sort) {

		if (nodeDescription == null) {
			return List.of();
		}

		return sort.stream().map(sortAdapterFor(nodeDescription)).collect(Collectors.toList());
	}

	private CypherAdapterUtils() {}
}
