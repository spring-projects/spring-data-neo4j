/*
 * Copyright 2011-2023 the original author or authors.
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
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.neo4j.driver.Value;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
			boolean propertyIsQualified = domainProperty.contains(".");
			SymbolicName root;
			if (!propertyIsQualified) {
				root = Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription);
			} else {
				int indexOfSeparator = domainProperty.indexOf(".");
				root = Cypher.name(domainProperty.substring(0, indexOfSeparator));
				domainProperty = domainProperty.substring(indexOfSeparator + 1);
			}

			var optionalGraphProperty = nodeDescription.getGraphProperty(domainProperty);
			if (optionalGraphProperty.isEmpty()) {
				throw new IllegalStateException(String.format("Cannot order by the unknown graph property: '%s'", order.getProperty()));
			}
			var graphProperty = optionalGraphProperty.get();
			Expression expression;
			if (graphProperty.isInternalIdProperty()) {
				// Not using the id expression here, as the root will be referring to the constructed map being returned.
				expression = property(root, Constants.NAME_OF_INTERNAL_ID);
			} else {
				expression = property(root, graphProperty.getPropertyName());
				if (order.isIgnoreCase()) {
					expression = Functions.toLower(expression);
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

	public static Condition combineKeysetIntoCondition(Neo4jPersistentEntity<?> entity, KeysetScrollPosition scrollPosition, Sort sort) {

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

		var resultingCondition = Conditions.noCondition();
		// This is the next equality pair if previous sort key was equal
		var nextEquals = Conditions.noCondition();
		// This is the condition for when all the sort orderedKeys are equal, and we must filter via id
		var allEqualsWithArtificialSort = Conditions.noCondition();

		for (Map.Entry<String, Object> entry : orderedKeys.entrySet()) {

			var k = entry.getKey();
			var v = entry.getValue();
			if (v == null || (v instanceof Value value && value.isNull())) {
				throw new IllegalStateException("Cannot resume from KeysetScrollPosition. Offending key: '%s' is 'null'".formatted(k));
			}
			var parameter = Cypher.anonParameter(v);

			Expression expression;

			var scrollDirection = scrollPosition.getDirection();
			if (Constants.NAME_OF_ADDITIONAL_SORT.equals(k)) {
				expression = entity.getIdExpression();
				var comparatorFunction = getComparatorFunction(scrollDirection == KeysetScrollPosition.Direction.Forward ? Sort.Direction.ASC : Sort.Direction.DESC, scrollDirection);
				allEqualsWithArtificialSort = allEqualsWithArtificialSort.and(comparatorFunction.apply(expression, parameter));
			} else {
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
		if (scrollDirection == KeysetScrollPosition.Direction.Backward) {
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
	public static Collection<SortItem> toSortItems(NodeDescription<?> nodeDescription, Sort sort) {

		return sort.stream().map(sortAdapterFor(nodeDescription)).collect(Collectors.toList());
	}

	public static StatementBuilder.BuildableStatement addPagingParameter(NodeDescription<?> nodeDescription,
			Pageable pageable, StatementBuilder.OngoingReadingAndReturn returning) {

		Sort sort = pageable.getSort();

		long skip = pageable.getOffset();

		int pageSize = pageable.getPageSize();

		return returning.orderBy(toSortItems(nodeDescription, sort)).skip(skip).limit(pageSize);
	}

	private CypherAdapterUtils() {}
}
