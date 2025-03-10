/*
 * Copyright 2011-2025 the original author or authors.
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

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.FunctionInvocation;
import org.neo4j.cypherdsl.core.Named;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.neo4j.core.TemplateSupport.FilteredBinderFunction;
import org.springframework.data.neo4j.core.TemplateSupport.NodesAndRelationshipsByIdStatementProvider;
import org.springframework.data.neo4j.core.mapping.AssociationHandlerSupport;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CreateRelationshipStatementHolder;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.DtoInstantiatingConverter;
import org.springframework.data.neo4j.core.mapping.EntityFromDtoInstantiatingConverter;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
import org.springframework.data.neo4j.core.mapping.IdDescription;
import org.springframework.data.neo4j.core.mapping.IdentitySupport;
import org.springframework.data.neo4j.core.mapping.MappingSupport;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.NestedRelationshipContext;
import org.springframework.data.neo4j.core.mapping.NestedRelationshipProcessingStateMachine;
import org.springframework.data.neo4j.core.mapping.NestedRelationshipProcessingStateMachine.ProcessState;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.neo4j.core.mapping.SpringDataCypherDsl;
import org.springframework.data.neo4j.core.mapping.callback.ReactiveEventSupport;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.repository.query.QueryFragments;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.asterisk;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Philipp Tölle
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class ReactiveNeo4jTemplate implements
		ReactiveNeo4jOperations, ReactiveFluentNeo4jOperations,
		BeanClassLoaderAware, BeanFactoryAware {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(ReactiveNeo4jTemplate.class));

	private static final String OPTIMISTIC_LOCKING_ERROR_MESSAGE = "An entity with the required version does not exist.";

	private static final String CONTEXT_RELATIONSHIP_HANDLER = "RELATIONSHIP_HANDLER";

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jMappingContext neo4jMappingContext;

	private final CypherGenerator cypherGenerator;

	private static final TransactionDefinition readOnlyTransactionDefinition = new TransactionDefinition() {
		@Override
		public boolean isReadOnly() {
			return true;
		}
	};

	private TransactionalOperator transactionalOperatorReadOnly;

	private TransactionalOperator transactionalOperator;

	private ClassLoader beanClassLoader;

	private ReactiveEventSupport eventSupport;

	private ProjectionFactory projectionFactory;

	private Renderer renderer;

	private Function<Named, FunctionInvocation> elementIdOrIdFunction;

	public ReactiveNeo4jTemplate(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {
		this(neo4jClient, neo4jMappingContext, null);
	}

	public ReactiveNeo4jTemplate(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext, @Nullable ReactiveTransactionManager transactionManager) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
		this.eventSupport = ReactiveEventSupport.useExistingCallbacks(neo4jMappingContext, ReactiveEntityCallbacks.create());
		this.renderer = Renderer.getDefaultRenderer();
		this.elementIdOrIdFunction = SpringDataCypherDsl.elementIdOrIdFunction.apply(null);
		setTransactionManager(transactionManager);
	}

	ProjectionFactory getProjectionFactory() {
		return Objects.requireNonNull(this.projectionFactory, "Projection support for the Neo4j template is only available when the template is a proper and fully initialized Spring bean.");
	}

	@Override
	public Mono<Long> count(Class<?> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData).returning(Cypher.count(asterisk())).build();

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
		return transactionalOperatorReadOnly.transactional(this.toExecutableQuery(preparedQuery).flatMap(ExecutableQuery::getSingleResult));
	}

	@Override
	public <T> Flux<T> findAll(Class<T> domainType) {

		return transactionalOperatorReadOnly.transactional(doFindAll(domainType, null));
	}

	private <T> Flux<T> doFindAll(Class<T> domainType, @Nullable Class<?> resultType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		return createExecutableQuery(domainType, resultType, QueryFragmentsAndParameters.forFindAll(entityMetaData))
				.flatMapMany(ExecutableQuery::getResults);
	}

	@Override
	public <T> Flux<T> findAll(Statement statement, Class<T> domainType) {

		return transactionalOperatorReadOnly.transactional(createExecutableQuery(domainType, statement).flatMapMany(ExecutableQuery::getResults));
	}

	@Override
	public <T> Flux<T> findAll(Statement statement, Map<String, Object> parameters, Class<T> domainType) {

		return transactionalOperatorReadOnly.transactional(createExecutableQuery(domainType, null, statement, parameters).flatMapMany(ExecutableQuery::getResults));
	}

	@Override
	public <T> Mono<T> findOne(Statement statement, Map<String, Object> parameters, Class<T> domainType) {

		return transactionalOperatorReadOnly.transactional(createExecutableQuery(domainType, null, statement, parameters).flatMap(ExecutableQuery::getSingleResult));
	}

	@Override
	public <T> Flux<T> findAll(String cypherQuery, Class<T> domainType) {
		return transactionalOperatorReadOnly.transactional(createExecutableQuery(domainType, cypherQuery).flatMapMany(ExecutableQuery::getResults));
	}

	@Override
	public <T> Flux<T> findAll(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return transactionalOperatorReadOnly.transactional(createExecutableQuery(domainType, null, cypherQuery, parameters).flatMapMany(ExecutableQuery::getResults));
	}

	@Override
	public <T> Mono<T> findOne(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return transactionalOperatorReadOnly.transactional(createExecutableQuery(domainType, null, cypherQuery, parameters).flatMap(ExecutableQuery::getSingleResult));
	}

	@Override
	public <T> ExecutableFind<T> find(Class<T> domainType) {
		return new ReactiveFluentOperationSupport(this).find(domainType);
	}

	@SuppressWarnings("unchecked")
	<T, R> Flux<R> doFind(@Nullable String cypherQuery, @Nullable Map<String, Object> parameters, Class<T> domainType, Class<R> resultType, TemplateSupport.FetchType fetchType, @Nullable QueryFragmentsAndParameters queryFragmentsAndParameters) {

		Flux<T> intermediaResults;
		if (cypherQuery == null && queryFragmentsAndParameters == null && fetchType == TemplateSupport.FetchType.ALL) {
			intermediaResults = doFindAll(domainType, resultType);
		} else {
			Mono<ExecutableQuery<T>> executableQuery;
			if (queryFragmentsAndParameters == null) {
				executableQuery = createExecutableQuery(domainType, resultType, cypherQuery,
						parameters == null ? Collections.emptyMap() : parameters);
			} else {
				executableQuery = createExecutableQuery(domainType, resultType, queryFragmentsAndParameters);
			}

			intermediaResults = switch (fetchType) {
				case ALL ->	transactionalOperatorReadOnly.transactional(executableQuery.flatMapMany(ExecutableQuery::getResults));
				case ONE -> transactionalOperatorReadOnly.transactional(executableQuery.flatMap(ExecutableQuery::getSingleResult).flux());
			};
		}

		if (resultType.isAssignableFrom(domainType)) {
			return (Flux<R>) intermediaResults;
		}

		if (resultType.isInterface()) {
			return intermediaResults.map(instance -> getProjectionFactory().createProjection(resultType, instance));
		}

		DtoInstantiatingConverter converter = new DtoInstantiatingConverter(resultType, neo4jMappingContext);
		return (Flux<R>) intermediaResults.map(EntityInstanceWithSource.class::cast)
				.mapNotNull(converter::convert);
	}

	@Override
	public <T> Mono<Boolean> existsById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);

		QueryFragmentsAndParameters fragmentsAndParameters = QueryFragmentsAndParameters
				.forExistsById(entityMetaData, convertIdValues(entityMetaData.getRequiredIdProperty(), id));

		Statement statement = fragmentsAndParameters.getQueryFragments().toStatement();
		Map<String, Object> parameters = fragmentsAndParameters.getParameters();

		return count(statement, parameters).map(r -> r > 0);
	}

	@Override
	public <T> Mono<T> findById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);

		return transactionalOperatorReadOnly.transactional(createExecutableQuery(domainType, null,
				QueryFragmentsAndParameters.forFindById(entityMetaData,
						convertIdValues(entityMetaData.getRequiredIdProperty(), id)))
				.flatMap(ExecutableQuery::getSingleResult));
	}

	@Override
	public <T> Flux<T> findAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);

		return transactionalOperatorReadOnly.transactional(createExecutableQuery(domainType, null,
						QueryFragmentsAndParameters.forFindByAllId(entityMetaData,
						convertIdValues(entityMetaData.getRequiredIdProperty(), ids)))
				.flatMapMany(ExecutableQuery::getResults));
	}

	@Override
	public <T> Mono<ExecutableQuery<T>> toExecutableQuery(Class<T> domainType,
														  QueryFragmentsAndParameters queryFragmentsAndParameters) {

		return createExecutableQuery(domainType, null, queryFragmentsAndParameters);
	}

	private Object convertIdValues(@Nullable Neo4jPersistentProperty idProperty, @Nullable Object idValues) {

		if (idProperty != null && ((Neo4jPersistentEntity<?>) idProperty.getOwner()).isUsingInternalIds()) {
			return idValues;
		}

		if (idValues != null) {
			return neo4jMappingContext.getConversionService().writeValue(idValues, TypeInformation.of(idValues.getClass()), idProperty == null ? null : idProperty.getOptionalConverter());
		} else if (idProperty != null) {
			return neo4jMappingContext.getConversionService().writeValue(idValues, idProperty.getTypeInformation(), idProperty.getOptionalConverter());
		} else {
			// Not much we can convert here
			return Values.NULL;
		}
	}

	@Override
	public <T> Mono<T> save(T instance) {

		return transactionalOperator.transactional(saveImpl(instance, Collections.emptySet(), null));
	}

	@Override
	public <T> Mono<T> saveAs(T instance, BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		if (instance == null) {
			return null;
		}

		return transactionalOperator.transactional(saveImpl(instance, TemplateSupport.computeIncludedPropertiesFromPredicate(this.neo4jMappingContext, instance.getClass(), includeProperty), null));
	}

	@Override
	public <T, R> Mono<R> saveAs(T instance, Class<R> resultType) {

		Assert.notNull(resultType, "ResultType must not be null");

		if (instance == null) {
			return null;
		}

		if (resultType.equals(instance.getClass())) {
			return save(instance).map(resultType::cast);
		}

		ProjectionFactory localProjectionFactory = getProjectionFactory();
		ProjectionInformation projectionInformation = localProjectionFactory.getProjectionInformation(resultType);
		Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(instance.getClass(), resultType,
				localProjectionFactory, neo4jMappingContext);

		Mono<T> savingPublisher = transactionalOperator.transactional(saveImpl(instance, pps, null));

		if (!resultType.isInterface()) {
			return savingPublisher.map(savedInstance -> {
				@SuppressWarnings("unchecked")
				R result = (R) (new DtoInstantiatingConverter(resultType, neo4jMappingContext).convertDirectly(savedInstance));
				return result;
			});
		}
		if (projectionInformation.isClosed()) {
			return savingPublisher.map(savedInstance -> localProjectionFactory.createProjection(resultType, savedInstance));
		}

		return savingPublisher.flatMap(savedInstance -> {

			Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(savedInstance.getClass());
			Neo4jPersistentProperty idProperty = entityMetaData.getIdProperty();
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
			return transactionalOperatorReadOnly.transactional(this.findById(propertyAccessor.getProperty(idProperty), savedInstance.getClass())
					.map(loadedValue -> localProjectionFactory.createProjection(resultType, loadedValue)));
		});
	}

	<T, R> Flux<R> doSave(Iterable<R> instances, Class<T> domainType) {
		// empty check
		if (!instances.iterator().hasNext()) {
			return Flux.empty();
		}

		Class<?> resultType = TemplateSupport.findCommonElementType(instances);

		Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(domainType, resultType,
				getProjectionFactory(), neo4jMappingContext);

		NestedRelationshipProcessingStateMachine stateMachine = new NestedRelationshipProcessingStateMachine(neo4jMappingContext);
		Collection<Object> knownRelationshipsIds = new HashSet<>();
		EntityFromDtoInstantiatingConverter<T> converter = new EntityFromDtoInstantiatingConverter<>(domainType, neo4jMappingContext);
		return Flux.fromIterable(instances)
				.concatMap(instance -> {
					T domainObject = converter.convert(instance);

					@SuppressWarnings("unchecked")
					Mono<R> result = transactionalOperator.transactional(saveImpl(domainObject, pps, stateMachine, knownRelationshipsIds)
							.map(savedEntity -> (R) new DtoInstantiatingConverter(resultType, neo4jMappingContext).convertDirectly(savedEntity)));
					return result;
				});
	}


	private <T> Mono<T> saveImpl(T instance, @Nullable Collection<PropertyFilter.ProjectedPath> includedProperties, @Nullable NestedRelationshipProcessingStateMachine stateMachine) {
		return saveImpl(instance, includedProperties, stateMachine, new HashSet<>());
	}

	private <T> Mono<T> saveImpl(T instance, @Nullable Collection<PropertyFilter.ProjectedPath> includedProperties, @Nullable NestedRelationshipProcessingStateMachine stateMachine, Collection<Object> knownRelationshipsIds) {

		if (stateMachine != null && stateMachine.hasProcessedValue(instance)) {
			return Mono.just(instance);
		}

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(instance.getClass());
		boolean isNewEntity = entityMetaData.isNew(instance);

		NestedRelationshipProcessingStateMachine finalStateMachine;
		if (stateMachine == null) {
			finalStateMachine = new NestedRelationshipProcessingStateMachine(neo4jMappingContext);
		} else {
			finalStateMachine = stateMachine;
		}

		return Mono.just(instance).flatMap(eventSupport::maybeCallBeforeBind)
				.flatMap(entityToBeSaved -> determineDynamicLabels(entityToBeSaved, entityMetaData)).flatMap(t -> {
					T entityToBeSaved = t.getT1();

					DynamicLabels dynamicLabels = t.getT2();

					@SuppressWarnings("unchecked")
					FilteredBinderFunction<T> binderFunction = TemplateSupport.createAndApplyPropertyFilter(
							includedProperties, entityMetaData,
							neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass()));

					boolean canUseElementId = TemplateSupport.rendererRendersElementId(renderer);
					Mono<Entity> idMono = this.neo4jClient.query(() -> renderer.render(cypherGenerator.prepareSaveOf(entityMetaData, dynamicLabels, canUseElementId)))
							.bind(entityToBeSaved)
							.with(binderFunction)
							.fetchAs(Entity.class)
							.one()
							.switchIfEmpty(Mono.defer(() -> {
								if (entityMetaData.hasVersionProperty()) {
									return Mono.error(() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE));
								}
								return Mono.empty();
							}));

					PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
					return idMono.doOnNext(newOrUpdatedNode -> {
						var elementId = !entityMetaData.isUsingDeprecatedInternalId() && canUseElementId
										? IdentitySupport.getElementId(newOrUpdatedNode)
										: newOrUpdatedNode.id();
						TemplateSupport.setGeneratedIdIfNecessary(entityMetaData, propertyAccessor, elementId, Optional.of(newOrUpdatedNode));
						TemplateSupport.updateVersionPropertyIfPossible(entityMetaData, propertyAccessor, newOrUpdatedNode);
						finalStateMachine.markEntityAsProcessed(instance, elementId);
					}).map(IdentitySupport::getElementId)
							.flatMap(internalId -> processRelations(entityMetaData,  propertyAccessor, isNewEntity, finalStateMachine, knownRelationshipsIds, binderFunction.filter));
				});
	}



	@SuppressWarnings("unchecked")
	private <T> Mono<Tuple2<T, DynamicLabels>> determineDynamicLabels(T entityToBeSaved,
			Neo4jPersistentEntity<?> entityMetaData) {
		return entityMetaData.getDynamicLabelsProperty().map(p -> {

			PersistentPropertyAccessor<?> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
			Neo4jPersistentProperty idProperty = entityMetaData.getRequiredIdProperty();
			ReactiveNeo4jClient.RunnableSpec runnableQuery = neo4jClient
					.query(() -> renderer.render(cypherGenerator.createStatementReturningDynamicLabels(entityMetaData)))
					.bind(convertIdValues(idProperty, propertyAccessor.getProperty(idProperty)))
					.to(Constants.NAME_OF_ID).bind(entityMetaData.getStaticLabels()).to(Constants.NAME_OF_STATIC_LABELS_PARAM);

			if (entityMetaData.hasVersionProperty()) {
				runnableQuery = runnableQuery
						.bind((Long) propertyAccessor.getProperty(entityMetaData.getRequiredVersionProperty()))
						.to(Constants.NAME_OF_VERSION_PARAM);
			}

			return runnableQuery.fetch().one().map(m -> (Collection<String>) m.get(Constants.NAME_OF_LABELS))
					.switchIfEmpty(Mono.just(Collections.emptyList()))
					.zipWith(Mono.just((Collection<String>) propertyAccessor.getProperty(p)))
					.map(t -> Tuples.of(entityToBeSaved, new DynamicLabels(entityMetaData, t.getT1(), t.getT2())));
		}).orElse(Mono.just(Tuples.of(entityToBeSaved, DynamicLabels.EMPTY)));
	}

	@Override
	public <T> Flux<T> saveAll(Iterable<T> instances) {
		return transactionalOperator.transactional(saveAllImpl(instances, Collections.emptySet(), null));
	}

	@Override
	public <T> Flux<T> saveAllAs(Iterable<T> instances, BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		return transactionalOperator.transactional(saveAllImpl(instances, null, includeProperty));
	}

	@Override
	public <T, R> Flux<R> saveAllAs(Iterable<T> instances, Class<R> resultType) {

		Assert.notNull(resultType, "ResultType must not be null");

		Class<?> commonElementType = TemplateSupport.findCommonElementType(instances);

		if (commonElementType == null) {
			return Flux.error(() -> new IllegalArgumentException(
					"Could not determine a common element of an heterogeneous collection"));
		}

		if (commonElementType == TemplateSupport.EmptyIterable.class) {
			return Flux.empty();
		}

		if (resultType.isAssignableFrom(commonElementType)) {
			return saveAll(instances).map(resultType::cast);
		}

		ProjectionFactory localProjectionFactory = getProjectionFactory();
		ProjectionInformation projectionInformation = localProjectionFactory.getProjectionInformation(resultType);
		Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(commonElementType, resultType,
				localProjectionFactory, neo4jMappingContext);

		Flux<T> savedInstances = transactionalOperator.transactional(saveAllImpl(instances, pps, null));
		if (projectionInformation.isClosed()) {
			return savedInstances.map(instance -> localProjectionFactory.createProjection(resultType, instance));
		}

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(commonElementType);
		Neo4jPersistentProperty idProperty = entityMetaData.getIdProperty();

		return savedInstances.concatMap(savedInstance -> {
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
			return transactionalOperatorReadOnly.transactional(findById(propertyAccessor.getProperty(idProperty), commonElementType));
		}).map(instance -> localProjectionFactory.createProjection(resultType, instance));
	}

	private <T> Flux<T> saveAllImpl(Iterable<T> instances, @Nullable Collection<PropertyFilter.ProjectedPath> includedProperties, @Nullable BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		Set<Class<?>> types = new HashSet<>();
		List<T> entities = new ArrayList<>();
		instances.forEach(instance -> {
			entities.add(instance);
			types.add(instance.getClass());
		});

		if (entities.isEmpty()) {
			return Flux.empty();
		}

		boolean heterogeneousCollection = types.size() > 1;
		Class<?> domainClass = types.iterator().next();

		Collection<PropertyFilter.ProjectedPath> pps = includeProperty == null ?
				includedProperties :
				TemplateSupport.computeIncludedPropertiesFromPredicate(this.neo4jMappingContext, domainClass,
						includeProperty);

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainClass);
		if (heterogeneousCollection || entityMetaData.isUsingInternalIds() || entityMetaData.hasVersionProperty()
			|| entityMetaData.getDynamicLabelsProperty().isPresent()) {
			log.debug("Saving entities using single statements.");

			NestedRelationshipProcessingStateMachine stateMachine = new NestedRelationshipProcessingStateMachine(neo4jMappingContext);
			return Flux.fromIterable(entities).concatMap(e -> this.saveImpl(e, pps, stateMachine));
		}

		@SuppressWarnings("unchecked") // We can safely assume here that we have a humongous collection with only one single type being either T or extending it
		Function<T, Map<String, Object>> binderFunction = TemplateSupport.createAndApplyPropertyFilter(
				pps, entityMetaData,
				neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) domainClass));
		return (Flux<T>) Flux.deferContextual((ctx) -> Flux.fromIterable(entities)
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
							.map(Tuple3::getT3) // extract PotentiallyModified
							.map(binderFunction).collect(Collectors.toList());
					return neo4jClient
							.query(() -> renderer.render(cypherGenerator.prepareSaveOfMultipleInstancesOf(entityMetaData)))
							.bind(boundedEntityList).to(Constants.NAME_OF_ENTITY_LIST_PARAM)
							.fetchAs(Tuple2.class)
							.mappedBy((t, r) -> Tuples.of(r.get(Constants.NAME_OF_ID), TemplateSupport.convertIdOrElementIdToString(r.get(Constants.NAME_OF_ELEMENT_ID))))
							.all()
							.collectMap(m -> (Value) m.getT1(), m -> (String) m.getT2());
				}).flatMapMany(idToInternalIdMapping -> Flux.fromIterable(entitiesToBeSaved)
						.concatMap(t -> {
							PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(t.getT3());
							Neo4jPersistentProperty idProperty = entityMetaData.getRequiredIdProperty();
							return processRelations(entityMetaData, propertyAccessor, t.getT2(),
								ctx.get("stateMachine"),
								ctx.get("knownRelIds"),
								TemplateSupport.computeIncludePropertyPredicate(pps, entityMetaData));
						}))
				))
				.contextWrite(ctx ->
					ctx
							.put("stateMachine", new NestedRelationshipProcessingStateMachine(neo4jMappingContext, null, null))
							.put("knownRelIds", new HashSet<>())
				);
	}

	@Override
	public <T> Mono<Void> deleteAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		String nameOfParameter = "ids";
		Condition condition = entityMetaData.getIdExpression().in(parameter(nameOfParameter));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		return transactionalOperator.transactional(Mono.defer(() ->
				this.neo4jClient.query(() -> renderer.render(statement))
					.bind(convertIdValues(entityMetaData.getRequiredIdProperty(), ids))
					.to(nameOfParameter).run().then()));
	}

	@Override
	public <T> Mono<Void> deleteById(Object id, Class<T> domainType) {

		Assert.notNull(id, "The given id must not be null");

		String nameOfParameter = "id";
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		return transactionalOperator.transactional(Mono.defer(() ->
				this.neo4jClient.query(() -> renderer.render(statement))
					.bind(convertIdValues(entityMetaData.getRequiredIdProperty(), id))
					.to(nameOfParameter).run().then()));
	}

	@Override
	public <T> Mono<Void> deleteByIdWithVersion(Object id, Class<T> domainType, Neo4jPersistentProperty versionProperty,
										  Object versionValue) {

		String nameOfParameter = "id";
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter))
				.and(Cypher.property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData), versionProperty.getPropertyName())
						.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))
						.or(Cypher.property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData), versionProperty.getPropertyName()).isNull()));

		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData, condition)
				.returning(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData)).build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(nameOfParameter, convertIdValues(entityMetaData.getRequiredIdProperty(), id));
		parameters.put(Constants.NAME_OF_VERSION_PARAM, versionValue);

		return transactionalOperator.transactional(Mono.defer(() ->
						this.neo4jClient.query(() -> renderer.render(statement))
							.bindAll(parameters)
							.fetch().one().switchIfEmpty(Mono.defer(() -> {
								if (entityMetaData.hasVersionProperty()) {
									return Mono.error(() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE));
								}
								return Mono.empty();
							})))
				.then(deleteById(id, domainType)));
	}

	@Override
	public Mono<Void> deleteAll(Class<?> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData);
		return transactionalOperator.transactional(Mono.defer(() -> this.neo4jClient.query(() -> renderer.render(statement)).run().then()));
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, Statement statement) {
		return createExecutableQuery(domainType, null, statement, Collections.emptyMap());
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, String cypherQuery) {
		return createExecutableQuery(domainType, null, cypherQuery, Collections.emptyMap());
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType, Statement statement,
			Map<String, Object> parameters) {

		return createExecutableQuery(domainType, resultType, renderer.render(statement), TemplateSupport.mergeParameters(statement, parameters));
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType, @Nullable String cypherQuery,
			Map<String, Object> parameters) {

		Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction = TemplateSupport
				.getAndDecorateMappingFunction(neo4jMappingContext, domainType, resultType);
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType).withCypherQuery(cypherQuery)
				.withParameters(parameters)
				.usingMappingFunction(mappingFunction).build();
		return this.toExecutableQuery(preparedQuery);
	}

	private <T> Mono<ExecutableQuery<T>> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType, QueryFragmentsAndParameters queryFragmentsAndParameters) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();

		boolean containsPossibleCircles = entityMetaData != null && entityMetaData.containsPossibleCircles(queryFragments::includeField);
		if (containsPossibleCircles && !queryFragments.isScalarValueReturn()) {
			return createNodesAndRelationshipsByIdStatementProvider(entityMetaData, queryFragments, queryFragmentsAndParameters.getParameters())
					.flatMap(finalQueryAndParameters -> {
						var statement = finalQueryAndParameters.toStatement(entityMetaData);
						return createExecutableQuery(domainType, resultType, renderer.render(statement),
								statement.getCatalog().getParameters());
					});
		}

		return createExecutableQuery(domainType, resultType, queryFragments.toStatement(), queryFragmentsAndParameters.getParameters());
	}

	private Mono<NodesAndRelationshipsByIdStatementProvider> createNodesAndRelationshipsByIdStatementProvider(Neo4jPersistentEntity<?> entityMetaData,
		 	QueryFragments queryFragments, Map<String, Object> parameters) {

			return Mono.deferContextual(ctx -> {
				Class<?> rootClass = entityMetaData.getUnderlyingClass();

				Set<String> rootNodeIds = ctx.get("rootNodes");
				Map<String, Set<String>> relationshipsToRelatedNodeIds = ctx.get("relationshipsToRelatedNodeIds");
				return Flux.fromIterable(entityMetaData.getRelationshipsInHierarchy(queryFragments::includeField))
						.concatMap(relationshipDescription -> {

							Statement statement = cypherGenerator.prepareMatchOf(entityMetaData, relationshipDescription,
									queryFragments.getMatchOn(), queryFragments.getCondition())
									.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

							Map<String, Object> usedParameters = new HashMap<>(parameters);
							usedParameters.putAll(statement.getCatalog().getParameters());
							return neo4jClient.query(renderer.render(statement))
									.bindAll(usedParameters)
									.fetchAs(Tuple2.class)
									.mappedBy((t, r) -> {
										Collection<String> rootIds = r.get(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE).asList(TemplateSupport::convertIdOrElementIdToString);
										rootNodeIds.addAll(rootIds);
										Collection<String> newRelationshipIds = r.get(Constants.NAME_OF_SYNTHESIZED_RELATIONS).asList(TemplateSupport::convertIdOrElementIdToString);
										Collection<String> newRelatedNodeIds = r.get(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES).asList(TemplateSupport::convertIdOrElementIdToString);
										return Tuples.of(newRelationshipIds, newRelatedNodeIds);
									})
									.one()
									.map((t) -> {
										//noinspection unchecked
										return (Tuple2<Collection<String>, Collection<String>>) t;
									})
									.expand(iterateAndMapNextLevel(relationshipDescription, queryFragments, rootClass, PropertyPathWalkStep.empty()));
						})
						.then(Mono.fromSupplier(() -> new NodesAndRelationshipsByIdStatementProvider(rootNodeIds, relationshipsToRelatedNodeIds.keySet(), relationshipsToRelatedNodeIds.values().stream().flatMap(Collection::stream).toList(), queryFragments, elementIdOrIdFunction)));
			})
			.contextWrite(ctx -> ctx
					.put("rootNodes", ConcurrentHashMap.newKeySet())
					.put("relationshipsToRelatedNodeIds", new ConcurrentHashMap<>()));

	}

	private Flux<Tuple2<Collection<String>, Collection<String>>> iterateNextLevel(Collection<String> relatedNodeIds,
			  RelationshipDescription sourceRelationshipDescription, QueryFragments queryFragments,
			  Class<?> rootClass, PropertyPathWalkStep currentPathStep) {

		NodeDescription<?> target = sourceRelationshipDescription.getTarget();

		@SuppressWarnings("unchecked")
		String fieldName = ((Association<Neo4jPersistentProperty>) sourceRelationshipDescription).getInverse().getFieldName();

		PropertyPathWalkStep nextPathStep = currentPathStep.with((sourceRelationshipDescription.hasRelationshipProperties() ?
				fieldName + "." + ((Neo4jPersistentEntity<?>) sourceRelationshipDescription.getRelationshipPropertiesEntity())
						.getPersistentProperty(TargetNode.class).getFieldName() : fieldName));

		return Flux.fromIterable(target
				.getRelationshipsInHierarchy(
						relaxedPropertyPath -> {
							PropertyFilter.RelaxedPropertyPath prepend = relaxedPropertyPath.prepend(nextPathStep.path);
							prepend = PropertyFilter.RelaxedPropertyPath.withRootType(rootClass).append(prepend.toDotPath());
							return queryFragments.includeField(prepend);
						}
				))
			.concatMap(relDe -> {
				Node node = anyNode(Constants.NAME_OF_TYPED_ROOT_NODE.apply(target));

				Statement statement = cypherGenerator
						.prepareMatchOf(target, relDe, null,
								elementIdOrIdFunction.apply(node).in(Cypher.parameter(Constants.NAME_OF_ID)))
						.returning(cypherGenerator.createGenericReturnStatement()).build();

				return neo4jClient.query(renderer.render(statement))
						.bindAll(Collections.singletonMap(Constants.NAME_OF_ID, TemplateSupport.convertToLongIdOrStringElementId(relatedNodeIds)))
						.fetchAs(Tuple2.class)
						.mappedBy((t, r) -> {
							Collection<String> newRelationshipIds = r.get(Constants.NAME_OF_SYNTHESIZED_RELATIONS).asList(TemplateSupport::convertIdOrElementIdToString);
							Collection<String> newRelatedNodeIds = r.get(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES).asList(TemplateSupport::convertIdOrElementIdToString);

							return Tuples.of(newRelationshipIds, newRelatedNodeIds);
						})
						.one()
						.map((t) -> {
							//noinspection unchecked
							return (Tuple2<Collection<String>, Collection<String>>) t;
						})
						.expand(object -> iterateAndMapNextLevel(relDe, queryFragments, rootClass, nextPathStep).apply(object));
			});

	}

	@NonNull
	private Function<Tuple2<Collection<String>, Collection<String>>,
			Publisher<Tuple2<Collection<String>, Collection<String>>>> iterateAndMapNextLevel(
			RelationshipDescription relationshipDescription, QueryFragments queryFragments, Class<?> rootClass, PropertyPathWalkStep currentPathStep) {

		return newRelationshipAndRelatedNodeIds ->
			Flux.deferContextual(ctx -> {
				Map<String, Set<String>> relationshipsToRelatedNodeIds = ctx.get("relationshipsToRelatedNodeIds");
				Map<String, Set<String>> relatedNodesVisited = new HashMap<>(relationshipsToRelatedNodeIds);

				Collection<String> newRelationshipIds = newRelationshipAndRelatedNodeIds.getT1();

				Collection<String> newRelatedNodeIds = newRelationshipAndRelatedNodeIds.getT2();
				Set<String> relatedIds = ConcurrentHashMap.newKeySet(newRelatedNodeIds.size());
				relatedIds.addAll(newRelatedNodeIds);

				for (String newRelationshipId : newRelationshipIds) {
					relatedNodesVisited.put(newRelationshipId, relatedIds);
					Set<String> knownRelatedNodesBefore = relationshipsToRelatedNodeIds.get(newRelationshipId);
					if (knownRelatedNodesBefore != null) {
						Set<String> mergedKnownRelatedNodes = new HashSet<>(knownRelatedNodesBefore);
						// there are already existing nodes in there for this relationship
						mergedKnownRelatedNodes.addAll(relatedIds);
						relatedNodesVisited.put(newRelationshipId, mergedKnownRelatedNodes);
						relatedIds.removeAll(knownRelatedNodesBefore);
					}
				}
				relationshipsToRelatedNodeIds.putAll(relatedNodesVisited);

				if (relatedIds.isEmpty()) {
					return Mono.empty();
				}

				return iterateNextLevel(newRelatedNodeIds, relationshipDescription, queryFragments, rootClass, currentPathStep);
			});
	}

	/**
	 * Starts of processing of the relationships.
	 *
	 * @param neo4jPersistentEntity  The description of the instance to save
	 * @param parentPropertyAccessor The property accessor of the parent, to modify the relationships
	 * @param isParentObjectNew      A flag if the parent was new
	 * @param stateMachine           Initial state of entity processing
	 * @param includeProperty        A predicate telling to include a relationship property or not
	 * @param <T>                    The type of the object being initially processed
	 * @return A mono representing the whole stream of save operations, eventually containing the owner of the relations being processed
	 */
	private <T> Mono<T> processRelations(
			Neo4jPersistentEntity<?> neo4jPersistentEntity,
			PersistentPropertyAccessor<?> parentPropertyAccessor,
			boolean isParentObjectNew,
			NestedRelationshipProcessingStateMachine stateMachine,
			Collection<Object> knownRelationshipsIds,
			PropertyFilter includeProperty
	) {

		PropertyFilter.RelaxedPropertyPath startingPropertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(neo4jPersistentEntity.getUnderlyingClass());
		return processNestedRelations(neo4jPersistentEntity, parentPropertyAccessor, isParentObjectNew,
				stateMachine, knownRelationshipsIds, includeProperty, startingPropertyPath);
	}

	private <T> Mono<T> processNestedRelations(Neo4jPersistentEntity<?> sourceEntity, PersistentPropertyAccessor<?> parentPropertyAccessor,
											   boolean isParentObjectNew, NestedRelationshipProcessingStateMachine stateMachine,
											   Collection<Object> knownRelationshipsIds,
											   PropertyFilter includeProperty, PropertyFilter.RelaxedPropertyPath previousPath) {

		Object fromId = parentPropertyAccessor.getProperty(sourceEntity.getRequiredIdProperty());
		List<Mono<Void>> relationshipDeleteMonos = new ArrayList<>();
		List<Flux<RelationshipHandler>> relationshipCreationCreations = new ArrayList<>();

		AssociationHandlerSupport.of(sourceEntity).doWithAssociations(association -> {

			// create context to bundle parameters
			NestedRelationshipContext relationshipContext = NestedRelationshipContext.of(association, parentPropertyAccessor, sourceEntity);
			if (relationshipContext.isReadOnly()) {
				return;
			}

			Object rawValue = relationshipContext.getValue();
			Collection<?> relatedValuesToStore = MappingSupport.unifyRelationshipValue(relationshipContext.getInverse(),
					rawValue);

			RelationshipDescription relationshipDescription = relationshipContext.getRelationship();

			PropertyFilter.RelaxedPropertyPath currentPropertyPath = previousPath.append(relationshipDescription.getFieldName());

			if (!includeProperty.isNotFiltering() && !includeProperty.contains(currentPropertyPath)) {
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
			ProcessState processState = stateMachine.getStateOf(fromId, relationshipDescription, relatedValuesToStore);
			if (processState == ProcessState.PROCESSED_ALL_RELATIONSHIPS || processState == ProcessState.PROCESSED_BOTH) {
				return;
			}

			// Remove all relationships before creating all new if the entity is not new and the relationship
			// has not been processed before.
			// This avoids the usage of cache but might have significant impact on overall performance
			boolean canUseElementId = TemplateSupport.rendererRendersElementId(renderer);
			if (!isParentObjectNew && !stateMachine.hasProcessedRelationship(fromId, relationshipDescription)) {

				if (idProperty != null) {
					for (Object relatedValueToStore : relatedValuesToStore) {
						if (relatedValueToStore == null) {
							continue;
						}

						Object id = relationshipContext
								.getRelationshipPropertiesPropertyAccessor(relatedValueToStore)
								.getProperty(idProperty);
						if (id != null) {
							knownRelationshipsIds.add(id);
						}
					}
				}

				Statement relationshipRemoveQuery = cypherGenerator.prepareDeleteOf(sourceEntity, relationshipDescription, canUseElementId);

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

				Object relatedObjectBeforeCallbacksApplied = relationshipContext.identifyAndExtractRelationshipTargetNode(relatedValueToStore);
				Neo4jPersistentEntity<?> targetEntity = neo4jMappingContext.getRequiredPersistentEntity(relatedObjectBeforeCallbacksApplied.getClass());
				boolean isNewEntity = targetEntity.isNew(relatedObjectBeforeCallbacksApplied);

				return Mono.deferContextual(ctx ->

						(stateMachine.hasProcessedValue(relatedObjectBeforeCallbacksApplied)
								? Mono.just(stateMachine.getProcessedAs(relatedObjectBeforeCallbacksApplied))
								: eventSupport.maybeCallBeforeBind(relatedObjectBeforeCallbacksApplied))

						.flatMap(newRelatedObject -> {

							Mono<Tuple2<AtomicReference<Object>, AtomicReference<Entity>>> queryOrSave;
							if (stateMachine.hasProcessedValue(relatedValueToStore)) {
								AtomicReference<Object> relatedInternalId = new AtomicReference<>();
								Object possibleValue = stateMachine.getObjectId(relatedValueToStore);
								if (possibleValue != null) {
									relatedInternalId.set(possibleValue);
								}
								queryOrSave = Mono.just(Tuples.of(relatedInternalId, new AtomicReference<>()));
							} else {
								Mono<Entity> savedEntity;
								if (isNewEntity || relationshipDescription.cascadeUpdates()) {
									savedEntity = saveRelatedNode(newRelatedObject, targetEntity, includeProperty, currentPropertyPath);
								} else {
									var targetPropertyAccessor = targetEntity.getPropertyAccessor(newRelatedObject);
									var requiredIdProperty = targetEntity.getRequiredIdProperty();
									savedEntity = loadRelatedNode(targetEntity, targetPropertyAccessor.getProperty(requiredIdProperty));
								}

								queryOrSave = savedEntity
										.map(entity -> Tuples.of(new AtomicReference<>((Object) (TemplateSupport.rendererCanUseElementIdIfPresent(renderer, targetEntity) ? entity.elementId() : entity.id())), new AtomicReference<>(entity)))
										.doOnNext(t -> {
											var relatedInternalId = t.getT1().get();
											stateMachine.markEntityAsProcessed(relatedValueToStore, relatedInternalId);
											if (relatedValueToStore instanceof MappingSupport.RelationshipPropertiesWithEntityHolder) {
												Object entity = ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValueToStore).getRelatedEntity();
												stateMachine.markAsAliased(entity, relatedInternalId);
											}
										});
							}

							return queryOrSave.flatMap(idAndEntity -> {
									Object relatedInternalId = idAndEntity.getT1().get();
									Entity savedEntity = idAndEntity.getT2().get();
									Neo4jPersistentProperty requiredIdProperty = targetEntity.getRequiredIdProperty();
									PersistentPropertyAccessor<?> targetPropertyAccessor = targetEntity.getPropertyAccessor(newRelatedObject);
									Object possibleInternalLongId = targetPropertyAccessor.getProperty(requiredIdProperty);
									relatedInternalId = TemplateSupport.retrieveOrSetRelatedId(targetEntity, targetPropertyAccessor, Optional.ofNullable(savedEntity), relatedInternalId);
									if (savedEntity != null) {
										TemplateSupport.updateVersionPropertyIfPossible(targetEntity, targetPropertyAccessor, savedEntity);
									}
									stateMachine.markAsAliased(relatedObjectBeforeCallbacksApplied, targetPropertyAccessor.getBean());
									stateMachine.markRelationshipAsProcessed(possibleInternalLongId == null ? relatedInternalId : possibleInternalLongId,
												relationshipDescription.getRelationshipObverse());

									Object idValue = idProperty != null
											? relationshipContext
											.getRelationshipPropertiesPropertyAccessor(relatedValueToStore).getProperty(idProperty)
											: null;

									boolean isNewRelationship = idValue == null;
									CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatementForSingleRelationship(
											sourceEntity, relationshipDescription, relatedValueToStore, isNewRelationship, canUseElementId);

									Map<String, Object> properties = new HashMap<>();
									properties.put(Constants.FROM_ID_PARAMETER_NAME, convertIdValues(sourceEntity.getRequiredIdProperty(), fromId));
									properties.put(Constants.TO_ID_PARAMETER_NAME, relatedInternalId);
									properties.put(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM, idValue);
									var update = true;
									if (!relationshipDescription.isDynamic() && relationshipDescription.hasRelationshipProperties()) {
										var hlp = ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValueToStore);
										var hasProcessedRelationshipEntity = stateMachine.hasProcessedRelationshipEntity(parentPropertyAccessor.getBean(), hlp.getRelatedEntity(), relationshipContext.getRelationship());
										if (hasProcessedRelationshipEntity) {
											stateMachine.requireIdUpdate(sourceEntity, relationshipDescription, canUseElementId, fromId, relatedInternalId, relationshipContext, relatedValueToStore, idProperty);
											update = false;
										} else {
											stateMachine.storeProcessRelationshipEntity(hlp, parentPropertyAccessor.getBean(), hlp.getRelatedEntity(), relationshipContext.getRelationship());
										}
									}
									List<Object> rows = new ArrayList<>();
									rows.add(properties);
									statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, rows);
									// in case of no properties the bind will just return an empty map
									if (update) {
										return neo4jClient
												.query(renderer.render(statementHolder.getStatement()))
												.bind(convertIdValues(sourceEntity.getRequiredIdProperty(), fromId)) //
												.to(Constants.FROM_ID_PARAMETER_NAME) //
												.bind(relatedInternalId) //
												.to(Constants.TO_ID_PARAMETER_NAME) //
												.bind(idValue) //
												.to(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM) //
												.bindAll(statementHolder.getProperties())
												.fetchAs(Object.class)
												.mappedBy((t, r) -> IdentitySupport.mapperForRelatedIdValues(idProperty).apply(r))
												.one()
												.flatMap(relationshipInternalId -> {
													if (idProperty != null && isNewRelationship) {
														relationshipContext
																.getRelationshipPropertiesPropertyAccessor(relatedValueToStore)
																.setProperty(idProperty, relationshipInternalId);
														knownRelationshipsIds.add(relationshipInternalId);
													}

													Mono<Object> nestedRelationshipsSignal = null;
													if (processState != ProcessState.PROCESSED_ALL_VALUES) {
														nestedRelationshipsSignal = processNestedRelations(targetEntity, targetPropertyAccessor, targetEntity.isNew(newRelatedObject), stateMachine, knownRelationshipsIds, includeProperty, currentPropertyPath);
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
									}
									return Mono.fromSupplier(() -> MappingSupport.getRelationshipOrRelationshipPropertiesObject(
											neo4jMappingContext,
											relationshipDescription.hasRelationshipProperties(),
											relationshipProperty.isDynamicAssociation(),
											relatedValueToStore,
											targetPropertyAccessor));
								})
								.doOnNext(potentiallyRecreatedRelatedObject -> {
									RelationshipHandler handler = ctx.get(CONTEXT_RELATIONSHIP_HANDLER);
									handler.handle(relatedValueToStore, relatedObjectBeforeCallbacksApplied, potentiallyRecreatedRelatedObject);
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

		@SuppressWarnings("unchecked")
		Mono<T> deleteAndThanCreateANew = (Mono<T>) Flux.concat(relationshipDeleteMonos)
				.thenMany(Flux.concat(relationshipCreationCreations))
				.doOnNext(objects -> objects.applyFinalResultToOwner(parentPropertyAccessor))
				.checkpoint()
				.then(stateMachine.updateRelationshipIds(this::getRelationshipId))
				.then(Mono.fromSupplier(parentPropertyAccessor::getBean));
		return deleteAndThanCreateANew;

	}

	private Mono<Object> getRelationshipId(Statement statement, Neo4jPersistentProperty idProperty, Object fromId, Object toId) {

		return neo4jClient.query(renderer.render(statement))
				.bind(convertIdValues(idProperty, fromId)) //
				.to(Constants.FROM_ID_PARAMETER_NAME) //
				.bind(toId) //
				.to(Constants.TO_ID_PARAMETER_NAME) //
				.fetchAs(Object.class)
				.mappedBy((t, r) -> IdentitySupport.mapperForRelatedIdValues(idProperty).apply(r))
				.one();
	}

	// The pendant to {@link #saveRelatedNode(Object, Neo4jPersistentEntity, PropertyFilter, PropertyFilter.RelaxedPropertyPath)}
	// We can't do without a query, as we need to refresh the internal id
	private Mono<Entity> loadRelatedNode(NodeDescription<?> targetNodeDescription, Object relatedInternalId) {

		var targetPersistentEntity = (Neo4jPersistentEntity<?>) targetNodeDescription;
		var queryFragmentsAndParameters = QueryFragmentsAndParameters.forFindById(targetPersistentEntity, convertIdValues(targetPersistentEntity.getRequiredIdProperty(), relatedInternalId));
		var nodeName = Constants.NAME_OF_TYPED_ROOT_NODE.apply(targetNodeDescription).getValue();

		return neo4jClient
				.query(() -> renderer.render(
						cypherGenerator.prepareFindOf(targetNodeDescription, queryFragmentsAndParameters.getQueryFragments().getMatchOn(),
								queryFragmentsAndParameters.getQueryFragments().getCondition()).returning(nodeName).build()))
				.bindAll(queryFragmentsAndParameters.getParameters())
				.fetchAs(Entity.class).mappedBy((t, r) -> r.get(nodeName).asNode())
				.one();
	}

	private Mono<Entity> saveRelatedNode(Object relatedNode, Neo4jPersistentEntity<?> targetNodeDescription, PropertyFilter includeProperty, PropertyFilter.RelaxedPropertyPath currentPropertyPath) {

		return determineDynamicLabels(relatedNode, targetNodeDescription)
				.flatMap(t -> {
					Object entity = t.getT1();
					@SuppressWarnings("rawtypes")
					Class entityType = entity.getClass();
					DynamicLabels dynamicLabels = t.getT2();
					@SuppressWarnings("unchecked")
					Function<Object, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(entityType);
					String idPropertyName = targetNodeDescription.getIdProperty().getPropertyName();
					IdDescription idDescription = targetNodeDescription.getIdDescription();
					boolean assignedId = idDescription.isAssignedId() || idDescription.isExternallyGeneratedId();
					binderFunction = binderFunction.andThen(tree -> {
						@SuppressWarnings("unchecked")
						Map<String, Object> properties = (Map<String, Object>) tree.get(Constants.NAME_OF_PROPERTIES_PARAM);

						if (!includeProperty.isNotFiltering()) {
							properties.entrySet().removeIf(e -> {
								// we cannot skip the id property if it is an assigned id
								boolean isIdProperty = e.getKey().equals(idPropertyName);
								return !(assignedId && isIdProperty) && !includeProperty.contains(currentPropertyPath.append(e.getKey()));
							});
						}
						return tree;
					});
					return neo4jClient
							.query(() -> renderer.render(cypherGenerator.prepareSaveOf(targetNodeDescription, dynamicLabels, TemplateSupport.rendererRendersElementId(renderer))))
							.bind(entity).with(binderFunction)
							.fetchAs(Entity.class)
							.one();
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

			QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();
			Neo4jPersistentEntity<?> entityMetaData = (Neo4jPersistentEntity<?>) queryFragmentsAndParameters.getNodeDescription();

			boolean containsPossibleCircles = entityMetaData != null && entityMetaData.containsPossibleCircles(queryFragments::includeField);
			if (cypherQuery == null || containsPossibleCircles) {

				if (containsPossibleCircles && !queryFragments.isScalarValueReturn()) {
					return createNodesAndRelationshipsByIdStatementProvider(entityMetaData, queryFragments, finalParameters)
							.map(nodesAndRelationshipsById -> {
								var statement = nodesAndRelationshipsById.toStatement(entityMetaData);
								ReactiveNeo4jClient.MappingSpec<T> mappingSpec = this.neo4jClient
										.query(renderer.render(statement))
										.bindAll(statement.getCatalog().getParameters())
										.fetchAs(resultType);

								ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec = preparedQuery.getOptionalMappingFunction()
										.map(mappingSpec::mappedBy).orElse(mappingSpec);

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
					.map(mappingSpec::mappedBy).orElse(mappingSpec);

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

		Configuration cypherDslConfiguration = beanFactory
				.getBeanProvider(Configuration.class)
				.getIfAvailable(Configuration::defaultConfig);
		this.renderer = Renderer.getRenderer(cypherDslConfiguration);
		this.elementIdOrIdFunction = SpringDataCypherDsl.elementIdOrIdFunction.apply(cypherDslConfiguration.getDialect());
		this.cypherGenerator.setElementIdOrIdFunction(elementIdOrIdFunction);

		if (this.transactionalOperator != null && this.transactionalOperatorReadOnly != null) {
			return;
		}

		ReactiveTransactionManager reactiveTransactionManager = null;
		var iter = beanFactory.getBeanProvider(ReactiveTransactionManager.class).stream().iterator();
		while (iter.hasNext()) {
			ReactiveTransactionManager transactionManagerCandidate = iter.next();
			if (transactionManagerCandidate instanceof ReactiveNeo4jTransactionManager reactiveNeo4jTransactionManager) {
				if (reactiveTransactionManager != null) {
					throw new IllegalStateException("Multiple ReactiveNeo4jTransactionManagers are defined in this context. " +
							"If this in intended, please pass the transaction manager to use with this ReactiveNeo4jTemplate in the constructor");
				}
				reactiveTransactionManager = reactiveNeo4jTransactionManager;
			}
		}
		setTransactionManager(reactiveTransactionManager);
	}

	private void setTransactionManager(@Nullable ReactiveTransactionManager reactiveTransactionManager) {
		if (reactiveTransactionManager == null) {
			return;
		}
		this.transactionalOperator = TransactionalOperator.create(reactiveTransactionManager);
		this.transactionalOperatorReadOnly = TransactionalOperator.create(reactiveTransactionManager, readOnlyTransactionDefinition);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = beanClassLoader == null ? org.springframework.util.ClassUtils.getDefaultClassLoader() : beanClassLoader;
	}

	@Override
	public <T> ExecutableSave<T> save(Class<T> domainType) {
		return new ReactiveFluentOperationSupport(this).save(domainType);
	}

	String render(Statement statement) {
		return this.renderer.render(statement);
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

			return transactionalOperator.transactional(fetchSpec.all().switchOnFirst((signal, f) -> {
				if (signal.hasValue() && preparedQuery.resultsHaveBeenAggregated()) {
					return f.concatMap(nested -> Flux.fromIterable((Collection<T>) nested).distinct()).distinct();
				}
				return f;
			}));
		}

		/**
		 * @return A single result
		 * @throws IncorrectResultSizeDataAccessException if there is no or more than one result
		 */
		public Mono<T> getSingleResult() {
			return transactionalOperator.transactional(fetchSpec.one().map(t -> {
				if (t instanceof LinkedHashSet) {
					@SuppressWarnings("unchecked")
					T firstItem = (T) ((LinkedHashSet<?>) t).iterator().next();
					return firstItem;
				}
				return t;
			}).onErrorMap(IndexOutOfBoundsException.class, e -> new IncorrectResultSizeDataAccessException(e.getMessage(), 1)));
		}
	}
}
