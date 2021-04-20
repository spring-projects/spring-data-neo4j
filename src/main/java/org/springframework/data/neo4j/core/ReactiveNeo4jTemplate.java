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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.summary.SummaryCounters;
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
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
import org.springframework.data.neo4j.core.mapping.callback.ReactiveEventSupport;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.NonNull;
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
public final class ReactiveNeo4jTemplate implements ReactiveNeo4jOperations, ReactiveFluentNeo4jOperations, BeanClassLoaderAware, BeanFactoryAware {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(ReactiveNeo4jTemplate.class));

	private static final String OPTIMISTIC_LOCKING_ERROR_MESSAGE = "An entity with the required version does not exist.";

	private static final Renderer renderer = Renderer.getDefaultRenderer();
	private static final String CONTEXT_RELATIONSHIP_HANDLER = "RELATIONSHIP_HANDLER";

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jMappingContext neo4jMappingContext;

	private final CypherGenerator cypherGenerator;

	private ClassLoader beanClassLoader;

	private ReactiveEventSupport eventSupport;

	private ProjectionFactory projectionFactory;

	@Deprecated
	public ReactiveNeo4jTemplate(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
								 ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

		this(neo4jClient, neo4jMappingContext);
		if (databaseSelectionProvider != neo4jClient.getDatabaseSelectionProvider()) {
			throw new IllegalStateException("The provided database selection provider differs from the ReactiveNeo4jClient's one.");
		}
	}

	public ReactiveNeo4jTemplate(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
		this.eventSupport = ReactiveEventSupport.useExistingCallbacks(neo4jMappingContext, ReactiveEntityCallbacks.create());
	}

	@Override
	public Mono<Long> count(Class<?> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData).returning(Functions.count(asterisk())).build();

		return count(statement);
	}

	@Override
	public Mono<Long> count(Statement statement) {
		return count(statement, Collections.emptyMap());
	}

	@Override
	public Mono<Long> count(Statement statement, Map<String, Object> parameters) {
		return count(renderer.render(statement), TemplateSupport.mergeParameters(statement, parameters));
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

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		return createExecutableQuery(domainType, QueryFragmentsAndParameters.forFindAll(entityMetaData))
				.flatMapMany(ExecutableQuery::getResults);
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
	public <T> ExecutableFind<T> find(Class<T> domainType) {
		return new ReactiveFluentFindOperationSupport(this).find(domainType);
	}

	<T, R> Flux<R> doFind(String cypherQuery, Map<String, Object> parameters, Class<T> domainType, Class<R> resultType, TemplateSupport.FetchType fetchType) {

		Flux<T> intermediaResults = Flux.empty();
		if (cypherQuery == null && fetchType == TemplateSupport.FetchType.ALL) {
			intermediaResults = findAll(domainType);
		} else {
			Mono<ExecutableQuery<T>> executableQuery = createExecutableQuery(domainType, cypherQuery,
					parameters == null ? Collections.emptyMap() : parameters);

			switch (fetchType) {
				case ALL:
					intermediaResults = executableQuery.flatMapMany(ExecutableQuery::getResults);
					break;
				case ONE:
					intermediaResults = executableQuery.flatMap(ExecutableQuery::getSingleResult).flux();
					break;
			}
		}

		if (resultType.isAssignableFrom(domainType)) {
			return (Flux<R>) intermediaResults;
		}

		return intermediaResults.map(instance -> projectionFactory.createProjection(resultType, instance));
	}

	@Override
	public <T> Mono<T> findById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);

		return createExecutableQuery(domainType,
				QueryFragmentsAndParameters.forFindById(entityMetaData,
						convertIdValues(entityMetaData.getRequiredIdProperty(), id)))
				.flatMap(ExecutableQuery::getSingleResult);
	}

	@Override
	public <T> Flux<T> findAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);

		return createExecutableQuery(domainType,
						QueryFragmentsAndParameters.forFindByAllId(entityMetaData,
						convertIdValues(entityMetaData.getRequiredIdProperty(), ids)))
				.flatMapMany(ExecutableQuery::getResults);
	}

	@Override
	public <T> Mono<ExecutableQuery<T>> toExecutableQuery(Class<T> domainType,
														  QueryFragmentsAndParameters queryFragmentsAndParameters) {

		return createExecutableQuery(domainType, queryFragmentsAndParameters);
	}


	private Object convertIdValues(@Nullable Neo4jPersistentProperty idProperty, Object idValues) {

		return neo4jMappingContext.getConversionService().writeValue(idValues,
				ClassTypeInformation.from(idValues.getClass()), idProperty == null ? null : idProperty.getOptionalWritingConverter());
	}

	@Override
	public <T> Mono<T> save(T instance) {

		return saveImpl(instance, null);
	}

	@Override
	public <T, R> Mono<R> saveAs(T instance, Class<R> resultType) {

		Assert.notNull(resultType, "ResultType must not be null!");

		if (instance == null) {
			return null;
		}

		if (resultType.isInstance(instance)) {
			return (Mono<R>) save(instance);
		}

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(resultType);
		Mono<T> savingPublisher = saveImpl(instance, projectionInformation.getInputProperties());
		if (projectionInformation.isClosed()) {
			return savingPublisher.map(savedInstance -> projectionFactory.createProjection(resultType, savedInstance));
		}

		return savingPublisher.flatMap(savedInstance -> {

			Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(savedInstance.getClass());
			Neo4jPersistentProperty idProperty = entityMetaData.getIdProperty();
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
			return this.findById(propertyAccessor.getProperty(idProperty), savedInstance.getClass())
					.map(loadedValue -> projectionFactory.createProjection(resultType, loadedValue));
		});
	}

	private <T> Mono<T> saveImpl(T instance, @Nullable List<PropertyDescriptor> includedProperties) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(instance.getClass());
		boolean isNewEntity = entityMetaData.isNew(instance);
		return Mono.just(instance).flatMap(eventSupport::maybeCallBeforeBind)
				.flatMap(entityToBeSaved -> determineDynamicLabels(entityToBeSaved, entityMetaData)).flatMap(t -> {
					T entityToBeSaved = t.getT1();

					DynamicLabels dynamicLabels = t.getT2();

					Function<T, Map<String, Object>> binderFunction = neo4jMappingContext
							.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass());

					Predicate<String> includeProperty;
					if (includedProperties == null) {
						includeProperty  = p -> true;
					} else {
						Set<String> includedPropertyNames = includedProperties.stream().map(PropertyDescriptor::getName).collect(Collectors.toSet());
						includeProperty = includedPropertyNames::contains;
						binderFunction = binderFunction.andThen(tree -> {
							Map<String, Object> properties = (Map<String, Object>) tree.get(Constants.NAME_OF_PROPERTIES_PARAM);
							properties.entrySet().removeIf(e -> !includeProperty.test(e.getKey()));
							return tree;
						});
					}

					Mono<Long> idMono = this.neo4jClient.query(() -> renderer.render(cypherGenerator.prepareSaveOf(entityMetaData, dynamicLabels)))
							.bind(entityToBeSaved).with(binderFunction)
							.fetchAs(Long.class).one()
							.switchIfEmpty(Mono.defer(() -> {
								if (entityMetaData.hasVersionProperty()) {
									return Mono.error(() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE));
								}
								return Mono.empty();
							}));

					PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
					return idMono.doOnNext(internalId -> {
						if (entityMetaData.isUsingInternalIds()) {
							propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), internalId);
						}
					}).then(Mono.defer(() -> processRelations(entityMetaData, instance, propertyAccessor, isNewEntity, includeProperty)));
				});
	}

	private <T> Mono<Tuple2<T, DynamicLabels>> determineDynamicLabels(T entityToBeSaved,
			Neo4jPersistentEntity<?> entityMetaData) {
		return entityMetaData.getDynamicLabelsProperty().map(p -> {

			PersistentPropertyAccessor propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
			ReactiveNeo4jClient.RunnableSpecTightToDatabase runnableQuery = neo4jClient
					.query(() -> renderer.render(cypherGenerator.createStatementReturningDynamicLabels(entityMetaData)))
					.bind(propertyAccessor.getProperty(entityMetaData.getRequiredIdProperty()))
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
		return saveAllImpl(instances, null);
	}

	@Override
	public <T, R> Flux<R> saveAllAs(Iterable<T> instances, Class<R> resultType) {

		Assert.notNull(resultType, "ResultType must not be null!");

		Class<?> commonElementType = TemplateSupport.findCommonElementType(instances);

		if (resultType.isAssignableFrom(commonElementType)) {
			return (Flux<R>) saveAll(instances);
		}

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(resultType);
		Flux<T> savedInstances = saveAllImpl(instances, projectionInformation.getInputProperties());
		if (projectionInformation.isClosed()) {
			return savedInstances.map(instance -> projectionFactory.createProjection(resultType, instance));
		}

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(commonElementType);
		Neo4jPersistentProperty idProperty = entityMetaData.getIdProperty();

		return savedInstances.flatMap(savedInstance -> {
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
			return findById(propertyAccessor.getProperty(idProperty), commonElementType);
		}).map(instance -> projectionFactory.createProjection(resultType, instance));
	}

	private <T> Flux<T> saveAllImpl(Iterable<T> instances, @Nullable List<PropertyDescriptor> includedProperties) {

		List<T> entities;
		if (instances instanceof Collection) {
			entities = new ArrayList<>((Collection<T>) instances);
		} else {
			entities = new ArrayList<>();
			instances.forEach(entities::add);
		}

		if (entities.isEmpty()) {
			return Flux.empty();
		}

		Class<T> domainClass = (Class<T>) CollectionUtils.findCommonElementType(entities);
		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainClass);

		if (entityMetaData.isUsingInternalIds() || entityMetaData.hasVersionProperty()
				|| entityMetaData.getDynamicLabelsProperty().isPresent()) {
			log.debug("Saving entities using single statements.");

			return Flux.fromIterable(entities).flatMap(e -> this.saveImpl(e, includedProperties));
		}

		Function<T, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(domainClass);
		return Flux.fromIterable(entities)
				// Map all entities into a tuple <Original, OriginalWasNew>
				.map(e -> Tuples.of(e, entityMetaData.isNew(e)))
				// Map that tuple into a tuple <<Original, OriginalWasNew>, PotentiallyModified>
				.zipWith(Flux.fromIterable(entities).flatMapSequential(eventSupport::maybeCallBeforeBind))
				// And for my own sanity, back into a flat Tuple3
				.map(nested -> Tuples.of(nested.getT1().getT1(), nested.getT1().getT2(), nested.getT2()))
				.collectList()
				.flatMapMany(entitiesToBeSaved -> Mono.defer(() -> {
					// Defer the actual save statement until the previous flux completes
					List<Map<String, Object>> boundedEntityList = entitiesToBeSaved.stream()
							.map(t -> t.getT3()) // extract PotentiallyModified
							.map(binderFunction).collect(Collectors.toList());
					return neo4jClient
							.query(() -> renderer.render(cypherGenerator.prepareSaveOfMultipleInstancesOf(entityMetaData)))
							.bind(boundedEntityList).to(Constants.NAME_OF_ENTITY_LIST_PARAM).run();
				}).doOnNext(resultSummary -> {
					SummaryCounters counters = resultSummary.counters();
					log.debug(() -> String.format(
							"Created %d and deleted %d nodes, created %d and deleted %d relationships and set %d properties.",
							counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
							counters.relationshipsDeleted(), counters.propertiesSet()));
				}).thenMany(Flux.fromIterable(entitiesToBeSaved)
						.flatMap(t -> processRelations(entityMetaData, t.getT1(), entityMetaData.getPropertyAccessor(t.getT3()), t.getT2(), TemplateSupport.computeIncludePropertyPredicate(includedProperties)))
				));
	}

	@Override
	public <T> Mono<Void> deleteAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		String nameOfParameter = "ids";
		Condition condition = entityMetaData.getIdExpression().in(parameter(nameOfParameter));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		return Mono.defer(() -> this.neo4jClient.query(() -> renderer.render(statement))
				.bind(convertIdValues(entityMetaData.getRequiredIdProperty(), ids))
				.to(nameOfParameter).run().then());
	}

	@Override
	public <T> Mono<Void> deleteById(Object id, Class<T> domainType) {

		Assert.notNull(id, "The given id must not be null!");

		String nameOfParameter = "id";
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		return Mono.defer(() -> this.neo4jClient.query(() -> renderer.render(statement))
				.bind(convertIdValues(entityMetaData.getRequiredIdProperty(), id))
				.to(nameOfParameter).run().then());
	}

	@Override
	public <T> Mono<Void> deleteByIdWithVersion(Object id, Class<T> domainType, Neo4jPersistentProperty versionProperty,
										  Object versionValue) {

		String nameOfParameter = "id";
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter))
				.and(Cypher.property(Constants.NAME_OF_ROOT_NODE, versionProperty.getPropertyName())
						.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))
						.or(Cypher.property(Constants.NAME_OF_ROOT_NODE, versionProperty.getPropertyName()).isNull()));

		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData, condition)
				.returning(Constants.NAME_OF_ROOT_NODE).build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(nameOfParameter, convertIdValues(entityMetaData.getRequiredIdProperty(), id));
		parameters.put(Constants.NAME_OF_VERSION_PARAM, versionValue);

		return Mono.defer(() -> this.neo4jClient.query(() -> renderer.render(statement))
				.bindAll(parameters)
				.fetch().one().switchIfEmpty(Mono.defer(() -> {
					if (entityMetaData.hasVersionProperty()) {
						return Mono.error(() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE));
					}
					return Mono.empty();
				})))
		.then(deleteById(id, domainType));
	}

	@Override
	public Mono<Void> deleteAll(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData);
		return Mono.defer(() -> this.neo4jClient.query(() -> renderer.render(statement)).run().then());
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, Statement statement) {
		return createExecutableQuery(domainType, statement, Collections.emptyMap());
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, String cypherQuery) {
		return createExecutableQuery(domainType, cypherQuery, Collections.emptyMap());
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, Statement statement,
			Map<String, Object> parameters) {

		return createExecutableQuery(domainType, renderer.render(statement), TemplateSupport.mergeParameters(statement, parameters));
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, String cypherQuery,
			Map<String, Object> parameters) {

		Assert.notNull(neo4jMappingContext.getPersistentEntity(domainType), "Cannot get or create persistent entity.");
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType).withCypherQuery(cypherQuery)
				.withParameters(parameters)
				.usingMappingFunction(this.neo4jMappingContext.getRequiredMappingFunctionFor(domainType)).build();
		return this.toExecutableQuery(preparedQuery);
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType,
		   QueryFragmentsAndParameters queryFragmentsAndParameters) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		QueryFragmentsAndParameters.QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();

		boolean containsPossibleCircles = entityMetaData != null && entityMetaData.containsPossibleCircles(queryFragments::includeField);
		if (containsPossibleCircles && !queryFragments.isScalarValueReturn()) {
			return createQueryAndParameters(entityMetaData, queryFragments, queryFragmentsAndParameters.getParameters())
					.flatMap(finalQueryAndParameters ->
							createExecutableQuery(domainType, renderer.render(queryFragments.generateGenericStatement()),
									finalQueryAndParameters.getParameters()));
		}

		return createExecutableQuery(domainType, queryFragments.toStatement(), queryFragmentsAndParameters.getParameters());
	}

	private Mono<GenericQueryAndParameters> createQueryAndParameters(Neo4jPersistentEntity<?> entityMetaData,
		 	QueryFragmentsAndParameters.QueryFragments queryFragments, Map<String, Object> parameters) {

			return Mono.deferContextual(ctx -> {
				Set<Long> rootNodeIds = ctx.get("rootNodes");
				Set<Long> processedRelationshipIds = ctx.get("processedRelationships");
				Set<Long> processedNodeIds = ctx.get("processedNodes");
				return Flux.fromIterable(entityMetaData.getRelationshipsInHierarchy(fieldName -> queryFragments.includeField(fieldName)))
						.flatMap(relationshipDescription -> {

							Statement statement = cypherGenerator.prepareMatchOf(entityMetaData, relationshipDescription,
									queryFragments.getMatchOn(), queryFragments.getCondition())
									.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

							Map<String, Object> usedParameters = new HashMap<>(parameters);
							usedParameters.putAll(statement.getParameters());
							return neo4jClient.query(renderer.render(statement))
									.bindAll(usedParameters)
									.fetch()
									.one()
									.map(record -> {
										Collection<Long> rootIds = (List<Long>) record.get(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE);
										Collection<Long> newRelationshipIds = (List<Long>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATIONS);
										Collection<Long> newRelatedNodeIds = (List<Long>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES);
										rootNodeIds.addAll(rootIds);

										return Tuples.of(newRelationshipIds, newRelatedNodeIds);
									})
									.expand(iterateAndMapNextLevel(relationshipDescription));
						})
						.collect(GenericQueryAndParameters::new, (genericQueryAndParameters, _not_used2) ->
								genericQueryAndParameters.with(rootNodeIds, processedRelationshipIds, processedNodeIds)
						);
			})
			.contextWrite(ctx -> {
				return ctx
						.put("rootNodes", ConcurrentHashMap.newKeySet())
						.put("processedNodes", ConcurrentHashMap.newKeySet())
						.put("processedRelationships", ConcurrentHashMap.newKeySet());
			});

	}

	private Flux<Tuple2<Collection<Long>, Collection<Long>>> iterateNextLevel(Collection<Long> relatedNodeIds,
				  						RelationshipDescription relationshipDescription) {

		NodeDescription<?> target = relationshipDescription.getTarget();

		return Flux.fromIterable(target.getRelationshipsInHierarchy(s -> true))
			.flatMap(relDe -> {
				Node node = anyNode(Constants.NAME_OF_ROOT_NODE);

				Statement statement = cypherGenerator
						.prepareMatchOf(target, relDe, null,
								Functions.id(node).in(Cypher.parameter(Constants.NAME_OF_ID)))
						.returning(cypherGenerator.createGenericReturnStatement()).build();

				return neo4jClient.query(renderer.render(statement))
						.bindAll(Collections.singletonMap(Constants.NAME_OF_ID, relatedNodeIds))

						.fetch()
						.one()
						.map(record -> {
							Collection<Long> newRelationshipIds = (List<Long>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATIONS);
							Collection<Long> newRelatedNodeIds = (List<Long>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES);

							return Tuples.of(newRelationshipIds, newRelatedNodeIds);
						})
						.expand(object -> iterateAndMapNextLevel(relDe).apply(object));
			});

	}

	@NonNull
	private Function<Tuple2<Collection<Long>, Collection<Long>>,
			Publisher<Tuple2<Collection<Long>, Collection<Long>>>> iterateAndMapNextLevel(
					RelationshipDescription relationshipDescription) {

		return newRelationshipAndRelatedNodeIds -> {
			return Flux.deferContextual(ctx -> {
				Set<Long> relationshipIds = ctx.get("processedRelationships");
				Set<Long> processedNodeIds = ctx.get("processedNodes");

				Collection<Long> newRelationshipIds = newRelationshipAndRelatedNodeIds.getT1();
				Set<Long> tmpProcessedRels = ConcurrentHashMap.newKeySet(newRelationshipIds.size());
				tmpProcessedRels.addAll(newRelationshipIds);
				tmpProcessedRels.removeAll(relationshipIds);
				relationshipIds.addAll(newRelationshipIds);

				Collection<Long> newRelatedNodeIds = newRelationshipAndRelatedNodeIds.getT2();
				Set<Long> tmpProcessedNodes = ConcurrentHashMap.newKeySet(newRelatedNodeIds.size());
				tmpProcessedNodes.addAll(newRelatedNodeIds);
				tmpProcessedNodes.removeAll(processedNodeIds);
				processedNodeIds.addAll(newRelatedNodeIds);

				if (tmpProcessedRels.isEmpty() && tmpProcessedNodes.isEmpty()) {
					return Mono.empty();
				}

				return iterateNextLevel(newRelatedNodeIds, relationshipDescription);
			});
		};
	}

	/**
	 * Starts of processing of the relationships.
	 *
	 * @param neo4jPersistentEntity  The description of the instance to save
	 * @param originalInstance       The original parent instance. It is paramount to pass in the original instance (prior
	 *                               to generating the id and prior to eventually create new instances via the property accessor,
	 *                               so that we can reliable stop traversing relationships.
	 * @param parentPropertyAccessor The property accessor of the parent, to modify the relationships
	 * @param isParentObjectNew      A flag if the parent was new
	 * @param includeProperty        A predicate telling to include a relationship property or not
	 * @param <T>                    The type of the entity to save
	 * @return A mono representing the whole stream of save operations.
	 */
	private <T> Mono<T> processRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, T originalInstance,
			PersistentPropertyAccessor<?> parentPropertyAccessor,
			boolean isParentObjectNew, Predicate<String> includeProperty) {

		return processNestedRelations(neo4jPersistentEntity, parentPropertyAccessor, isParentObjectNew,
				new NestedRelationshipProcessingStateMachine(originalInstance), includeProperty);
	}

	private <T> Mono<T> processNestedRelations(Neo4jPersistentEntity<?> sourceEntity, PersistentPropertyAccessor<?> parentPropertyAccessor,
		  boolean isParentObjectNew, NestedRelationshipProcessingStateMachine stateMachine, Predicate<String> includeProperty) {

		Object fromId = parentPropertyAccessor.getProperty(sourceEntity.getRequiredIdProperty());
		List<Mono<Void>> relationshipDeleteMonos = new ArrayList<>();
		List<Flux<RelationshipHandler>> relationshipCreationCreations = new ArrayList<>();

		sourceEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) association -> {

			// create context to bundle parameters
			NestedRelationshipContext relationshipContext = NestedRelationshipContext.of(association, parentPropertyAccessor,
					sourceEntity);

			Object rawValue = relationshipContext.getValue();
			Collection<?> relatedValuesToStore = MappingSupport.unifyRelationshipValue(relationshipContext.getInverse(),
					rawValue);

			RelationshipDescription relationshipDescription = relationshipContext.getRelationship();
			RelationshipDescription relationshipDescriptionObverse = relationshipDescription.getRelationshipObverse();

			if (!includeProperty.test(relationshipDescription.getFieldName())) {
				return;
			}
			Neo4jPersistentProperty idProperty;
			if (!relationshipDescription.hasInternalIdProperty()) {
				idProperty = null;
			} else {
				Neo4jPersistentEntity<?> relationshipPropertiesEntity = (Neo4jPersistentEntity<?>) relationshipDescription.getRelationshipPropertiesEntity();
				idProperty = relationshipPropertiesEntity.getIdProperty();
			}

				// break recursive procession and deletion of previously created relationships
				ProcessState processState = stateMachine.getStateOf(fromId, relationshipDescriptionObverse, relatedValuesToStore);
				if (processState == ProcessState.PROCESSED_ALL_RELATIONSHIPS || processState == ProcessState.PROCESSED_BOTH) {
					return;
				}

				// Remove all relationships before creating all new if the entity is not new and the relationship
				// has not been processed before.
				// This avoids the usage of cache but might have significant impact on overall performance
				if (!isParentObjectNew && !stateMachine.hasProcessedRelationship(fromId, relationshipDescription)) {

				List<Long> knownRelationshipsIds = new ArrayList<>();
				if (idProperty != null) {
					for (Object relatedValueToStore : relatedValuesToStore) {
						if (relatedValueToStore == null) {
							continue;
						}

						Long id = (Long) relationshipContext
								.getRelationshipPropertiesPropertyAccessor(relatedValueToStore)
								.getProperty(idProperty);
						if (id != null) {
							knownRelationshipsIds.add(id);
						}
					}
				}

				Statement relationshipRemoveQuery = cypherGenerator.prepareDeleteOf(sourceEntity, relationshipDescription);

				relationshipDeleteMonos.add(
						neo4jClient.query(renderer.render(relationshipRemoveQuery))
								.bind(convertIdValues(sourceEntity.getIdProperty(), fromId)) //
									.to(Constants.FROM_ID_PARAMETER_NAME) //
								.bind(knownRelationshipsIds) //
									.to(Constants.NAME_OF_KNOWN_RELATIONSHIPS_PARAM) //
								.run().checkpoint("delete relationships").then());
			}

			// nothing to do because there is nothing to map
			if (relationshipContext.inverseValueIsEmpty()) {
				return;
			}
			Neo4jPersistentProperty relationshipProperty = association.getInverse();

			stateMachine.markRelationshipAsProcessed(fromId, relationshipDescription);
			Flux<RelationshipHandler> relationshipCreation = Flux.fromIterable(relatedValuesToStore).concatMap(relatedValueToStore -> {

				Object relatedNodePreEvt = relationshipContext.identifyAndExtractRelationshipTargetNode(relatedValueToStore);
				return Mono.deferContextual(ctx -> eventSupport
						.maybeCallBeforeBind(relatedNodePreEvt)
						.flatMap(newRelatedObject -> {
							Neo4jPersistentEntity<?> targetEntity = neo4jMappingContext.getPersistentEntity(relatedNodePreEvt.getClass());

							Mono<Long> queryOrSave;
							if (stateMachine.hasProcessedValue(relatedValueToStore)) {
								Object newRelatedObjectForQuery = stateMachine.getProcessedAs(newRelatedObject);
								queryOrSave = queryRelatedNode(newRelatedObjectForQuery, targetEntity);
							} else {
								queryOrSave = saveRelatedNode(newRelatedObject, targetEntity);
							}
							stateMachine.markValueAsProcessed(relatedValueToStore);
							return queryOrSave.flatMap(relatedInternalId -> {
									// if an internal id is used this must be set to link this entity in the next iteration
									PersistentPropertyAccessor<?> targetPropertyAccessor = targetEntity.getPropertyAccessor(newRelatedObject);
									if (targetEntity.isUsingInternalIds()) {
										targetPropertyAccessor.setProperty(targetEntity.getRequiredIdProperty(), relatedInternalId);
										stateMachine.markValueAsProcessedAs(newRelatedObject, targetPropertyAccessor.getBean());
									}

									Object idValue = idProperty != null
											? relationshipContext
											.getRelationshipPropertiesPropertyAccessor(relatedValueToStore).getProperty(idProperty)
											: null;

									boolean isNewRelationship = idValue == null;
									CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatement(
											sourceEntity, relationshipContext, relatedValueToStore, isNewRelationship);

									// in case of no properties the bind will just return an empty map
									return neo4jClient
											.query(renderer.render(statementHolder.getStatement()))
											.bind(convertIdValues(sourceEntity.getRequiredIdProperty(), fromId)) //
												.to(Constants.FROM_ID_PARAMETER_NAME) //
											.bind(relatedInternalId) //
												.to(Constants.TO_ID_PARAMETER_NAME) //
											.bind(idValue) //
													.to(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM) //
											.bindAll(statementHolder.getProperties())
											.fetchAs(Long.class).one()
											.flatMap(relationshipInternalId -> {
												if (idProperty != null && isNewRelationship) {
													relationshipContext
															.getRelationshipPropertiesPropertyAccessor(relatedValueToStore)
															.setProperty(idProperty, relationshipInternalId);
												}

												Mono<Object> nestedRelationshipsSignal = null;
												if (processState != ProcessState.PROCESSED_ALL_VALUES) {
													nestedRelationshipsSignal = processNestedRelations(targetEntity, targetPropertyAccessor, targetEntity.isNew(newRelatedObject), stateMachine, s -> true);
												}

												Mono<Object> getRelationshipOrRelationshipPropertiesObject = Mono.fromSupplier(() -> MappingSupport.getRelationshipOrRelationshipPropertiesObject(
																neo4jMappingContext,
																relationshipDescription.hasRelationshipProperties(),
																relationshipProperty.isDynamicAssociation(),
																relatedValueToStore,
																targetPropertyAccessor));
												return nestedRelationshipsSignal == null ? getRelationshipOrRelationshipPropertiesObject :
														nestedRelationshipsSignal.then(getRelationshipOrRelationshipPropertiesObject);
											});
								})
								.doOnNext(potentiallyRecreatedRelatedObject -> {
									RelationshipHandler handler = ctx.get(CONTEXT_RELATIONSHIP_HANDLER);
									handler.handle(relatedValueToStore, relatedNodePreEvt, potentiallyRecreatedRelatedObject);
								});
						})
						.then(Mono.fromSupplier(() -> ctx.<RelationshipHandler>get(CONTEXT_RELATIONSHIP_HANDLER))));

			})
			.contextWrite(ctx -> {
				RelationshipHandler relationshipHandler = RelationshipHandler.forProperty(relationshipProperty, rawValue);
				return ctx.put(CONTEXT_RELATIONSHIP_HANDLER, relationshipHandler);
			});
			relationshipCreationCreations.add(relationshipCreation);
		});

		return (Mono<T>) Flux.concat(relationshipDeleteMonos)
				.thenMany(Flux.concat(relationshipCreationCreations))
				.doOnNext(objects -> objects.applyFinalResultToOwner(parentPropertyAccessor))
				.checkpoint()
				.then(Mono.fromSupplier(parentPropertyAccessor::getBean));

	}

	private <Y> Mono<Long> queryRelatedNode(Object entity, Neo4jPersistentEntity<?> targetNodeDescription) {

		Neo4jPersistentProperty requiredIdProperty = targetNodeDescription.getRequiredIdProperty();
		PersistentPropertyAccessor<Object> targetPropertyAccessor = targetNodeDescription.getPropertyAccessor(entity);
		Object idValue = targetPropertyAccessor.getProperty(requiredIdProperty);

		return neo4jClient.query(() ->
				renderer.render(cypherGenerator.prepareMatchOf(targetNodeDescription,
						targetNodeDescription.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID)))
						.returning(Constants.NAME_OF_INTERNAL_ID)
						.build())
		)
				.bindAll(Collections.singletonMap(Constants.NAME_OF_ID, idValue))
				.fetchAs(Long.class).one();
	}

	private <Y> Mono<Long> saveRelatedNode(Object relatedNode, Neo4jPersistentEntity<?> targetNodeDescription) {

		return determineDynamicLabels((Y) relatedNode, targetNodeDescription)
				.flatMap(t -> {
					Y entity = t.getT1();
					Class<Y> entityType = (Class<Y>) ((Neo4jPersistentEntity<?>) targetNodeDescription).getType();
					DynamicLabels dynamicLabels = t.getT2();

					return neo4jClient
							.query(() -> renderer.render(cypherGenerator.prepareSaveOf(targetNodeDescription, dynamicLabels)))
							.bind((Y) entity).with(neo4jMappingContext.getRequiredBinderFunctionFor(entityType))
							.fetchAs(Long.class).one();
				}).switchIfEmpty(Mono.defer(() -> {
					if (targetNodeDescription.hasVersionProperty()) {
						return Mono.error(() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE));
					}
					return Mono.empty();
				}));
	}

	@Override
	public <T> Mono<ExecutableQuery<T>> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		return Mono.defer(() -> {
			Class<T> resultType = preparedQuery.getResultType();
			QueryFragmentsAndParameters queryFragmentsAndParameters = preparedQuery.getQueryFragmentsAndParameters();
			String cypherQuery = queryFragmentsAndParameters.getCypherQuery();
			Map<String, Object> finalParameters = queryFragmentsAndParameters.getParameters();

			QueryFragmentsAndParameters.QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();
			Neo4jPersistentEntity<?> entityMetaData = (Neo4jPersistentEntity<?>) queryFragmentsAndParameters.getNodeDescription();

			boolean containsPossibleCircles = entityMetaData != null && entityMetaData.containsPossibleCircles(queryFragments::includeField);
			if (cypherQuery == null || containsPossibleCircles) {

				if (containsPossibleCircles && !queryFragments.isScalarValueReturn()) {
					return createQueryAndParameters(entityMetaData, queryFragments, finalParameters)
							.map(genericQueryAndParameters -> {
								ReactiveNeo4jClient.MappingSpec<T> mappingSpec = this.neo4jClient.query(renderer.render(queryFragments.generateGenericStatement()))
										.bindAll(genericQueryAndParameters.getParameters()).fetchAs(resultType);

								ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec = preparedQuery.getOptionalMappingFunction()
										.map(mappingFunction -> mappingSpec.mappedBy(mappingFunction)).orElse(mappingSpec);

								return new DefaultReactiveExecutableQuery<>(preparedQuery, fetchSpec);
							});
				}

				Statement statement = queryFragments.toStatement();
				cypherQuery = renderer.render(statement);
				finalParameters = TemplateSupport.mergeParameters(statement, finalParameters);
			}

			ReactiveNeo4jClient.MappingSpec<T> mappingSpec = this.neo4jClient.query(cypherQuery)
					.bindAll(finalParameters).fetchAs(resultType);

			ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec = preparedQuery.getOptionalMappingFunction()
					.map(mappingFunction -> mappingSpec.mappedBy(mappingFunction)).orElse(mappingSpec);

			return Mono.just(new DefaultReactiveExecutableQuery<>(preparedQuery, fetchSpec));
		});
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		this.eventSupport = ReactiveEventSupport.discoverCallbacks(neo4jMappingContext, beanFactory);

		SpelAwareProxyProjectionFactory spelAwareProxyProjectionFactory = new SpelAwareProxyProjectionFactory();
		spelAwareProxyProjectionFactory.setBeanClassLoader(beanClassLoader);
		spelAwareProxyProjectionFactory.setBeanFactory(beanFactory);
		this.projectionFactory = spelAwareProxyProjectionFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = beanClassLoader == null ? org.springframework.util.ClassUtils.getDefaultClassLoader() : beanClassLoader;
	}

	final class DefaultReactiveExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;
		private final ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec;

		DefaultReactiveExecutableQuery(PreparedQuery<T> preparedQuery, ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec) {
			this.preparedQuery = preparedQuery;
			this.fetchSpec = fetchSpec;
		}

		/**
		 * @return All results returned by this query.
		 */
		@SuppressWarnings("unchecked")
		public Flux<T> getResults() {

			return fetchSpec.all().switchOnFirst((signal, f) -> {
				if (signal.hasValue() && preparedQuery.resultsHaveBeenAggregated()) {
					return f.flatMap(nested -> Flux.fromIterable((Collection<T>) nested).distinct()).distinct();
				}
				return f;
			});
		}

		/**
		 * @return A single result
		 * @throws IncorrectResultSizeDataAccessException if there is no or more than one result
		 */
		public Mono<T> getSingleResult() {
			return fetchSpec.one().map(t -> {
				if (t instanceof LinkedHashSet) {
					return (T) ((LinkedHashSet<?>) t).iterator().next();
				}
				return t;
			}).onErrorMap(IndexOutOfBoundsException.class, e -> new IncorrectResultSizeDataAccessException(e.getMessage(), 1));
		}
	}
}
