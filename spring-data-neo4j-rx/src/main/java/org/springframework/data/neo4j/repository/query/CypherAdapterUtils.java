/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.query;

import static org.springframework.data.neo4j.core.cypher.Cypher.*;

import java.util.function.Function;

import org.apiguardian.api.API;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.cypher.Cypher;
import org.springframework.data.neo4j.core.cypher.SortItem;
import org.springframework.data.neo4j.core.cypher.SymbolicName;
import org.springframework.data.neo4j.core.schema.GraphPropertyDescription;
import org.springframework.data.neo4j.core.schema.NodeDescription;

/**
 * A set of shared utils to adapt {@code org.springframework.data.neo4j.core.cypher} functionality with Spring Data
 * and vice versa.
 *
 * @author Michael J. Simons
 * @soundtrack Rammstein - Herzeleid
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class CypherAdapterUtils {

	/**
	 * Maps Spring Datas {@link Sort.Order} to a {@link SortItem}.
	 * See {@link #toSortItems(NodeDescription, Sort)}.
	 *
	 * @param nodeDescription
	 * @return A stream if sort items. Will be empty when sort is unsorted.
	 */
	public static Function<Sort.Order, SortItem> sortAdapterFor(NodeDescription<?> nodeDescription) {
		SymbolicName rootNode = Cypher.symbolicName(NodeDescription.NAME_OF_ROOT_NODE);

		return order -> {
			String property = nodeDescription.getGraphProperty(order.getProperty())
				.map(GraphPropertyDescription::getPropertyName)
				.orElseThrow(() -> new IllegalStateException(
					String.format("Cannot order by the unknown graph property: '%s'", order.getProperty())));
			SortItem sortItem = Cypher.sort(property(rootNode, property));

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
	 * @param sort            The sort object to convert
	 * @return An of sort items. It will be empty when sort is unsorted.
	 */
	public static SortItem[] toSortItems(NodeDescription<?> nodeDescription, Sort sort) {

		return sort.stream().map(sortAdapterFor(nodeDescription)).toArray(SortItem[]::new);
	}

	private CypherAdapterUtils() {
	}
}
