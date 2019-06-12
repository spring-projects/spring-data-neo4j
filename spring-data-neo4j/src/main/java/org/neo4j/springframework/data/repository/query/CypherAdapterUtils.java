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
package org.neo4j.springframework.data.repository.query;

import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.schema.NodeDescription.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.*;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.BuildableStatement;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingReadingAndWith;
import org.neo4j.springframework.data.core.schema.GraphPropertyDescription;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.IdDescription;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.core.schema.Schema;
import org.neo4j.springframework.data.repository.support.Neo4jEntityInformation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.util.Assert;

/**
 * A set of shared utils to adapt {@code org.neo4j.springframework.data.core.cypher} functionality with Spring Data
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
		SymbolicName rootNode = Cypher.name(NodeDescription.NAME_OF_ROOT_NODE);

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
			Node rootNode = node(nodeDescription.getPrimaryLabel()).named(NAME_OF_ROOT_NODE);
			IdDescription idDescription = nodeDescription.getIdDescription();

			List<Expression> expressions = new ArrayList<>();
			expressions.add(rootNode);
			if (idDescription.getIdStrategy() == Id.Strategy.INTERNALLY_GENERATED) {
				expressions.add(Functions.id(rootNode).as(NAME_OF_INTERNAL_ID));
			}
			return Cypher.match(rootNode).where(condition.orElse(Conditions.noCondition()))
				.with(expressions.toArray(new Expression[expressions.size()]));
		}

		public Statement prepareDeleteOf(NodeDescription<?> nodeDescription,
			Optional<Condition> condition) {

			Node rootNode = node(nodeDescription.getPrimaryLabel()).named(NAME_OF_ROOT_NODE);
			return Cypher.match(rootNode).where(condition.orElse(Conditions.noCondition())).delete(rootNode).build();
		}

		public Statement prepareSaveOf(NodeDescription<?> nodeDescription) {

			Node rootNode = node(nodeDescription.getPrimaryLabel()).named(NAME_OF_ROOT_NODE);
			IdDescription idDescription = nodeDescription.getIdDescription();
			Parameter idParameter = parameter(NAME_OF_ID_PARAM);
			if (idDescription.getIdStrategy().isExternal()) {
				String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
					.orElseThrow(() -> new MappingException("External id does not correspond to a graph property!"));

				return Cypher.merge(rootNode.properties(nameOfIdProperty, idParameter))
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM))
					.returning(rootNode.internalId())
					.build();
			} else {
				Node possibleExistingNode = node(nodeDescription.getPrimaryLabel()).named("hlp");

				Statement createIfNew = Cypher
					.optionalMatch(possibleExistingNode)
					.where(possibleExistingNode.internalId().isEqualTo(idParameter))
					.with(possibleExistingNode).where(possibleExistingNode.isNull())
					.create(rootNode)
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM))
					.returning(rootNode.internalId())
					.build();

				Statement updateIfExists = Cypher
					.match(rootNode)
					.where(rootNode.internalId().isEqualTo(idParameter))
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM))
					.returning(rootNode.internalId())
					.build();

				return Cypher.union(createIfNew, updateIfExists);
			}
		}

		public Statement prepareSaveOfMultipleInstancesOf(NodeDescription<?> nodeDescription) {

			Assert.isTrue(!nodeDescription.isUsingInternalIds(),
				"Only entities that use external IDs can be saved in a batch.");

			Node rootNode = node(nodeDescription.getPrimaryLabel()).named(NAME_OF_ROOT_NODE);
			IdDescription idDescription = nodeDescription.getIdDescription();

			String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
				.orElseThrow(() -> new MappingException("External id does not correspond to a graph property!"));

			String row = "entity";
			return Cypher
				.unwind(parameter(NAME_OF_ENTITY_LIST_PARAM)).as(row)
				.merge(rootNode.properties(nameOfIdProperty, property(row, NAME_OF_ID_PARAM)))
				.set(rootNode, property(row, NAME_OF_PROPERTIES_PARAM))
				.returning(rootNode.internalId())
				.build();
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

		final SymbolicName rootNode = Cypher.name(NAME_OF_ROOT_NODE);
		final IdDescription idDescription = nodeDescription.getIdDescription();
		Expression idExpression;
		switch (idDescription.getIdStrategy()) {
			case INTERNALLY_GENERATED:
				idExpression = Functions.id(rootNode);
				break;
			case ASSIGNED:
			case EXTERNALLY_GENERATED:
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
