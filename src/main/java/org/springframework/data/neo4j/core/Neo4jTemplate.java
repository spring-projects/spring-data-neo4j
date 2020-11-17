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
package org.springframework.data.neo4j.core;

import static org.neo4j.cypherdsl.core.Cypher.asterisk;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CreateRelationshipStatementHolder;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.MappingSupport;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.NestedRelationshipContext;
import org.springframework.data.neo4j.core.mapping.NestedRelationshipProcessingStateMachine;
import org.springframework.data.neo4j.core.mapping.NestedRelationshipProcessingStateMachine.ProcessState;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.neo4j.repository.NoResultException;
import org.springframework.data.neo4j.repository.event.BeforeBindCallback;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @soundtrack Motörhead - We Are Motörhead
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
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

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
			DatabaseSelectionProvider databaseSelectionProvider) {

		this(neo4jClient, neo4jMappingContext, databaseSelectionProvider, EntityCallbacks.create());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
			DatabaseSelectionProvider databaseSelectionProvider, EntityCallbacks entityCallbacks) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");
		Assert.notNull(databaseSelectionProvider, "The database name provider is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
		this.eventSupport = new Neo4jEvents(entityCallbacks);

		this.databaseSelectionProvider = databaseSelectionProvider;
	}

	@Override
	public long count(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData).returning(Functions.count(asterisk()))
				.build();

		return count(statement);
	}

	@Override
	public long count(Statement statement) {
		return count(statement, Collections.emptyMap());
	}

	@Override
	public long count(Statement statement, Map<String, Object> parameters) {
		return count(renderer.render(statement), parameters);
	}

	@Override
	public long count(String cypherQuery) {
		return count(cypherQuery, Collections.emptyMap());
	}

	@Override
	public long count(String cypherQuery, Map<String, Object> parameters) {

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class).withCypherQuery(cypherQuery)
				.withParameters(parameters).build();
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
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator
				.prepareMatchOf(entityMetaData,
						entityMetaData.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID)))
				.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();
		return createExecutableQuery(domainType, statement, Collections
				.singletonMap(Constants.NAME_OF_ID, convertIdValues(entityMetaData.getRequiredIdProperty(), id)))
				.getSingleResult();
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> domainType) {
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator
				.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().in((parameter(Constants.NAME_OF_IDS))))
				.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

		return createExecutableQuery(domainType, statement, Collections
				.singletonMap(Constants.NAME_OF_IDS, convertIdValues(entityMetaData.getRequiredIdProperty(), ids)))
				.getResults();
	}

	private Object convertIdValues(@Nullable Neo4jPersistentProperty idProperty, Object idValues) {

		return neo4jMappingContext.getConversionService().writeValue(idValues,
				ClassTypeInformation.from(idValues.getClass()),
				idProperty == null ? null : idProperty.getOptionalWritingConverter());
	}

	@Override
	public <T> T save(T instance) {

		return saveImpl(instance, getDatabaseName());
	}

	private <T> T saveImpl(T instance, @Nullable String inDatabase) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(instance.getClass());
		Object idBeforeCallback = entityMetaData.getPropertyAccessor(instance).getProperty(entityMetaData.getRequiredIdProperty());

		T entityToBeSaved = eventSupport.maybeCallBeforeBind(instance);

		Object idAfterCallback = entityMetaData.getPropertyAccessor(entityToBeSaved).getProperty(entityMetaData.getRequiredIdProperty());
		boolean isIdWiseNew = hasNewIdentifierValue(idBeforeCallback, idAfterCallback);

		DynamicLabels dynamicLabels = determineDynamicLabels(entityToBeSaved, entityMetaData, inDatabase);

		Optional<Long> optionalInternalId = neo4jClient
				.query(() -> renderer.render(cypherGenerator.prepareSaveOf(entityMetaData, dynamicLabels)))
				.in(inDatabase)
				.bind((T) entityToBeSaved)
				.with(neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass()))
				.fetchAs(Long.class).one();

		if (entityMetaData.hasVersionProperty() && !optionalInternalId.isPresent()) {
			throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
		}

		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
		if (!entityMetaData.isUsingInternalIds()) {
			processRelations(entityMetaData, entityToBeSaved, inDatabase, isIdWiseNew);
			return entityToBeSaved;
		} else {
			propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), optionalInternalId.get());
			processRelations(entityMetaData, entityToBeSaved, inDatabase, isIdWiseNew);

			return propertyAccessor.getBean();
		}
	}

	private boolean hasNewIdentifierValue(Object idBeforeCallback, Object idAfterCallback) {
		return idBeforeCallback == null || !idBeforeCallback.equals(idAfterCallback);
	}

	private <T> DynamicLabels determineDynamicLabels(T entityToBeSaved, Neo4jPersistentEntity<?> entityMetaData,
			@Nullable String inDatabase) {
		return entityMetaData.getDynamicLabelsProperty().map(p -> {

			PersistentPropertyAccessor propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
			Neo4jClient.RunnableSpecTightToDatabase runnableQuery = neo4jClient
					.query(() -> renderer.render(cypherGenerator.createStatementReturningDynamicLabels(entityMetaData)))
					.in(inDatabase).bind(propertyAccessor.getProperty(entityMetaData.getRequiredIdProperty()))
					.to(Constants.NAME_OF_ID).bind(entityMetaData.getStaticLabels())
					.to(Constants.NAME_OF_STATIC_LABELS_PARAM);

			if (entityMetaData.hasVersionProperty()) {
				runnableQuery = runnableQuery
						.bind((Long) propertyAccessor.getProperty(entityMetaData.getRequiredVersionProperty()) - 1)
						.to(Constants.NAME_OF_VERSION_PARAM);
			}

			Optional<Map<String, Object>> optionalResult = runnableQuery.fetch().one();
			return new DynamicLabels(optionalResult.map(r -> (Collection<String>) r.get(Constants.NAME_OF_LABELS))
					.orElseGet(Collections::emptyList), (Collection<String>) propertyAccessor.getProperty(p));
		}).orElse(DynamicLabels.EMPTY);
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

			return entities.stream().map(e -> saveImpl(e, databaseName)).collect(Collectors.toList());
		}

		List<T> entitiesToBeSaved = entities.stream().map(eventSupport::maybeCallBeforeBind)
				.collect(Collectors.toList());

		// Save roots
		Function<T, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(domainClass);
		List<Map<String, Object>> entityList = entitiesToBeSaved.stream().map(binderFunction)
				.collect(Collectors.toList());
		ResultSummary resultSummary = neo4jClient
				.query(() -> renderer.render(cypherGenerator.prepareSaveOfMultipleInstancesOf(entityMetaData)))
				.in(databaseName)
				.bind(entityList).to(Constants.NAME_OF_ENTITY_LIST_PARAM).run();

		// Save related
		entitiesToBeSaved.forEach(entityToBeSaved -> processRelations(entityMetaData, entityToBeSaved, databaseName, false));

		SummaryCounters counters = resultSummary.counters();
		log.debug(() -> String.format(
				"Created %d and deleted %d nodes, created %d and deleted %d relationships and set %d properties.",
				counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
				counters.relationshipsDeleted(), counters.propertiesSet()));

		return entitiesToBeSaved;
	}

	@Override
	public <T> void deleteById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		String nameOfParameter = "id";
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

		log.debug(() -> String.format("Deleting entity with id %s ", id));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement)).in(getDatabaseName())
				.bind(convertIdValues(entityMetaData.getRequiredIdProperty(), id))
				.to(nameOfParameter).run();

		log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
				summary.counters().relationshipsDeleted()));
	}

	@Override
	public <T> void deleteByIdWithVersion(Object id, Class<T> domainType, Neo4jPersistentProperty versionProperty,
										  Object versionValue) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);

		String nameOfParameter = "id";
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter))
				.and(Cypher.property(Constants.NAME_OF_ROOT_NODE, versionProperty.getPropertyName())
						.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))
						.or(Cypher.property(Constants.NAME_OF_ROOT_NODE, versionProperty.getPropertyName()).isNull()));

		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData, condition)
				.returning(Constants.NAME_OF_ROOT_NODE).build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(nameOfParameter, convertIdValues(entityMetaData.getRequiredIdProperty(), id));
		parameters.put(Constants.NAME_OF_VERSION_PARAM, versionValue);

		createExecutableQuery(domainType, statement, parameters).getSingleResult().orElseThrow(
				() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE)
		);

		deleteById(id, domainType);
	}

	@Override
	public <T> void deleteAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		String nameOfParameter = "ids";
		Condition condition = entityMetaData.getIdExpression().in(parameter(nameOfParameter));

		log.debug(() -> String.format("Deleting all entities with the following ids: %s ", ids));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement)).in(getDatabaseName()).bind(
				convertIdValues(entityMetaData.getRequiredIdProperty(), ids))
				.to(nameOfParameter).run();

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

		Assert.notNull(neo4jMappingContext.getPersistentEntity(domainType), "Cannot get or create persistent entity.");
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType).withCypherQuery(cypherStatement)
				.withParameters(parameters)
				.usingMappingFunction(neo4jMappingContext.getRequiredMappingFunctionFor(domainType))
				.build();
		return toExecutableQuery(preparedQuery);
	}

	private void processRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject,
			@Nullable String inDatabase, boolean isIdWiseNew) {

		processNestedRelations(neo4jPersistentEntity, parentObject, inDatabase,
				new NestedRelationshipProcessingStateMachine(), isIdWiseNew);
	}

	private void processNestedRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject,
			@Nullable String inDatabase, NestedRelationshipProcessingStateMachine stateMachine, boolean isIdWiseNew) {

		PersistentPropertyAccessor<?> propertyAccessor = neo4jPersistentEntity.getPropertyAccessor(parentObject);
		Object fromId = propertyAccessor.getProperty(neo4jPersistentEntity.getRequiredIdProperty());

		neo4jPersistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) association -> {

			// create context to bundle parameters
			NestedRelationshipContext relationshipContext = NestedRelationshipContext.of(association, propertyAccessor,
					neo4jPersistentEntity);

			Collection<?> relatedValuesToStore = MappingSupport.unifyRelationshipValue(relationshipContext.getInverse(),
					relationshipContext.getValue());

			RelationshipDescription relationshipDescription = relationshipContext.getRelationship();
			RelationshipDescription relationshipDescriptionObverse = relationshipDescription.getRelationshipObverse();

			// break recursive procession and deletion of previously created relationships
			ProcessState processState = stateMachine.getStateOf(relationshipDescriptionObverse, relatedValuesToStore);
			if (processState == ProcessState.PROCESSED_BOTH) {
				return;
			}

			// remove all relationships before creating all new if the entity is not new
			// this avoids the usage of cache but might have significant impact on overall performance
			if (!neo4jPersistentEntity.isNew(parentObject) && !isIdWiseNew) {
				Neo4jPersistentEntity<?> previouslyRelatedPersistentEntity = neo4jMappingContext
						.getPersistentEntity(relationshipContext.getAssociationTargetType());

				Statement relationshipRemoveQuery = cypherGenerator.createRelationshipRemoveQuery(neo4jPersistentEntity,
						relationshipDescription, previouslyRelatedPersistentEntity);

				neo4jClient.query(renderer.render(relationshipRemoveQuery)).in(inDatabase)
						.bind(convertIdValues(previouslyRelatedPersistentEntity.getIdProperty(), fromId))
						.to(Constants.FROM_ID_PARAMETER_NAME).run();
			}

			// nothing to do because there is nothing to map
			if (relationshipContext.inverseValueIsEmpty()) {
				return;
			}

			stateMachine.markAsProcessed(relationshipDescription, relatedValuesToStore);

			for (Object relatedValueToStore : relatedValuesToStore) {

				// here map entry is not always anymore a dynamic association
				Object relatedNode = relationshipContext.identifyAndExtractRelationshipTargetNode(relatedValueToStore);
				relatedNode = eventSupport.maybeCallBeforeBind(relatedNode);

				Neo4jPersistentEntity<?> targetNodeDescription = neo4jMappingContext
						.getPersistentEntity(relatedNode.getClass());

				Long relatedInternalId = saveRelatedNode(relatedNode, relationshipContext.getAssociationTargetType(),
						targetNodeDescription, inDatabase);

				CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatement(
						neo4jPersistentEntity, relationshipContext, relatedInternalId, relatedValueToStore);

				neo4jClient.query(renderer.render(statementHolder.getStatement())).in(inDatabase)
						.bind(convertIdValues(targetNodeDescription.getRequiredIdProperty(), fromId))
						.to(Constants.FROM_ID_PARAMETER_NAME).bindAll(statementHolder.getProperties())
						.run();

				// if an internal id is used this must get set to link this entity in the next iteration
				if (targetNodeDescription.isUsingInternalIds()) {
					PersistentPropertyAccessor<?> targetPropertyAccessor = targetNodeDescription
							.getPropertyAccessor(relatedNode);
					targetPropertyAccessor
							.setProperty(targetNodeDescription.getRequiredIdProperty(), relatedInternalId);
				}
				if (processState != ProcessState.PROCESSED_ALL_VALUES) {
					processNestedRelations(targetNodeDescription, relatedNode, inDatabase, stateMachine, isIdWiseNew);
				}
			}
		});
	}

	private <Y> Long saveRelatedNode(Object entity, Class<Y> entityType, NodeDescription targetNodeDescription,
			@Nullable String inDatabase) {

		DynamicLabels dynamicLabels = determineDynamicLabels(entity, (Neo4jPersistentEntity) targetNodeDescription,
				inDatabase);
		Optional<Long> optionalSavedNodeId = neo4jClient
				.query(() -> renderer.render(cypherGenerator.prepareSaveOf(targetNodeDescription, dynamicLabels)))
				.in(inDatabase).bind((Y) entity).with(neo4jMappingContext.getRequiredBinderFunctionFor(entityType))
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

		Neo4jClient.MappingSpec<T> mappingSpec = this.neo4jClient.query(preparedQuery.getCypherQuery())
				.in(getDatabaseName()).bindAll(preparedQuery.getParameters()).fetchAs(preparedQuery.getResultType());
		Neo4jClient.RecordFetchSpec<T> fetchSpec = preparedQuery.getOptionalMappingFunction()
				.map(f -> mappingSpec.mappedBy(f)).orElse(mappingSpec);

		return new DefaultExecutableQuery<>(preparedQuery, fetchSpec);
	}

	final class DefaultExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;
		private final Neo4jClient.RecordFetchSpec<T> fetchSpec;

		DefaultExecutableQuery(PreparedQuery<T> preparedQuery, Neo4jClient.RecordFetchSpec<T> fetchSpec) {
			this.preparedQuery = preparedQuery;
			this.fetchSpec = fetchSpec;
		}

		@SuppressWarnings("unchecked")
		public List<T> getResults() {

			Collection<T> all = fetchSpec.all();
			if (preparedQuery.resultsHaveBeenAggregated()) {
				return all.stream().flatMap(nested -> ((Collection<T>) nested).stream()).collect(Collectors.toList());
			}
			return all.stream().collect(Collectors.toList());
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
			return fetchSpec.one().orElseThrow(() -> new NoResultException(1, preparedQuery.getCypherQuery()));
		}
	}

	/**
	 * Utility class that orchestrates {@link EntityCallbacks}. All the methods provided here check for their availability
	 * and do nothing when an event cannot be published.
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
