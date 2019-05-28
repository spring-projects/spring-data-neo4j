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
import static org.springframework.data.neo4j.core.schema.NodeDescription.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.cypher.Condition;
import org.springframework.data.neo4j.core.cypher.Conditions;
import org.springframework.data.neo4j.core.cypher.Cypher;
import org.springframework.data.neo4j.core.cypher.Expression;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.Node;
import org.springframework.data.neo4j.core.cypher.SortItem;
import org.springframework.data.neo4j.core.cypher.StatementBuilder;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.BuildableStatement;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingReadingAndWith;
import org.springframework.data.neo4j.core.cypher.SymbolicName;
import org.springframework.data.neo4j.core.schema.GraphPropertyDescription;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.IdDescription;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.Schema;
import org.springframework.data.neo4j.repository.support.Neo4jEntityInformation;

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

	/**
	 * Provides a builder for statements based on the schema of entity classes.
	 *
	 * @param schema The schema used for creating queries.
	 * @return
	 */
	public static SchemaBasedStatementBuilder createSchemaBasedStatementBuilder(Schema schema) {
		return new SchemaBasedStatementBuilder(schema);
	}

	/**
	 * This is a adapter between the Schema and the Cypher module.
	 */
	public static class SchemaBasedStatementBuilder {

		private final Schema schema;

		private SchemaBasedStatementBuilder(Schema schema) {
			this.schema = schema;
		}

		/**
		 * @param nodeDescription
		 * @return
		 * @see #prepareMatchOf(NodeDescription, Optional)
		 */
		public OngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription) {
			return prepareMatchOf(nodeDescription, Optional.empty());
		}

		/**
		 * This will create a match statement that fits the given node description and may contains additional conditions.
		 * The {@code WITH} clause of this statement contains all nodes and relationships necessary to map a record to
		 * the given {@code nodeDescription}.
		 * <p/>
		 * It is recommended to use {@link Cypher#asterisk()} to return everything from the query in the end.
		 * <p/>
		 * The root node is guaranteed to have the symbolic name {@code n}.
		 *
		 * @param nodeDescription The node description for which a match clause should be generated
		 * @param condition       Optional conditions to add
		 * @return An ongoing match
		 */
		public OngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription, Optional<Condition> condition) {
			Node rootNode = Cypher.node(nodeDescription.getPrimaryLabel()).named(NAME_OF_ROOT_NODE);
			IdDescription idDescription = nodeDescription.getIdDescription();

			List<Expression> expressions = new ArrayList<>();
			expressions.add(rootNode);
			if (idDescription.getIdStrategy() == Id.Strategy.INTERNAL) {
				expressions.add(Functions.id(rootNode).as(NAME_OF_INTERNAL_ID));
			}
			return Cypher.match(rootNode).where(condition.orElse(Conditions.noCondition()))
				.with(expressions.toArray(new Expression[expressions.size()]));
		}

		public BuildableStatement prepareDeleteOf(NodeDescription<?> nodeDescription,
			Optional<Condition> condition) {

			Node rootNode = Cypher.node(nodeDescription.getPrimaryLabel()).named(NAME_OF_ROOT_NODE);
			return Cypher.match(rootNode).where(condition.orElse(Conditions.noCondition())).delete(rootNode);
		}
	}

	public static BuildableStatement addPagingParameter(
		NodeDescription<?> nodeDescription,
		Pageable pageable,
		StatementBuilder.OngoingReadingAndReturn returning) {

		Sort sort = pageable.getSort();

		long skip = pageable.getOffset();

		int pageSize = pageable.getPageSize();

		return returning.orderBy(toSortItems(nodeDescription, sort)).skip(skip).limit(pageSize);
	}

	/**
	 * @param nodeDescription
	 * @return
	 * @see Neo4jEntityInformation#getIdExpression()
	 */
	public static Expression createIdExpression(final NodeDescription<?> nodeDescription) {

		final SymbolicName rootNode = Cypher.symbolicName(NAME_OF_ROOT_NODE);
		final IdDescription idDescription = nodeDescription.getIdDescription();
		Expression idExpression;
		switch (idDescription.getIdStrategy()) {
			case INTERNAL:
				idExpression = Functions.id(rootNode);
				break;
			case ASSIGNED:
			case GENERATED:
				idExpression = idDescription.getOptionalGraphPropertyName()
					.map(propertyName -> property(rootNode.getName(), propertyName)).get();
				break;
			default:
				throw new IllegalStateException("Unsupported ID strategy: %s" + idDescription.getIdStrategy());
		}

		return idExpression;
	}


	private CypherAdapterUtils() {
	}
}
