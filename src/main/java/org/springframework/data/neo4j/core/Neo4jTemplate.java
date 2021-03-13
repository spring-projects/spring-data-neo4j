/*
 * Copyright 2011-2021 the original author or authors.
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

import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.asterisk;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
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
import org.springframework.data.neo4j.core.mapping.callback.EventSupport;
import org.springframework.data.neo4j.repository.NoResultException;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @author Gerrit Meier
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

	private EventSupport eventSupport;

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
		this.eventSupport = EventSupport.useExistingCallbacks(neo4jMappingContext, entityCallbacks);

		this.databaseSelectionProvider = databaseSelectionProvider;
	}

	@Override
	public long count(Class<?> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
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
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		return createExecutableQuery(domainType, QueryFragmentsAndParameters.forFindAll(entityMetaData))
				.getResults();
	}

	@Override
	public <T> List<T> findAll(Statement statement, Class<T> domainType) {
		return createExecutableQuery(domainType, renderer.render(statement)).getResults();
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

		return createExecutableQuery(domainType,
				QueryFragmentsAndParameters.forFindById(entityMetaData,
						convertIdValues(entityMetaData.getRequiredIdProperty(), id)))
				.getSingleResult();
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> domainType) {
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);

		return createExecutableQuery(domainType,
				QueryFragmentsAndParameters.forFindByAllId(
						entityMetaData, convertIdValues(entityMetaData.getRequiredIdProperty(), ids)))
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

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(instance.getClass());
		boolean isEntityNew = entityMetaData.isNew(instance);

		T entityToBeSaved = eventSupport.maybeCallBeforeBind(instance);

		DynamicLabels dynamicLabels = determineDynamicLabels(entityToBeSaved, entityMetaData, inDatabase);

		Optional<Long> optionalInternalId = neo4jClient
				.query(() -> renderer.render(cypherGenerator.prepareSaveOf(entityMetaData, dynamicLabels)))
				.in(inDatabase)
				.bind(entityToBeSaved)
				.with(neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass()))
				.fetchAs(Long.class).one();

		if (entityMetaData.hasVersionProperty() && !optionalInternalId.isPresent()) {
			throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
		}

		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
		if (entityMetaData.isUsingInternalIds()) {
			propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), optionalInternalId.get());
			entityToBeSaved = propertyAccessor.getBean();
		}
		return processRelations(entityMetaData, entityToBeSaved, isEntityNew, inDatabase);
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
		if (entityMetaData.isUsingInternalIds() || entityMetaData.hasVersionProperty()
				|| entityMetaData.getDynamicLabelsProperty().isPresent()) {
			log.debug("Saving entities using single statements.");

			return entities.stream().map(e -> saveImpl(e, databaseName)).collect(Collectors.toList());
		}

		// we need to determine the `isNew` state of the entities before calling the id generator
		List<Boolean> isNewIndicator = entities.stream().map(entity ->
			neo4jMappingContext.getPersistentEntity(entity.getClass()).isNew(entity)
		).collect(Collectors.toList());

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
		entitiesToBeSaved.forEach(entityToBeSaved -> processRelations(entityMetaData, entityToBeSaved,
				isNewIndicator.get(entitiesToBeSaved.indexOf(entityToBeSaved)), databaseName));

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
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
				.withCypherQuery(cypherStatement)
				.withParameters(parameters)
				.usingMappingFunction(neo4jMappingContext.getRequiredMappingFunctionFor(domainType))
				.build();

		return toExecutableQuery(preparedQuery);
	}

	private <T> T processRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject,
			boolean isParentObjectNew, @Nullable String inDatabase) {

		return processNestedRelations(neo4jPersistentEntity, parentObject, isParentObjectNew, inDatabase,
				new NestedRelationshipProcessingStateMachine());
	}

	private <T> T processNestedRelations(Neo4jPersistentEntity<?> sourceEntity, Object parentObject,
			boolean isParentObjectNew, @Nullable String inDatabase, NestedRelationshipProcessingStateMachine stateMachine) {

		PersistentPropertyAccessor<?> propertyAccessor = sourceEntity.getPropertyAccessor(parentObject);
		Object fromId = propertyAccessor.getProperty(sourceEntity.getRequiredIdProperty());

		sourceEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) association -> {

			// create context to bundle parameters
			NestedRelationshipContext relationshipContext = NestedRelationshipContext.of(association, propertyAccessor,
					sourceEntity);

			Collection<?> relatedValuesToStore = MappingSupport.unifyRelationshipValue(relationshipContext.getInverse(),
					relationshipContext.getValue());

			RelationshipDescription relationshipDescription = relationshipContext.getRelationship();
			RelationshipDescription relationshipDescriptionObverse = relationshipDescription.getRelationshipObverse();

			Neo4jPersistentProperty idProperty;
			if (!relationshipDescription.hasInternalIdProperty()) {
				idProperty = null;
			} else {
				Neo4jPersistentEntity<?> relationshipPropertiesEntity = (Neo4jPersistentEntity<?>) relationshipDescription.getRelationshipPropertiesEntity();
				idProperty = relationshipPropertiesEntity.getIdProperty();
			}

			// break recursive procession and deletion of previously created relationships
			ProcessState processState = stateMachine.getStateOf(relationshipDescriptionObverse, relatedValuesToStore);
			if (processState == ProcessState.PROCESSED_ALL_RELATIONSHIPS) {
				return;
			}

			// remove all relationships before creating all new if the entity is not new
			// this avoids the usage of cache but might have significant impact on overall performance
			if (!isParentObjectNew) {

				List<Long> knownRelationshipsIds = new ArrayList<>();
				if (idProperty != null) {
					for (Object relatedValueToStore : relatedValuesToStore) {
						if (relatedValueToStore == null) {
							continue;
						}

						Long id = (Long) relationshipContext.getRelationshipPropertiesPropertyAccessor(relatedValueToStore).getProperty(idProperty);
						if (id != null) {
							knownRelationshipsIds.add(id);
						}
					}
				}

				Statement relationshipRemoveQuery = cypherGenerator.prepareDeleteOf(sourceEntity, relationshipDescription);

				neo4jClient.query(renderer.render(relationshipRemoveQuery)).in(inDatabase)
						.bind(convertIdValues(sourceEntity.getIdProperty(), fromId)) //
							.to(Constants.FROM_ID_PARAMETER_NAME) //
						.bind(knownRelationshipsIds) //
							.to(Constants.NAME_OF_KNOWN_RELATIONSHIPS_PARAM) //
						.run();
			}

			// nothing to do because there is nothing to map
			if (relationshipContext.inverseValueIsEmpty()) {
				return;
			}

			stateMachine.markAsProcessed(relationshipDescription, relatedValuesToStore);

			for (Object relatedValueToStore : relatedValuesToStore) {

				// here a map entry is not always anymore a dynamic association
				Object relatedNode = relationshipContext.identifyAndExtractRelationshipTargetNode(relatedValueToStore);
				Neo4jPersistentEntity<?> targetEntity = neo4jMappingContext.getPersistentEntity(relatedNode.getClass());

				boolean isEntityNew = targetEntity.isNew(relatedNode);

				relatedNode = eventSupport.maybeCallBeforeBind(relatedNode);

				Long relatedInternalId = saveRelatedNode(relatedNode, relationshipContext.getAssociationTargetType(),
						targetEntity, inDatabase);

				CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatement(
						sourceEntity, relationshipContext, relatedValueToStore);

				Optional<Long> relationshipInternalId = neo4jClient.query(renderer.render(statementHolder.getStatement())).in(inDatabase)
						.bind(convertIdValues(sourceEntity.getRequiredIdProperty(), fromId)) //
							.to(Constants.FROM_ID_PARAMETER_NAME)
						.bind(relatedInternalId) //
							.to(Constants.TO_ID_PARAMETER_NAME) //
						.bindAll(statementHolder.getProperties())
						.fetchAs(Long.class).one();

				if (idProperty != null) {
					relationshipContext
							.getRelationshipPropertiesPropertyAccessor(relatedValueToStore)
							.setProperty(idProperty, relationshipInternalId.get());
				}

				PersistentPropertyAccessor<?> targetPropertyAccessor = targetEntity.getPropertyAccessor(relatedNode);
				// if an internal id is used this must get set to link this entity in the next iteration
				if (targetEntity.isUsingInternalIds()) {
					targetPropertyAccessor.setProperty(targetEntity.getRequiredIdProperty(), relatedInternalId);
				}
				if (processState != ProcessState.PROCESSED_ALL_VALUES) {
					processNestedRelations(targetEntity, targetPropertyAccessor.getBean(), isEntityNew, inDatabase, stateMachine);
				}
			}


		});

		return (T) propertyAccessor.getBean();
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

		this.eventSupport = EventSupport.discoverCallbacks(neo4jMappingContext, beanFactory);
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(Class<T> domainType,
													QueryFragmentsAndParameters queryFragmentsAndParameters) {

		return createExecutableQuery(domainType, queryFragmentsAndParameters);
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType,
														 QueryFragmentsAndParameters queryFragmentsAndParameters) {

		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
				.withQueryFragmentsAndParameters(queryFragmentsAndParameters)
				.usingMappingFunction(neo4jMappingContext.getRequiredMappingFunctionFor(domainType))
				.build();
		return toExecutableQuery(preparedQuery);
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		return new DefaultExecutableQuery<>(preparedQuery);
	}

	final class DefaultExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;

		DefaultExecutableQuery(PreparedQuery<T> preparedQuery) {
			this.preparedQuery = preparedQuery;
		}

		@SuppressWarnings("unchecked")
		public List<T> getResults() {
			Collection<T> all = createFetchSpec().map(Neo4jClient.RecordFetchSpec::all).orElse(Collections.emptyList());
			if (preparedQuery.resultsHaveBeenAggregated()) {
				return all.stream().flatMap(nested -> ((Collection<T>) nested).stream()).distinct().collect(Collectors.toList());
			}
			return all.stream().collect(Collectors.toList());
		}

		public Optional<T> getSingleResult() {
			try {
				Optional<T> one = createFetchSpec().flatMap(Neo4jClient.RecordFetchSpec::one);
				if (preparedQuery.resultsHaveBeenAggregated()) {
					return one.map(aggregatedResults -> (T) ((LinkedHashSet<?>) aggregatedResults).iterator().next());
				}
				return one;
			} catch (NoSuchRecordException e) {
				// This exception is thrown by the driver in both cases when there are 0 or 1+n records
				// So there has been an incorrect result size, but not to few results but to many.
				throw new IncorrectResultSizeDataAccessException(e.getMessage(), 1);
			}
		}

		public T getRequiredSingleResult() {
			Optional<T> one = createFetchSpec().flatMap(Neo4jClient.RecordFetchSpec::one);
			if (preparedQuery.resultsHaveBeenAggregated()) {
				one = one.map(aggregatedResults -> (T) ((LinkedHashSet<?>) aggregatedResults).iterator().next());
			}
			return one.orElseThrow(() -> new NoResultException(1, preparedQuery.getQueryFragmentsAndParameters().getCypherQuery()));
		}

		private Optional<Neo4jClient.RecordFetchSpec<T>> createFetchSpec() {
			QueryFragmentsAndParameters queryFragmentsAndParameters = preparedQuery.getQueryFragmentsAndParameters();
			String cypherQuery = queryFragmentsAndParameters.getCypherQuery();
			Map<String, Object> finalParameters = preparedQuery.getQueryFragmentsAndParameters().getParameters();

			QueryFragmentsAndParameters.QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();
			Neo4jPersistentEntity<?> entityMetaData = (Neo4jPersistentEntity<?>) queryFragmentsAndParameters.getNodeDescription();

			boolean containsPossibleCircles = entityMetaData != null && entityMetaData.containsPossibleCircles(queryFragments::includeField);
			if (cypherQuery == null || containsPossibleCircles) {

				Map<String, Object> parameters = queryFragmentsAndParameters.getParameters();

				if (containsPossibleCircles && !queryFragments.isScalarValueReturn()) {
					GenericQueryAndParameters genericQueryAndParameters =
							createQueryAndParameters(entityMetaData, queryFragments, parameters);

					if (genericQueryAndParameters.isEmpty()) {
						return Optional.empty();
					}
					cypherQuery = renderer.render(queryFragments.toGenericStatement());
					finalParameters = genericQueryAndParameters.getParameters();
				} else {
					cypherQuery = renderer.render(queryFragments.toStatement());
				}

			}

			Neo4jClient.MappingSpec<T> newMappingSpec = neo4jClient.query(cypherQuery)
					.in(getDatabaseName()).bindAll(finalParameters).fetchAs(preparedQuery.getResultType());
			return Optional.of(preparedQuery.getOptionalMappingFunction()
					.map(f -> newMappingSpec.mappedBy(f)).orElse(newMappingSpec));
		}

		private GenericQueryAndParameters createQueryAndParameters(Neo4jPersistentEntity<?> entityMetaData,
						   QueryFragmentsAndParameters.QueryFragments queryFragments, Map<String, Object> parameters) {

			// first check if the root node(s) exist(s) at all
			Statement rootNodesStatement = cypherGenerator
					.prepareMatchOf(entityMetaData, queryFragments.getMatchOn(), queryFragments.getCondition())
					.returning(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE).build();

			final Collection<Long> rootNodeIds = new HashSet<>((Collection<Long>) neo4jClient
					.query(renderer.render(rootNodesStatement)).in(getDatabaseName())
					.bindAll(parameters)
					.fetch()
					.one()
					.map(values -> values.get(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE))
					.get());

			if (rootNodeIds.isEmpty()) {
				// fast return if no matching root node(s) are found
				return GenericQueryAndParameters.EMPTY;
			}
			// load first level relationships
			final Set<Long> relationshipIds = new HashSet<>();
			final Set<Long> relatedNodeIds = new HashSet<>();

			for (RelationshipDescription relationshipDescription : entityMetaData.getRelationshipsInHierarchy(fieldName -> queryFragments.includeField(fieldName))) {

				Statement statement = cypherGenerator
						.prepareMatchOf(entityMetaData, relationshipDescription, queryFragments.getMatchOn(), queryFragments.getCondition())
						.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

				neo4jClient.query(renderer.render(statement)).in(getDatabaseName())
						.bindAll(parameters)
						.fetch()
						.one()
						.ifPresent(iterateAndMapNextLevel(relationshipIds, relatedNodeIds, relationshipDescription));
			}

			return new GenericQueryAndParameters(rootNodeIds, relationshipIds, relatedNodeIds);
		}

		private void iterateNextLevel(Collection<Long> nodeIds, Neo4jPersistentEntity<?> target, Set<Long> relationshipIds,
									  Set<Long> relatedNodeIds) {

			Collection<RelationshipDescription> relationships = target.getRelationshipsInHierarchy(s -> true);
			for (RelationshipDescription relationshipDescription : relationships) {

				Node node = anyNode(Constants.NAME_OF_ROOT_NODE);

				Statement statement = cypherGenerator
						.prepareMatchOf(target, relationshipDescription, null,
								Functions.id(node).in(Cypher.parameter(Constants.NAME_OF_IDS)))
						.returning(cypherGenerator.createGenericReturnStatement()).build();

				neo4jClient.query(renderer.render(statement)).in(getDatabaseName())
						.bindAll(Collections.singletonMap(Constants.NAME_OF_IDS, nodeIds))
						.fetch()
						.one()
						.ifPresent(iterateAndMapNextLevel(relationshipIds, relatedNodeIds, relationshipDescription));
			}
		}

		@NonNull
		private Consumer<Map<String, Object>> iterateAndMapNextLevel(Set<Long> relationshipIds,
																	 Set<Long> relatedNodeIds, RelationshipDescription relationshipDescription) {

			return record -> {
				List<Long> newRelationshipIds = (List<Long>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATIONS);
				relationshipIds.addAll(newRelationshipIds);

				List<Long> newRelatedNodeIds = (List<Long>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES);

				Set<Long> relatedIds = new HashSet<>(newRelatedNodeIds);
				// use this list to get down the road
				// 1. remove already visited ones;
				relatedIds.removeAll(relatedNodeIds);
				relatedNodeIds.addAll(relatedIds);
				// 2. for the rest start the exploration
				if (!relatedIds.isEmpty()) {
					iterateNextLevel(relatedIds, (Neo4jPersistentEntity<?>) relationshipDescription.getTarget(),
							relationshipIds, relatedNodeIds);
				}
			};
		}
	}

}
