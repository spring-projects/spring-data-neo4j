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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
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
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.neo4j.core.NestedRelationshipProcessingStateMachine.ProcessState;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.neo4j.repository.event.ReactiveBeforeBindCallback;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Philipp Tölle
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class ReactiveNeo4jTemplate implements ReactiveNeo4jOperations, BeanFactoryAware {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(ReactiveNeo4jTemplate.class));

	private static final String OPTIMISTIC_LOCKING_ERROR_MESSAGE = "An entity with the required version does not exist.";

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jMappingContext neo4jMappingContext;

	private final CypherGenerator cypherGenerator;

	private ReactiveNeo4jEvents eventSupport;

	private final ReactiveDatabaseSelectionProvider databaseSelectionProvider;

	public ReactiveNeo4jTemplate(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
			ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");
		Assert.notNull(databaseSelectionProvider, "The database selection provider is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
		this.eventSupport = new ReactiveNeo4jEvents(ReactiveEntityCallbacks.create());
		this.databaseSelectionProvider = databaseSelectionProvider;
	}

	@Override
	public Mono<Long> count(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData).returning(Functions.count(asterisk())).build();

		return count(statement);
	}

	@Override
	public Mono<Long> count(Statement statement) {
		return count(statement, Collections.emptyMap());
	}

	@Override
	public Mono<Long> count(Statement statement, Map<String, Object> parameters) {
		return count(renderer.render(statement), parameters);
	}

	@Override
	public Mono<Long> count(String cypherQuery) {
		return count(cypherQuery, Collections.emptyMap());
	}

	@Override
	public Mono<Long> count(String cypherQuery, Map<String, Object> parameters) {
		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class).withCypherQuery(cypherQuery)
				.withParameters(parameters).build();
		return this.toExecutableQuery(preparedQuery).flatMap(ExecutableQuery::getSingleResult);
	}

	@Override
	public <T> Flux<T> findAll(Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData)
				.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();
		return createExecutableQuery(domainType, statement).flatMapMany(ExecutableQuery::getResults);
	}

	@Override
	public <T> Flux<T> findAll(Statement statement, Class<T> domainType) {

		return createExecutableQuery(domainType, statement).flatMapMany(ExecutableQuery::getResults);
	}

	@Override
	public <T> Flux<T> findAll(Statement statement, Map<String, Object> parameters, Class<T> domainType) {

		return createExecutableQuery(domainType, statement, parameters).flatMapMany(ExecutableQuery::getResults);
	}

	@Override
	public <T> Mono<T> findOne(Statement statement, Map<String, Object> parameters, Class<T> domainType) {

		return createExecutableQuery(domainType, statement, parameters).flatMap(ExecutableQuery::getSingleResult);
	}

	@Override
	public <T> Flux<T> findAll(String cypherQuery, Class<T> domainType) {
		return createExecutableQuery(domainType, cypherQuery).flatMapMany(ExecutableQuery::getResults);
	}

	@Override
	public <T> Flux<T> findAll(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, cypherQuery, parameters).flatMapMany(ExecutableQuery::getResults);
	}

	@Override
	public <T> Mono<T> findOne(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, cypherQuery, parameters).flatMap(ExecutableQuery::getSingleResult);
	}

	@Override
	public <T> Mono<T> findById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator
				.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID)))
				.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

		return createExecutableQuery(domainType, statement, Collections
				.singletonMap(Constants.NAME_OF_ID, convertIdValues(id)))
				.flatMap(ExecutableQuery::getSingleResult);
	}

	@Override
	public <T> Flux<T> findAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator
				.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().in((parameter(Constants.NAME_OF_IDS))))
				.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

		return createExecutableQuery(domainType, statement, Collections
				.singletonMap(Constants.NAME_OF_IDS, convertIdValues(ids)))
				.flatMapMany(ExecutableQuery::getResults);
	}

	private Object convertIdValues(Object idValues) {

		return neo4jMappingContext.getEntityConverter().writeValueFromProperty(idValues,
				ClassTypeInformation.from(idValues.getClass()));
	}

	@Override
	public <T> Mono<T> save(T instance) {

		return getDatabaseName().flatMap(databaseName -> saveImpl(instance, databaseName.getValue()));
	}

	private <T> Mono<T> saveImpl(T instance, @Nullable String inDatabase) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(instance.getClass());
		return Mono.just(instance).flatMap(eventSupport::maybeCallBeforeBind)
				.flatMap(entity -> determineDynamicLabels(entity, entityMetaData, inDatabase)).flatMap(t -> {
					T entity = t.getT1();
					DynamicLabels dynamicLabels = t.getT2();

					Statement saveStatement = cypherGenerator.prepareSaveOf(entityMetaData, dynamicLabels);

					Mono<Long> idMono = this.neo4jClient.query(() -> renderer.render(saveStatement)).in(inDatabase)
							.bind((T) entity).with(neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entity.getClass()))
							.fetchAs(Long.class).one().switchIfEmpty(Mono.defer(() -> {
								if (entityMetaData.hasVersionProperty()) {
									return Mono.error(() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE));
								}
								return Mono.empty();
							}));

					if (!entityMetaData.isUsingInternalIds()) {
						return idMono.then(processRelations(entityMetaData, entity, inDatabase)).thenReturn(entity);
					} else {
						return idMono.map(internalId -> {
							PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entity);
							propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), internalId);

							return propertyAccessor.getBean();
						}).flatMap(
								savedEntity -> processRelations(entityMetaData, savedEntity, inDatabase).thenReturn(savedEntity));
					}
				});
	}

	private <T> Mono<Tuple2<T, DynamicLabels>> determineDynamicLabels(T entityToBeSaved,
			Neo4jPersistentEntity<?> entityMetaData, @Nullable String inDatabase) {
		return entityMetaData.getDynamicLabelsProperty().map(p -> {

			PersistentPropertyAccessor propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
			ReactiveNeo4jClient.RunnableSpecTightToDatabase runnableQuery = neo4jClient
					.query(() -> renderer.render(cypherGenerator.createStatementReturningDynamicLabels(entityMetaData)))
					.in(inDatabase).bind(propertyAccessor.getProperty(entityMetaData.getRequiredIdProperty()))
					.to(Constants.NAME_OF_ID).bind(entityMetaData.getStaticLabels()).to(Constants.NAME_OF_STATIC_LABELS_PARAM);

			if (entityMetaData.hasVersionProperty()) {
				runnableQuery = runnableQuery
						.bind((Long) propertyAccessor.getProperty(entityMetaData.getRequiredVersionProperty()) - 1)
						.to(Constants.NAME_OF_VERSION_PARAM);
			}

			return runnableQuery.fetch().one().map(m -> (Collection<String>) m.get(Constants.NAME_OF_LABELS))
					.switchIfEmpty(Mono.just(Collections.emptyList()))
					.zipWith(Mono.just((Collection<String>) propertyAccessor.getProperty(p)))
					.map(t -> Tuples.of(entityToBeSaved, new DynamicLabels(t.getT1(), t.getT2())));
		}).orElse(Mono.just(Tuples.of(entityToBeSaved, DynamicLabels.EMPTY)));
	}

	@Override
	public <T> Flux<T> saveAll(Iterable<T> instances) {

		Collection<T> entities;
		if (instances instanceof Collection) {
			entities = (Collection<T>) instances;
		} else {
			entities = new ArrayList<>();
			instances.forEach(entities::add);
		}

		if (entities.isEmpty()) {
			return Flux.empty();
		}

		Class<T> domainClass = (Class<T>) CollectionUtils.findCommonElementType(entities);
		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainClass);

		if (entityMetaData.isUsingInternalIds() || entityMetaData.hasVersionProperty()) {
			log.debug("Saving entities using single statements.");

			return getDatabaseName().flatMapMany(
					databaseName -> Flux.fromIterable(entities).flatMap(e -> this.saveImpl(e, databaseName.getValue())));
		}

		Function<T, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(domainClass);
		return getDatabaseName().flatMapMany(databaseName -> Flux.fromIterable(entities)
				.flatMap(eventSupport::maybeCallBeforeBind).collectList().flatMapMany(entitiesToBeSaved -> Mono.defer(() -> { // Defer
																																																											// the
																																																											// actual
																																																											// save
																																																											// statement
																																																											// until
																																																											// the
																																																											// previous
																																																											// flux
																																																											// completes
					List<Map<String, Object>> boundedEntityList = entitiesToBeSaved.stream().map(binderFunction)
							.collect(Collectors.toList());

					return neo4jClient
							.query(() -> renderer.render(cypherGenerator.prepareSaveOfMultipleInstancesOf(entityMetaData)))
							.in(databaseName.getValue()).bind(boundedEntityList).to(Constants.NAME_OF_ENTITY_LIST_PARAM).run();
				}).doOnNext(resultSummary -> {
					SummaryCounters counters = resultSummary.counters();
					log.debug(() -> String.format(
							"Created %d and deleted %d nodes, created %d and deleted %d relationships and set %d properties.",
							counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
							counters.relationshipsDeleted(), counters.propertiesSet()));
				}).thenMany(Flux.fromIterable(entitiesToBeSaved))));
	}

	@Override
	public <T> Mono<Void> deleteAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		String nameOfParameter = "ids";
		Condition condition = entityMetaData.getIdExpression().in(parameter(nameOfParameter));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		return getDatabaseName().flatMap(databaseName -> this.neo4jClient.query(() -> renderer.render(statement))
				.in(databaseName.getValue()).bind(convertIdValues(ids)).to(nameOfParameter).run().then());
	}

	@Override
	public <T> Mono<Void> deleteById(Object id, Class<T> domainType) {

		Assert.notNull(id, "The given id must not be null!");

		String nameOfParameter = "id";
		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		return getDatabaseName().flatMap(databaseName -> this.neo4jClient.query(() -> renderer.render(statement))
				.in(databaseName.getValue()).bind(convertIdValues(id)).to(nameOfParameter).run().then());
	}

	@Override
	public Mono<Void> deleteAll(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData);
		return getDatabaseName().flatMap(databaseName -> this.neo4jClient.query(() -> renderer.render(statement))
				.in(databaseName.getValue()).run().then());
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, Statement statement) {
		return createExecutableQuery(domainType, statement, Collections.emptyMap());
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, String cypherQuery) {
		return createExecutableQuery(domainType, cypherQuery, Collections.emptyMap());
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, Statement statement,
			Map<String, Object> parameters) {

		return createExecutableQuery(domainType, renderer.render(statement), parameters);
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, String cypherQuery,
			Map<String, Object> parameters) {

		Assert.notNull(neo4jMappingContext.getPersistentEntity(domainType), "Cannot get or create persistent entity.");
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType).withCypherQuery(cypherQuery)
				.withParameters(parameters)
				.usingMappingFunction(this.neo4jMappingContext.getRequiredMappingFunctionFor(domainType)).build();
		return this.toExecutableQuery(preparedQuery);
	}

	private Mono<Void> processRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject,
			@Nullable String inDatabase) {

		return processNestedRelations(neo4jPersistentEntity, parentObject, inDatabase,
				new NestedRelationshipProcessingStateMachine());
	}

	private Mono<Void> processNestedRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject,
			@Nullable String inDatabase, NestedRelationshipProcessingStateMachine stateMachine) {

		return Mono.defer(() -> {
			PersistentPropertyAccessor<?> propertyAccessor = neo4jPersistentEntity.getPropertyAccessor(parentObject);
			Object fromId = propertyAccessor.getProperty(neo4jPersistentEntity.getRequiredIdProperty());
			List<Mono<Void>> relationshipCreationMonos = new ArrayList<>();

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
				if (!neo4jPersistentEntity.isNew(parentObject)) {
					Neo4jPersistentEntity<?> previouslyRelatedPersistentEntity = neo4jMappingContext
							.getPersistentEntity(relationshipContext.getAssociationTargetType());

					Statement relationshipRemoveQuery = cypherGenerator.createRelationshipRemoveQuery(neo4jPersistentEntity,
							relationshipDescription, previouslyRelatedPersistentEntity);
					relationshipCreationMonos.add(
							neo4jClient.query(renderer.render(relationshipRemoveQuery)).in(inDatabase).bind(convertIdValues(fromId))
									.to(Constants.FROM_ID_PARAMETER_NAME).run().checkpoint("delete relationships").then());
				}

				// nothing to do because there is nothing to map
				if (relationshipContext.inverseValueIsEmpty()) {
					return;
				}

				stateMachine.markAsProcessed(relationshipDescription, relatedValuesToStore);

				for (Object relatedValueToStore : relatedValuesToStore) {

					Object valueToBeSavedPreEvt = relationshipContext.identifyAndExtractRelationshipTargetNode(relatedValueToStore);

					Mono<Void> createRelationship = eventSupport.maybeCallBeforeBind(valueToBeSavedPreEvt)
							.flatMap(valueToBeSaved -> {
								Neo4jPersistentEntity<?> targetNodeDescription = neo4jMappingContext
										.getPersistentEntity(valueToBeSavedPreEvt.getClass());
								return saveRelatedNode(valueToBeSaved, relationshipContext.getAssociationTargetType(),
										targetNodeDescription, inDatabase).flatMap(relatedInternalId -> {

											// if an internal id is used this must get set to link this entity in the next iteration
											if (targetNodeDescription.isUsingInternalIds()) {
												PersistentPropertyAccessor<?> targetPropertyAccessor = targetNodeDescription
														.getPropertyAccessor(valueToBeSaved);
												targetPropertyAccessor.setProperty(targetNodeDescription.getRequiredIdProperty(),
														relatedInternalId);
											}

											RelationshipStatementHolder statementHolder = RelationshipStatementHolder.createStatement(
													neo4jMappingContext, neo4jPersistentEntity, relationshipContext, relatedInternalId,
													relatedValueToStore);

											// in case of no properties the bind will just return an empty map
											Mono<ResultSummary> relationshipCreationMonoNested = neo4jClient
													.query(renderer.render(statementHolder.getRelationshipCreationQuery())).in(inDatabase)
													.bind(convertIdValues(fromId)).to(Constants.FROM_ID_PARAMETER_NAME)
													.bindAll(statementHolder.getProperties()).run();

											if (processState != ProcessState.PROCESSED_ALL_VALUES) {
												return relationshipCreationMonoNested.checkpoint().then(
														processNestedRelations(targetNodeDescription, valueToBeSaved, inDatabase, stateMachine));
											} else {
												return relationshipCreationMonoNested.checkpoint().then();
											}
										}).checkpoint();
							});
					relationshipCreationMonos.add(createRelationship);
				}
			});

			return Flux.concat(relationshipCreationMonos).checkpoint().then();
		});
	}

	private <Y> Mono<Long> saveRelatedNode(Object relatedNode, Class<Y> entityType, NodeDescription targetNodeDescription,
			@Nullable String inDatabase) {

		return determineDynamicLabels((Y) relatedNode, (Neo4jPersistentEntity<?>) targetNodeDescription, inDatabase)
				.flatMap(t -> {
					Y entity = t.getT1();
					DynamicLabels dynamicLabels = t.getT2();

					return neo4jClient
							.query(() -> renderer.render(cypherGenerator.prepareSaveOf(targetNodeDescription, dynamicLabels)))
							.in(inDatabase).bind((Y) entity).with(neo4jMappingContext.getRequiredBinderFunctionFor(entityType))
							.fetchAs(Long.class).one();
				}).switchIfEmpty(Mono.defer(() -> {
					if (((Neo4jPersistentEntity) targetNodeDescription).hasVersionProperty()) {
						return Mono.error(() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE));
					}
					return Mono.empty();
				}));
	}

	private Mono<DatabaseSelection> getDatabaseName() {

		return this.databaseSelectionProvider.getDatabaseSelection()
				.switchIfEmpty(Mono.just(DatabaseSelection.undecided()));
	}

	@Override
	public <T> Mono<ExecutableQuery<T>> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		return getDatabaseName().map(databaseName -> {
			Class<T> resultType = preparedQuery.getResultType();
			ReactiveNeo4jClient.MappingSpec<T> mappingSpec = this.neo4jClient.query(preparedQuery.getCypherQuery())
					.in(databaseName.getValue()).bindAll(preparedQuery.getParameters()).fetchAs(resultType);

			ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec = preparedQuery.getOptionalMappingFunction()
					.map(mappingFunction -> mappingSpec.mappedBy(mappingFunction)).orElse(mappingSpec);

			return new DefaultReactiveExecutableQuery<>(fetchSpec);
		});
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		this.eventSupport = new ReactiveNeo4jEvents(ReactiveEntityCallbacks.create(beanFactory));
	}

	final class DefaultReactiveExecutableQuery<T> implements ExecutableQuery<T> {

		private final ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec;

		DefaultReactiveExecutableQuery(ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec) {
			this.fetchSpec = fetchSpec;
		}

		/**
		 * @return All results returned by this query.
		 */
		public Flux<T> getResults() {
			return fetchSpec.all();
		}

		/**
		 * @return A single result
		 * @throws IncorrectResultSizeDataAccessException if there is no or more than one result
		 */
		public Mono<T> getSingleResult() {
			try {
				return fetchSpec.one();
			} catch (NoSuchRecordException e) {
				// This exception is thrown by the driver in both cases when there are 0 or 1+n records
				// So there has been an incorrect result size, but not to few results but to many.
				throw new IncorrectResultSizeDataAccessException(1);
			}
		}
	}

	/**
	 * Utility class that orchestrates {@link ReactiveEntityCallbacks}. All the methods provided here check for their
	 * availability and do nothing when an event cannot be published.
	 */
	final class ReactiveNeo4jEvents {

		private final ReactiveEntityCallbacks entityCallbacks;

		ReactiveNeo4jEvents(ReactiveEntityCallbacks entityCallbacks) {
			this.entityCallbacks = entityCallbacks;
		}

		<T> Mono<T> maybeCallBeforeBind(T object) {
			return entityCallbacks.callback(ReactiveBeforeBindCallback.class, object);
		}
	}
}
