/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
import static org.neo4j.springframework.data.core.RelationshipStatementHolder.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.schema.Constants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @soundtrack Motörhead - We Are Motörhead
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public final class Neo4jTemplate implements Neo4jOperations, BeanFactoryAware {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(Neo4jTemplate.class));

	private static final String OPTIMISTIC_LOCKING_ERROR_MESSAGE = "An entity with the required version does not exist.";

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final Neo4jClient neo4jClient;

	private final Neo4jMappingContext neo4jMappingContext;

	private final CypherGenerator cypherGenerator;

	private Neo4jEvents eventSupport;

	private final DatabaseSelectionProvider databaseSelectionProvider;

	public Neo4jTemplate(Neo4jClient neo4jClient) {
		this(neo4jClient, new Neo4jMappingContext(), DatabaseSelectionProvider.getDefaultSelectionProvider());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext, DatabaseSelectionProvider databaseSelectionProvider) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");
		Assert.notNull(databaseSelectionProvider, "The database name provider is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
		this.eventSupport = new Neo4jEvents(EntityCallbacks.create());

		this.databaseSelectionProvider = databaseSelectionProvider;
	}

	@Override
	public long count(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData)
			.returning(Functions.count(asterisk())).build();

		return count(statement);
	}

	@Override
	public long count(Statement statement) {
		return count(statement, emptyMap());
	}

	@Override
	public long count(Statement statement, Map<String, Object> parameters) {
		return count(renderer.render(statement), parameters);
	}

	@Override
	public long count(String cypherQuery) {
		return count(cypherQuery, emptyMap());
	}

	@Override
	public long count(String cypherQuery, Map<String, Object> parameters) {

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(cypherQuery)
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
	public <T> List<T> findAll(String cypherQuery, Class<T> domainType) {
		return createExecutableQuery(domainType, cypherQuery).getResults();
	}

	@Override
	public <T> List<T> findAll(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, cypherQuery, parameters).getResults();
	}

	@Override
	public <T> Optional<T> findOne(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, cypherQuery, parameters).getSingleResult();
	}

	@Override
	public <T> Optional<T> findById(Object id, Class<T> domainType) {
		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator
			.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().isEqualTo(parameter(NAME_OF_ID)))
			.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData))
			.build();
		return createExecutableQuery(domainType, statement, singletonMap(NAME_OF_ID, convertIdValues(id))).getSingleResult();
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> domainType) {
		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator
			.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().in((parameter(NAME_OF_IDS))))
			.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData))
			.build();

		return createExecutableQuery(domainType, statement, singletonMap(NAME_OF_IDS, convertIdValues(ids))).getResults();
	}

	private Object convertIdValues(Object idValues) {

		return neo4jMappingContext.getConverter()
			.writeValueFromProperty(idValues, ClassTypeInformation.from(idValues.getClass()));
	}

	@Override
	public <T> T save(T instance) {

		return saveImpl(instance, getDatabaseName());
	}

	private <T> T saveImpl(T instance, @Nullable String inDatabase) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(instance.getClass());
		T entityToBeSaved = eventSupport.maybeCallBeforeBind(instance);
		Optional<Long> optionalInternalId = neo4jClient
			.query(() -> renderer.render(cypherGenerator.prepareSaveOf(entityMetaData)))
			.in(inDatabase)
			.bind((T) entityToBeSaved)
			.with(neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass()))
			.fetchAs(Long.class).one();

		if (entityMetaData.hasVersionProperty() && !optionalInternalId.isPresent()) {
			throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
		}

		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
		if (!entityMetaData.isUsingInternalIds()) {
			processAssociations(entityMetaData, entityToBeSaved, inDatabase);
			return entityToBeSaved;
		} else {
			propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), optionalInternalId.get());
			processAssociations(entityMetaData, entityToBeSaved, inDatabase);

			return propertyAccessor.getBean();
		}
	}

	@Override
	public <T> List<T> saveAll(Iterable<T> instances) {

		String databaseName = getDatabaseName();

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
		if (entityMetaData.isUsingInternalIds() || entityMetaData.hasVersionProperty()) {
			log.debug("Saving entities using single statements.");

			return entities.stream()
				.map(e -> saveImpl(e, databaseName))
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
			.in(databaseName)
			.bind(entityList).to(NAME_OF_ENTITY_LIST_PARAM)
			.run();

		// Save related
		entitiesToBeSaved.forEach(entityToBeSaved -> {
			processAssociations(entityMetaData, entityToBeSaved, databaseName);
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

		log.debug(() -> String.format("Deleting entity with id %s ", id));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
			.in(getDatabaseName())
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
			.in(getDatabaseName())
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
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement)).in(getDatabaseName()).run();

		log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted()));
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Statement statement) {
		return createExecutableQuery(domainType, statement, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, String cypherStatement) {
		return createExecutableQuery(domainType, cypherStatement, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Statement statement,
		Map<String, Object> parameters) {

		return createExecutableQuery(domainType, renderer.render(statement), parameters);
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, String cypherStatement,
		Map<String, Object> parameters) {

		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
			.withCypherQuery(cypherStatement)
			.withParameters(parameters)
			.usingMappingFunction(neo4jMappingContext.getRequiredMappingFunctionFor(domainType))
			.build();
		return toExecutableQuery(preparedQuery);
	}

	private void processAssociations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject,
		@Nullable String inDatabase) {
		processNestedAssociations(neo4jPersistentEntity, parentObject, inDatabase, new HashSet<>());
	}

	private void processNestedAssociations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject,
		@Nullable String inDatabase, Set<RelationshipDescription> processedRelationshipDescriptions) {

		PersistentPropertyAccessor<?> propertyAccessor = neo4jPersistentEntity.getPropertyAccessor(parentObject);

		neo4jPersistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) handler -> {

			// create context to bundle parameters
			NestedRelationshipContext relationshipContext = NestedRelationshipContext
				.of(handler, propertyAccessor, neo4jPersistentEntity);

			// break recursive procession and deletion of previously created relationships
			RelationshipDescription relationshipObverse = relationshipContext.getRelationship().getRelationshipObverse();
			if (hasProcessed(processedRelationshipDescriptions, relationshipObverse)) {
				return;
			}

			Neo4jPersistentEntity<?> relationshipsToRemoveDescription = neo4jMappingContext
				.getPersistentEntity(relationshipContext.getAssociationTargetType());

			Object fromId = propertyAccessor.getProperty(neo4jPersistentEntity.getRequiredIdProperty());
			// remove all relationships before creating all new if the entity is not new
			// this avoids the usage of cache but might have significant impact on overall performance
			if (!neo4jPersistentEntity.isNew(parentObject)) {
				Statement relationshipRemoveQuery = cypherGenerator.createRelationshipRemoveQuery(neo4jPersistentEntity,
					relationshipContext.getRelationship(), relationshipsToRemoveDescription);

				neo4jClient.query(renderer.render(relationshipRemoveQuery))
					.in(inDatabase)
					.bind(fromId).to(FROM_ID_PARAMETER_NAME).run();
			}

			// nothing to do because there is nothing to map
			if (relationshipContext.inverseValueIsEmpty()) {
				return;
			}

			processedRelationshipDescriptions.add(relationshipContext.getRelationship());

			for (Object relatedValue : Relationships
				.unifyRelationshipValue(relationshipContext.getInverse(), relationshipContext.getValue())) {

				// here map entry is not always anymore a dynamic association
				Object valueToBeSaved = relationshipContext.identifyAndExtractRelationshipValue(relatedValue);

				Neo4jPersistentEntity<?> targetNodeDescription = neo4jMappingContext.getPersistentEntity(valueToBeSaved.getClass());

				valueToBeSaved = eventSupport.maybeCallBeforeBind(valueToBeSaved);

				Long relatedInternalId = saveRelatedNode(valueToBeSaved, relationshipContext.getAssociationTargetType(),
					targetNodeDescription, inDatabase);

				// handle creation of relationship depending on properties on relationship or not
				RelationshipStatementHolder statementHolder = relationshipContext.hasRelationshipWithProperties()
					? createStatementForRelationShipWithProperties(neo4jMappingContext,
						neo4jPersistentEntity,
						relationshipContext,
						relatedInternalId,
						(Map.Entry) relatedValue)
					: createStatementForRelationshipWithoutProperties(neo4jPersistentEntity,
						relationshipContext,
						relatedInternalId,
						relatedValue);

				neo4jClient.query(renderer.render(statementHolder.getRelationshipCreationQuery()))
					.in(inDatabase)
					.bind(fromId).to(FROM_ID_PARAMETER_NAME)
					.bindAll(statementHolder.getProperties())
					.run();

				// if an internal id is used this must get set to link this entity in the next iteration
				if (targetNodeDescription.isUsingInternalIds()) {
					PersistentPropertyAccessor<?> targetPropertyAccessor = targetNodeDescription
						.getPropertyAccessor(valueToBeSaved);
					targetPropertyAccessor
						.setProperty(targetNodeDescription.getRequiredIdProperty(), relatedInternalId);
				}
				processNestedAssociations(targetNodeDescription, valueToBeSaved, inDatabase, processedRelationshipDescriptions);
			}
		});
	}

	private boolean hasProcessed(Set<RelationshipDescription> processedRelationshipDescriptions,
		RelationshipDescription relationshipDescription) {

		if (relationshipDescription != null) {
			return processedRelationshipDescriptions.contains(relationshipDescription);
		}
		return false;
	}

	private <Y> Long saveRelatedNode(Object entity, Class<Y> entityType, NodeDescription targetNodeDescription, @Nullable String inDatabase) {
		Optional<Long> optionalSavedNodeId = neo4jClient
			.query(() -> renderer.render(cypherGenerator.prepareSaveOf(targetNodeDescription)))
			.in(inDatabase)
			.bind((Y) entity).with(neo4jMappingContext.getRequiredBinderFunctionFor(entityType))
			.fetchAs(Long.class).one();

		if (((Neo4jPersistentEntity) targetNodeDescription).hasVersionProperty() && !optionalSavedNodeId.isPresent()) {
			throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
		}

		return optionalSavedNodeId.get();
	}

	private String getDatabaseName() {

		return this.databaseSelectionProvider.getDatabaseSelection().getValue();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		this.eventSupport = new Neo4jEvents(EntityCallbacks.create(beanFactory));
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		Neo4jClient.MappingSpec<T> mappingSpec = this
			.neo4jClient.query(preparedQuery.getCypherQuery())
			.in(getDatabaseName())
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

		private final EntityCallbacks entityCallbacks;

		Neo4jEvents(EntityCallbacks entityCallbacks) {
			this.entityCallbacks = entityCallbacks;
		}

		public <T> T maybeCallBeforeBind(T object) {
			return entityCallbacks.callback(BeforeBindCallback.class, object);
		}
	}
}
