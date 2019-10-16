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
package org.neo4j.springframework.data.core;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.schema.CypherGenerator.*;
import static org.neo4j.springframework.data.core.schema.NodeDescription.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.springframework.data.core.cypher.Condition;
import org.neo4j.springframework.data.core.cypher.Functions;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.neo4j.springframework.data.core.schema.CypherGenerator;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.neo4j.springframework.data.core.support.Relationships;
import org.neo4j.springframework.data.repository.NoResultException;
import org.neo4j.springframework.data.repository.event.BeforeBindCallback;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Michael J. Simons
 * @soundtrack Motörhead - We Are Motörhead
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public final class Neo4jTemplate implements Neo4jOperations, BeanFactoryAware {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(Neo4jTemplate.class));

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final Neo4jClient neo4jClient;

	private final Neo4jMappingContext neo4jMappingContext;

	private final CypherGenerator cypherGenerator;

	private Neo4jEvents eventSupport;

	public Neo4jTemplate(Neo4jClient neo4jClient) {
		this(neo4jClient, new Neo4jMappingContext());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
		this.eventSupport = new Neo4jEvents(null);
	}

	@Override
	public long count(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData)
			.returning(Functions.count(asterisk())).build();

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(renderer.render(statement))
			.build();
		return toExecutableQuery(preparedQuery)
			.getRequiredSingleResult();
	}

	@Override
	public long count(Statement statement, Map<String, Object> parameters) {

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.build();
		return toExecutableQuery(preparedQuery).getRequiredSingleResult();
	}

	@Override
	public <T> List<T> findAll(Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData)
			.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();
		return createExecutableQuery(domainType, statement).getResults();
	}

	@Override
	public <T> List<T> findAll(Statement statement, Class<T> domainType) {
		return createExecutableQuery(domainType, statement).getResults();
	}

	@Override
	public <T> List<T> findAll(Statement statement, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, statement, parameters).getResults();
	}

	@Override
	public <T> Optional<T> findOne(Statement statement, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, statement, parameters).getSingleResult();
	}

	@Override
	public <T> Optional<T> findById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator
			.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().isEqualTo(literalOf(id)))
			.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData))
			.build();
		return createExecutableQuery(domainType, statement).getSingleResult();
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator
			.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().in((parameter("ids"))))
			.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData))
			.build();

		return createExecutableQuery(domainType, statement, singletonMap("ids", ids)).getResults();
	}

	@Override
	public <T> T save(T instance) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(instance.getClass());
		T entityToBeSaved = eventSupport.maybeCallBeforeBind(instance);
		Long internalId = neo4jClient
			.query(() -> renderer.render(cypherGenerator.prepareSaveOf(entityMetaData)))
			.bind((T) entityToBeSaved)
			.with(neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass()))
			.fetchAs(Long.class).one().get();

		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);

		if (!entityMetaData.isUsingInternalIds()) {
			processNestedAssociations(entityMetaData, entityToBeSaved);
			return entityToBeSaved;
		} else {
			propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), internalId);
			processNestedAssociations(entityMetaData, entityToBeSaved);

			return propertyAccessor.getBean();
		}
	}

	@Override
	public <T> List<T> saveAll(Iterable<T> instances) {

		Collection<T> entities;
		if (instances instanceof Collection) {
			entities = (Collection<T>) instances;
		} else {
			entities = new ArrayList<>();
			instances.forEach(entities::add);
		}

		if (entities.isEmpty()) {
			return Collections.emptyList();
		}

		Class<T> domainClass = (Class<T>) CollectionUtils.findCommonElementType(entities);
		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainClass);
		if (entityMetaData.isUsingInternalIds()) {
			log.debug("Saving entities using single statements.");

			return entities.stream()
				.map(this::save)
				.collect(toList());
		}

		List<T> entitiesToBeSaved = entities.stream()
			.map(eventSupport::maybeCallBeforeBind)
			.collect(toList());

		// Save roots
		Function<T, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(domainClass);
		List<Map<String, Object>> entityList = entitiesToBeSaved.stream()
			.map(binderFunction).collect(toList());
		ResultSummary resultSummary = neo4jClient
			.query(() -> renderer.render(cypherGenerator.prepareSaveOfMultipleInstancesOf(entityMetaData)))
			.bind(entityList).to(NAME_OF_ENTITY_LIST_PARAM)
			.run();

		// Save related
		entitiesToBeSaved.forEach(entityToBeSaved -> {
			processNestedAssociations(entityMetaData, entityToBeSaved);
		});

		SummaryCounters counters = resultSummary.counters();
		log.debug(() -> String
			.format("Created %d and deleted %d nodes, created %d and deleted %d relationships and set %d properties.",
				counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
				counters.relationshipsDeleted(), counters.propertiesSet()));

		return entitiesToBeSaved;
	}

	@Override
	public <T> void deleteById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		String nameOfParameter = "id";
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

		log.debug(() -> String.format("Deleting entity with id %d ", id));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
			.bind(id).to(nameOfParameter)
			.run();

		log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted()));
	}

	@Override
	public <T> void deleteAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		String nameOfParameter = "ids";
		Condition condition = entityMetaData.getIdExpression().in(parameter(nameOfParameter));

		log.debug(() -> String.format("Deleting all entities with the following ids: %s ", ids));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
			.bind(ids).to(nameOfParameter)
			.run();

		log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted()));
	}

	@Override
	public void deleteAll(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		log.debug(() -> String.format("Deleting all nodes with primary label %s", entityMetaData.getPrimaryLabel()));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement)).run();

		log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted()));
	}

	private <T> ExecutableQuery createExecutableQuery(Class<T> domainType, Statement statement) {
		return createExecutableQuery(domainType, statement, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Statement statement,
		Map<String, Object> parameters) {

		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.usingMappingFunction(this.neo4jMappingContext.getRequiredMappingFunctionFor(domainType))
			.build();
		return toExecutableQuery(preparedQuery);
	}

	private void processNestedAssociations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject) {

		PersistentPropertyAccessor<?> propertyAccessor = neo4jPersistentEntity.getPropertyAccessor(parentObject);
		Object fromId = propertyAccessor.getProperty(neo4jPersistentEntity.getRequiredIdProperty());

		neo4jPersistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) handler -> {

			Neo4jPersistentProperty inverse = handler.getInverse();

			Object value = propertyAccessor.getProperty(inverse);

			Class<?> associationTargetType = inverse.getAssociationTargetType();

			Neo4jPersistentEntity<?> targetNodeDescription = (Neo4jPersistentEntity<?>) neo4jMappingContext
				.getPersistentEntity(associationTargetType);

			Collection<RelationshipDescription> relationships = neo4jPersistentEntity.getRelationships();
			RelationshipDescription relationship = relationships.stream()
				.filter(r -> r.getFieldName().equals(inverse.getName()))
				.findFirst().get();

			// remove all relationships before creating all new if the entity is not new
			// this avoids the usage of cache but might have significant impact on overall performance
			if (!neo4jPersistentEntity.isNew(parentObject)) {
				Statement relationshipRemoveQuery = cypherGenerator.createRelationshipRemoveQuery(neo4jPersistentEntity,
					relationship, targetNodeDescription.getPrimaryLabel());

				neo4jClient.query(renderer.render(relationshipRemoveQuery))
					.bind(fromId).to(FROM_ID_PARAMETER_NAME).run();
			}

			if (value == null) {
				return;
			}

			for (Object relatedValue : Relationships.unifyRelationshipValue(inverse, value)) {

				Object valueToBeSaved = relatedValue instanceof Map.Entry ?
					((Map.Entry) relatedValue).getValue() :
					relatedValue;
				valueToBeSaved = eventSupport.maybeCallBeforeBind(valueToBeSaved);

				Long relatedInternalId = saveRelatedNode(valueToBeSaved, associationTargetType, targetNodeDescription);

				Statement relationshipCreationQuery = cypherGenerator
					.createRelationshipCreationQuery(neo4jPersistentEntity,
						relationship,
						relatedValue instanceof Map.Entry ? ((Map.Entry<String, ?>) relatedValue).getKey() : null,
						relatedInternalId);

				neo4jClient.query(renderer.render(relationshipCreationQuery))
					.bind(fromId).to(FROM_ID_PARAMETER_NAME).run();

				// if an internal id is used this must get set to link this entity in the next iteration
				if (targetNodeDescription.isUsingInternalIds()) {
					PersistentPropertyAccessor<?> targetPropertyAccessor = targetNodeDescription
						.getPropertyAccessor(valueToBeSaved);
					targetPropertyAccessor
						.setProperty(targetNodeDescription.getRequiredIdProperty(), relatedInternalId);
				}
				processNestedAssociations(targetNodeDescription, valueToBeSaved);
			}
		});
	}

	private <Y> Long saveRelatedNode(Object entity, Class<Y> entityType, NodeDescription targetNodeDescription) {
		return neo4jClient.query(() -> renderer.render(cypherGenerator.prepareSaveOf(targetNodeDescription)))
			.bind((Y) entity).with(neo4jMappingContext.getRequiredBinderFunctionFor(entityType))
			.fetchAs(Long.class).one().get();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		this.eventSupport = new Neo4jEvents(EntityCallbacks.create(beanFactory));
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		Neo4jClient.MappingSpec<T> mappingSpec = this
			.neo4jClient.query(preparedQuery.getCypherQuery())
			.bindAll(preparedQuery.getParameters())
			.fetchAs(preparedQuery.getResultType());
		Neo4jClient.RecordFetchSpec<T> fetchSpec = preparedQuery
			.getOptionalMappingFunction()
			.map(f -> mappingSpec.mappedBy(f))
			.orElse(mappingSpec);

		return new DefaultExecutableQuery<>(preparedQuery, fetchSpec);
	}

	final class DefaultExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;
		private final Neo4jClient.RecordFetchSpec<T> fetchSpec;

		DefaultExecutableQuery(PreparedQuery<T> preparedQuery, Neo4jClient.RecordFetchSpec<T> fetchSpec) {
			this.preparedQuery = preparedQuery;
			this.fetchSpec = fetchSpec;
		}

		public List<T> getResults() {
			return fetchSpec.all().stream().collect(toList());
		}

		public Optional<T> getSingleResult() {
			try {
				return fetchSpec.one();
			} catch (NoSuchRecordException e) {
				// This exception is thrown by the driver in both cases when there are 0 or 1+n records
				// So there has been an incorrect result size, but not to few results but to many.
				throw new IncorrectResultSizeDataAccessException(1);
			}
		}

		public T getRequiredSingleResult() {
			return fetchSpec.one()
				.orElseThrow(() -> new NoResultException(1, preparedQuery.getCypherQuery()));
		}
	}

	/**
	 * Utility class that orchestrates {@link EntityCallbacks}.
	 * All the methods provided here check for their availability and do nothing when an event cannot be published.
	 */
	final class Neo4jEvents {

		private final @Nullable EntityCallbacks entityCallbacks;

		Neo4jEvents(@Nullable EntityCallbacks entityCallbacks) {
			this.entityCallbacks = entityCallbacks;
		}

		public <T> T maybeCallBeforeBind(T object) {
			if (entityCallbacks != null) {
				return entityCallbacks.callback(BeforeBindCallback.class, object);
			}

			return object;
		}
	}
}
