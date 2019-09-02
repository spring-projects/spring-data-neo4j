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
package org.neo4j.springframework.data.repository.support;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.schema.NodeDescription.*;
import static org.neo4j.springframework.data.repository.query.CypherAdapterUtils.*;
import static org.neo4j.springframework.data.repository.query.CypherAdapterUtils.SchemaBasedStatementBuilder.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.Neo4jClient.ExecutableQuery;
import org.neo4j.springframework.data.core.PreparedQuery;
import org.neo4j.springframework.data.core.cypher.Condition;
import org.neo4j.springframework.data.core.cypher.Functions;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.cypher.StatementBuilder;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingReadingAndReturn;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository base implementation for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@Repository
@Transactional(readOnly = true)
class SimpleNeo4jRepository<T, ID> implements PagingAndSortingRepository<T, ID> {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(SimpleNeo4jRepository.class));

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final Neo4jClient neo4jClient;

	private final Neo4jEntityInformation<T, ID> entityInformation;

	private final Neo4jPersistentEntity<T> entityMetaData;

	private final SchemaBasedStatementBuilder statementBuilder;

	private final Neo4jEvents eventSupport;

	private final Neo4jMappingContext neo4jMappingContext;

	SimpleNeo4jRepository(Neo4jClient neo4jClient, Neo4jEntityInformation<T, ID> entityInformation,
		SchemaBasedStatementBuilder statementBuilder, Neo4jEvents eventSupport,
		Neo4jMappingContext neo4jMappingContext) {

		this.neo4jClient = neo4jClient;
		this.entityInformation = entityInformation;
		this.entityMetaData = this.entityInformation.getEntityMetaData();
		this.statementBuilder = statementBuilder;
		this.eventSupport = eventSupport;
		this.neo4jMappingContext = neo4jMappingContext;
	}

	@Override
	public Iterable<T> findAll(Sort sort) {

		Statement statement = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(statementBuilder.createReturnStatementForMatch(entityMetaData))
			.orderBy(toSortItems(entityMetaData, sort))
			.build();

		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		OngoingReadingAndReturn returning = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(statementBuilder.createReturnStatementForMatch(entityMetaData));

		StatementBuilder.BuildableStatement returningWithPaging = addPagingParameter(entityMetaData, pageable,
			returning);

		Statement statement = returningWithPaging.build();

		List<T> allResult = createExecutableQuery(statement).getResults();
		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(allResult, pageable, totalCountSupplier);
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {

		S entityToBeSaved = eventSupport.maybeCallBeforeBind(entity);
		Long internalId = neo4jClient
			.query(() -> renderer.render(statementBuilder.prepareSaveOf(entityMetaData)))
			.bind((T) entityToBeSaved)
			.with(entityInformation.getBinderFunction())
			.fetchAs(Long.class).one().get();

		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);

		if (!entityMetaData.isUsingInternalIds()) {
			processNestedAssociations(entityMetaData, entityToBeSaved);
			return entityToBeSaved;
		} else {
			propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), internalId);
			processNestedAssociations(entityMetaData, entityToBeSaved);

			return (S) propertyAccessor.getBean();
		}
	}

	private void processNestedAssociations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject) {

		PersistentPropertyAccessor<?> propertyAccessor = neo4jPersistentEntity.getPropertyAccessor(parentObject);
		Object fromId = propertyAccessor.getProperty(neo4jPersistentEntity.getRequiredIdProperty());

		neo4jPersistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) handler -> {

			Neo4jPersistentProperty inverse = handler.getInverse();

			Object value = propertyAccessor.getProperty(inverse);

			Class<?> associationTargetType = inverse.getAssociationTargetType();

			Neo4jPersistentEntity<?> targetNodeDescription = (Neo4jPersistentEntity<?>) neo4jMappingContext
				.getRequiredNodeDescription(associationTargetType);

			Collection<RelationshipDescription> relationships = neo4jMappingContext
				.getRelationshipsOf(neo4jPersistentEntity.getPrimaryLabel());

			RelationshipDescription relationship = relationships.stream()
				.filter(r -> r.getPropertyName().equals(inverse.getName()))
				.findFirst().get();

			// remove all relationships before creating all new
			// this avoids the usage of cache but might have significant impact on overall performance
			Statement relationshipRemoveQuery = createRelationshipRemoveQuery(neo4jPersistentEntity, fromId, relationship, targetNodeDescription.getPrimaryLabel());
			neo4jClient.query(renderer.render(relationshipRemoveQuery)).run();

			if (value == null) {
				return;
			}

			Collection<Object> relatedValues = inverse.isCollectionLike() ?
				(Collection<Object>) value :
				Collections.singleton(value);

			for (Object relatedValue : relatedValues) {

				Object valueToBeSaved = eventSupport.maybeCallBeforeBind(relatedValue);

				Long relatedInternalId = saveRelatedNode(valueToBeSaved, associationTargetType, targetNodeDescription);

				Statement relationshipCreationQuery = createRelationshipCreationQuery(neo4jPersistentEntity,
					fromId, relationship, relatedInternalId);

				neo4jClient.query(renderer.render(relationshipCreationQuery)).run();

				// if an internal id is used this must get set to link this entity in the next iteration
				if (targetNodeDescription.isUsingInternalIds()) {
					PersistentPropertyAccessor<?> targetPropertyAccessor = targetNodeDescription.getPropertyAccessor(valueToBeSaved);
					targetPropertyAccessor.setProperty(targetNodeDescription.getRequiredIdProperty(), relatedInternalId);
				}
				processNestedAssociations(targetNodeDescription, valueToBeSaved);
			}
		});
	}

	private <Y> Long saveRelatedNode(Object entity, Class<Y> entityType, NodeDescription targetNodeDescription) {
		return neo4jClient.query(() -> renderer.render(statementBuilder.prepareSaveOf(targetNodeDescription)))
			.bind((Y) entity).with(neo4jMappingContext.getRequiredBinderFunctionFor(entityType))
			.fetchAs(Long.class).one().get();
	}

	@Override
	@Transactional
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {

		if (entityMetaData.isUsingInternalIds()) {
			log.debug("Saving entities using single statements.");

			return StreamSupport.stream(entities.spliterator(), false)
				.map(this::save)
				.collect(toList());
		}

		List<S> entitiesToBeSaved = StreamSupport.stream(entities.spliterator(), false)
			.map(eventSupport::maybeCallBeforeBind)
			.collect(toList());
		List<Map<String, Object>> entityList = entitiesToBeSaved.stream()
			.map(entityInformation.getBinderFunction()).collect(toList());

		ResultSummary resultSummary = neo4jClient
			.query(() -> renderer.render(statementBuilder.prepareSaveOfMultipleInstancesOf(entityMetaData)))
			.bind(entityList).to(NAME_OF_ENTITY_LIST_PARAM)
			.run();

		entitiesToBeSaved.forEach(entityToBeSaved -> {
			processNestedAssociations(entityMetaData, entityToBeSaved);
		});

		SummaryCounters counters = resultSummary.counters();
		log.debug(() -> String.format("Created %d and deleted %d nodes, created %d and deleted %d relationships and set %d properties.",
			counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
			counters.relationshipsDeleted(), counters.propertiesSet()));

		return entitiesToBeSaved;
	}

	@Override
	public Optional<T> findById(ID id) {

		Statement statement = statementBuilder
			.prepareMatchOf(entityMetaData, entityInformation.getIdExpression().isEqualTo(literalOf(id)))
			.returning(statementBuilder.createReturnStatementForMatch(entityMetaData))
			.build();
		return createExecutableQuery(statement).getSingleResult();
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	public Iterable<T> findAll() {

		Statement statement = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(statementBuilder.createReturnStatementForMatch(entityMetaData)).build();
		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {

		Statement statement = statementBuilder
			.prepareMatchOf(entityMetaData, entityInformation.getIdExpression().in((parameter("ids"))))
			.returning(statementBuilder.createReturnStatementForMatch(entityMetaData))
			.build();

		return createExecutableQuery(statement, singletonMap("ids", ids)).getResults();
	}

	@Override
	public long count() {

		Statement statement = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(Functions.count(asterisk())).build();

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(renderer.render(statement))
			.build();
		return neo4jClient.toExecutableQuery(preparedQuery)
			.getRequiredSingleResult();
	}

	@Override
	@Transactional
	public void deleteById(ID id) {

		String nameOfParameter = "id";
		Condition condition = this.entityInformation.getIdExpression().isEqualTo(parameter(nameOfParameter));

		log.debug(() -> String.format("Deleting entity with id %d ", id));

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, condition);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
			.bind(id).to(nameOfParameter)
			.run();

		log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted()));
	}

	@Override
	@Transactional
	public void delete(T entity) {

		ID id = this.entityInformation.getId(entity);
		this.deleteById(id);
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {

		String nameOfParameter = "ids";
		Condition condition = entityInformation.getIdExpression().in(parameter(nameOfParameter));

		List<Object> ids = StreamSupport.stream(entities.spliterator(), false)
			.map(this.entityInformation::getId).collect(toList());

		log.debug(() -> String.format("Deleting all entities with the following ids: %s ", ids));

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, condition);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
			.bind(ids).to(nameOfParameter)
			.run();

		log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted()));
	}

	@Override
	@Transactional
	public void deleteAll() {

		log.debug(() -> String.format("Deleting all nodes with primary label %s", entityMetaData.getPrimaryLabel()));

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement)).run();

		log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted()));
	}

	private ExecutableQuery<T> createExecutableQuery(Statement statement) {
		return createExecutableQuery(statement, Collections.emptyMap());
	}

	private ExecutableQuery<T> createExecutableQuery(Statement statement, Map<String, Object> parameters) {

		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(this.entityInformation.getJavaType())
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.usingMappingFunction(this.entityInformation.getMappingFunction())
			.build();
		return neo4jClient.toExecutableQuery(preparedQuery);
	}
}
