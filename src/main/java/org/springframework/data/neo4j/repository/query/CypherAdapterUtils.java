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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.GraphPropertyDescription;
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
			String graphProperty = nodeDescription.getGraphProperty(domainProperty)
					.map(GraphPropertyDescription::getPropertyName).orElseThrow(() -> new IllegalStateException(
							String.format("Cannot order by the unknown graph property: '%s'", order.getProperty())));
			Expression expression = property(root, graphProperty);
			if (order.isIgnoreCase()) {
				expression = Functions.toLower(expression);
			}
			SortItem sortItem = Cypher.sort(expression);

			// Spring's Sort.Order defaults to ascending, so we just need to change this if we have descending order.
			if (order.isDescending()) {
				sortItem = sortItem.descending();
			}
			return sortItem;
		};
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
