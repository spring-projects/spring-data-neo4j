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

import java.util.AbstractMap;
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
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.FunctionInvocation;
import org.neo4j.cypherdsl.core.Named;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.EntityCallbacks;
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
import org.springframework.data.neo4j.core.mapping.callback.EventSupport;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.support.UserDefinedChangeEvaluator;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.NoResultException;
import org.springframework.data.neo4j.repository.query.QueryFragments;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.asterisk;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

/**
 * The Neo4j template combines various operations. All simple repositories will delegate
 * to it. It provides a convenient way of dealing with mapped domain objects without
 * having to define repositories for each type.
 *
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @author Gerrit Meier
 * @author Corey Beres
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
@SuppressWarnings("DataFlowIssue")
public final class Neo4jTemplate
		implements Neo4jOperations, FluentNeo4jOperations, BeanClassLoaderAware, BeanFactoryAware {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(Neo4jTemplate.class));

	private static final String OPTIMISTIC_LOCKING_ERROR_MESSAGE = "An entity with the required version does not exist.";

	private static final TransactionDefinition readOnlyTransactionDefinition = new TransactionDefinition() {
		@Override
		public boolean isReadOnly() {
			return true;
		}
	};

	private final Neo4jClient neo4jClient;

	private final Neo4jMappingContext neo4jMappingContext;

	private final CypherGenerator cypherGenerator;

	@Nullable
	private ClassLoader beanClassLoader;

	private EventSupport eventSupport;

	@Nullable
	private ProjectionFactory projectionFactory;

	private Renderer renderer;

	private Function<Named, FunctionInvocation> elementIdOrIdFunction;

	@Nullable
	private TransactionTemplate transactionTemplate;

	@Nullable
	private TransactionTemplate transactionTemplateReadOnly;
	private final Map<Class, UserDefinedChangeEvaluator> userDefinedChangeEvaluators = new HashMap<>();

	public Neo4jTemplate(Neo4jClient neo4jClient) {
		this(neo4jClient, new Neo4jMappingContext());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {
		this(neo4jClient, neo4jMappingContext, EntityCallbacks.create());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
			PlatformTransactionManager transactionManager) {
		this(neo4jClient, neo4jMappingContext, EntityCallbacks.create(), transactionManager);
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
			EntityCallbacks entityCallbacks) {
		this(neo4jClient, neo4jMappingContext, entityCallbacks, null);
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
			EntityCallbacks entityCallbacks, @Nullable PlatformTransactionManager platformTransactionManager) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
		this.eventSupport = EventSupport.useExistingCallbacks(neo4jMappingContext, entityCallbacks);
		this.renderer = Renderer.getDefaultRenderer();
		this.elementIdOrIdFunction = SpringDataCypherDsl.elementIdOrIdFunction.apply(null);
		setTransactionManager(platformTransactionManager);
	}

	ProjectionFactory getProjectionFactory() {
		return Objects.requireNonNull(this.projectionFactory,
				"Projection support for the Neo4j template is only available when the template is a proper and fully initialized Spring bean.");
	}

	private <T> T execute(TransactionCallback<T> action) throws TransactionException {
		return Objects.requireNonNull(Objects.requireNonNull(this.transactionTemplate).execute(action));
	}

	private <T> T executeReadOnly(TransactionCallback<T> action) throws TransactionException {
		return Objects.requireNonNull(Objects.requireNonNull(this.transactionTemplateReadOnly).execute(action));
	}

	private void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
		Objects.requireNonNull(this.transactionTemplate).executeWithoutResult(action);
	}

	@Override
	public long count(Class<?> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);
		Statement statement = this.cypherGenerator.prepareMatchOf(entityMetaData)
			.returning(Cypher.count(asterisk()))
			.build();

		return count(statement);
	}

	@Override
	public long count(Statement statement) {
		return count(statement, Collections.emptyMap());
	}

	@Override
	public long count(Statement statement, Map<String, Object> parameters) {

		return count(this.renderer.render(statement), TemplateSupport.mergeParameters(statement, parameters));
	}

	@Override
	public long count(String cypherQuery) {
		return count(cypherQuery, Collections.emptyMap());
	}

	@Override
	public long count(String cypherQuery, Map<String, Object> parameters) {
		return executeReadOnly(tx -> {
			PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
				.withCypherQuery(cypherQuery)
				.withParameters(parameters)
				.build();
			return toExecutableQuery(preparedQuery, true).getRequiredSingleResult();
		});
	}

	@Override
	public <T> List<T> findAll(Class<T> domainType) {

		return doFindAll(domainType, null);
	}

	private <T> List<T> doFindAll(Class<T> domainType, @Nullable Class<?> resultType) {
		return executeReadOnly(tx -> {
			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);
			return createExecutableQuery(domainType, resultType,
					QueryFragmentsAndParameters.forFindAll(entityMetaData, this.neo4jMappingContext), true)
				.getResults();
		});
	}

	@Override
	public <T> List<T> findAll(Statement statement, Class<T> domainType) {
		return executeReadOnly(tx -> createExecutableQuery(domainType, statement, true).getResults());
	}

	@Override
	public <T> List<T> findAll(Statement statement, Map<String, Object> parameters, Class<T> domainType) {
		return executeReadOnly(tx -> createExecutableQuery(domainType, null, statement, parameters, true).getResults());
	}

	@Override
	public <T> Optional<T> findOne(Statement statement, Map<String, Object> parameters, Class<T> domainType) {
		return executeReadOnly(
				tx -> createExecutableQuery(domainType, null, statement, parameters, true).getSingleResult());
	}

	@Override
	public <T> List<T> findAll(String cypherQuery, Class<T> domainType) {
		return executeReadOnly(tx -> createExecutableQuery(domainType, cypherQuery, true).getResults());
	}

	@Override
	public <T> List<T> findAll(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return executeReadOnly(
				tx -> createExecutableQuery(domainType, null, cypherQuery, parameters, true).getResults());
	}

	@Override
	public <T> Optional<T> findOne(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return executeReadOnly(
				tx -> createExecutableQuery(domainType, null, cypherQuery, parameters, true).getSingleResult());
	}

	@Override
	public <T> ExecutableFind<T> find(Class<T> domainType) {
		return new FluentOperationSupport(this).find(domainType);
	}

	@SuppressWarnings("unchecked")
	<T, R> List<R> doFind(@Nullable String cypherQuery, @Nullable Map<String, Object> parameters, Class<T> domainType,
			Class<R> resultType, TemplateSupport.FetchType fetchType,
			@Nullable QueryFragmentsAndParameters queryFragmentsAndParameters) {

		return executeReadOnly(tx -> {
			List<T> intermediaResults;
			if (cypherQuery == null && queryFragmentsAndParameters == null
					&& fetchType == TemplateSupport.FetchType.ALL) {
				intermediaResults = doFindAll(domainType, resultType);
			}
			else {
				ExecutableQuery<T> executableQuery;
				if (queryFragmentsAndParameters == null && cypherQuery != null) {
					executableQuery = createExecutableQuery(domainType, resultType, cypherQuery,
							(parameters != null) ? parameters : Collections.emptyMap(), true);
				}
				else {
					executableQuery = createExecutableQuery(domainType, resultType,
							Objects.requireNonNull(queryFragmentsAndParameters), true);
				}
				intermediaResults = switch (fetchType) {
					case ALL -> executableQuery.getResults();
					case ONE -> executableQuery.getSingleResult()
						.map(Collections::singletonList)
						.orElseGet(Collections::emptyList);
				};
			}

			if (resultType.isAssignableFrom(domainType)) {
				return (List<R>) intermediaResults;
			}

			if (resultType.isInterface()) {
				return intermediaResults.stream()
					.map(instance -> getProjectionFactory().createProjection(resultType, instance))
					.collect(Collectors.toList());
			}

			DtoInstantiatingConverter converter = new DtoInstantiatingConverter(resultType, this.neo4jMappingContext);
			return intermediaResults.stream()
				.map(EntityInstanceWithSource.class::cast)
				.map(converter::convert)
				.map(v -> (R) v)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		});
	}

	@Override
	public <T> boolean existsById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);

		QueryFragmentsAndParameters fragmentsAndParameters = QueryFragmentsAndParameters.forExistsById(entityMetaData,
				TemplateSupport.convertIdValues(this.neo4jMappingContext, entityMetaData.getRequiredIdProperty(), id));

		Statement statement = fragmentsAndParameters.getQueryFragments().toStatement();
		Map<String, Object> parameters = fragmentsAndParameters.getParameters();

		return count(statement, parameters) > 0;
	}

	@Override
	public <T> Optional<T> findById(Object id, Class<T> domainType) {
		return executeReadOnly(tx -> {
			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);

			return createExecutableQuery(domainType, null,
					QueryFragmentsAndParameters.forFindById(entityMetaData,
							TemplateSupport.convertIdValues(this.neo4jMappingContext,
									entityMetaData.getRequiredIdProperty(), id),
							this.neo4jMappingContext),
					true)
				.getSingleResult();
		});
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> domainType) {
		return executeReadOnly(tx -> {
			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);

			return createExecutableQuery(domainType, null,
					QueryFragmentsAndParameters.forFindByAllId(entityMetaData,
							TemplateSupport.convertIdValues(this.neo4jMappingContext,
									entityMetaData.getRequiredIdProperty(), ids),
							this.neo4jMappingContext),
					true)
				.getResults();
		});
	}

	@Override
	public <T> T save(T instance) {
		Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport
			.getInputPropertiesForAggregateBoundary(instance.getClass(), this.neo4jMappingContext);
		return execute(tx -> saveImpl(instance, pps, null));

	}

	@Override
	@Nullable public <T> T saveAs(T instance, BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		if (instance == null) {
			return null;
		}
		return execute(tx -> saveImpl(instance, TemplateSupport.computeIncludedPropertiesFromPredicate(
				this.neo4jMappingContext, instance.getClass(), includeProperty), null));
	}

	@Override
	@Nullable public <T, R> R saveAs(T instance, Class<R> resultType) {

		Assert.notNull(resultType, "ResultType must not be null");
		if (instance == null) {
			return null;
		}

		return execute(tx -> {

			if (resultType.equals(instance.getClass())) {
				return resultType.cast(save(instance));
			}

			ProjectionFactory localProjectionFactory = getProjectionFactory();
			ProjectionInformation projectionInformation = localProjectionFactory.getProjectionInformation(resultType);
			Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(instance.getClass(),
					resultType, localProjectionFactory, this.neo4jMappingContext);

			T savedInstance = saveImpl(instance, pps, null);
			if (!resultType.isInterface()) {
				@SuppressWarnings("unchecked")
				R result = (R) new DtoInstantiatingConverter(resultType, this.neo4jMappingContext)
					.convertDirectly(savedInstance);
				return result;
			}
			if (projectionInformation.isClosed()) {
				return localProjectionFactory.createProjection(resultType, savedInstance);
			}

			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext
				.getRequiredPersistentEntity(savedInstance.getClass());
			Neo4jPersistentProperty idProperty = entityMetaData.getRequiredIdProperty();
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
			return localProjectionFactory.createProjection(resultType, this
				.findById(Objects.requireNonNull(propertyAccessor.getProperty(idProperty)), savedInstance.getClass())
				.orElseThrow());
		});
	}

	private <T> T saveImpl(T instance, @Nullable Collection<PropertyFilter.ProjectedPath> includedProperties,
			@Nullable NestedRelationshipProcessingStateMachine stateMachine) {

		if ((stateMachine != null && stateMachine.hasProcessedValue(instance))
			|| (this.userDefinedChangeEvaluators.containsKey(instance.getClass()) && !this.userDefinedChangeEvaluators.get(instance.getClass()).needsUpdate(instance))) {
			return instance;
		}

		Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext
			.getRequiredPersistentEntity(instance.getClass());
		boolean isEntityNew = entityMetaData.isNew(instance);

		T entityToBeSaved = this.eventSupport.maybeCallBeforeBind(instance);

		DynamicLabels dynamicLabels = determineDynamicLabels(entityToBeSaved, entityMetaData);

		@SuppressWarnings("unchecked") // Applies to retrieving the meta data
		TemplateSupport.FilteredBinderFunction<T> binderFunction = TemplateSupport.createAndApplyPropertyFilter(
				includedProperties, entityMetaData,
				this.neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass()));
		var statement = this.cypherGenerator.prepareSaveOf(entityMetaData, dynamicLabels,
				TemplateSupport.rendererRendersElementId(this.renderer));
		Optional<Entity> newOrUpdatedNode = this.neo4jClient.query(() -> this.renderer.render(statement))
			.bind(entityToBeSaved)
			.with(binderFunction)
			.bindAll(statement.getCatalog().getParameters())
			.fetchAs(Entity.class)
			.one();

		if (newOrUpdatedNode.isEmpty()) {
			if (entityMetaData.hasVersionProperty()) {
				throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
			}
			// defensive exception throwing
			throw new IllegalStateException("Could not retrieve an internal id while saving");
		}

		Object elementId = newOrUpdatedNode.map(node -> {
			if (!entityMetaData.isUsingDeprecatedInternalId()
					&& TemplateSupport.rendererRendersElementId(this.renderer)) {
				return IdentitySupport.getElementId(node);
			}
			@SuppressWarnings("deprecation")
			var id = node.id();
			return id;
		}).orElseThrow();

		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
		TemplateSupport.setGeneratedIdIfNecessary(entityMetaData, propertyAccessor, elementId, newOrUpdatedNode);
		TemplateSupport.updateVersionPropertyIfPossible(entityMetaData, propertyAccessor, newOrUpdatedNode.get());

		if (stateMachine == null) {
			stateMachine = new NestedRelationshipProcessingStateMachine(this.neo4jMappingContext, instance, elementId);
		}

		stateMachine.markEntityAsProcessed(instance, elementId);
		processRelations(entityMetaData, propertyAccessor, isEntityNew, stateMachine, binderFunction.filter);

		T bean = propertyAccessor.getBean();
		stateMachine.markAsAliased(instance, bean);
		return bean;
	}

	@SuppressWarnings("unchecked")
	private <T> DynamicLabels determineDynamicLabels(T entityToBeSaved, Neo4jPersistentEntity<?> entityMetaData) {
		return entityMetaData.getDynamicLabelsProperty().map(p -> {

			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
			Neo4jPersistentProperty idProperty = entityMetaData.getRequiredIdProperty();
			var statementReturningDynamicLabels = this.cypherGenerator
				.createStatementReturningDynamicLabels(entityMetaData);
			Neo4jClient.RunnableSpec runnableQuery = this.neo4jClient
				.query(() -> this.renderer.render(statementReturningDynamicLabels))
				.bind(TemplateSupport.convertIdValues(this.neo4jMappingContext, idProperty,
						propertyAccessor.getProperty(idProperty)))
				.to(Constants.NAME_OF_ID)
				.bind(entityMetaData.getStaticLabels())
				.to(Constants.NAME_OF_STATIC_LABELS_PARAM)
				.bindAll(statementReturningDynamicLabels.getCatalog().getParameters());

			if (entityMetaData.hasVersionProperty()) {
				runnableQuery = runnableQuery
					.bind((Long) propertyAccessor.getProperty(entityMetaData.getRequiredVersionProperty()))
					.to(Constants.NAME_OF_VERSION_PARAM);
			}

			Optional<Map<String, Object>> optionalResult = runnableQuery.fetch().one();
			return new DynamicLabels(entityMetaData,
					optionalResult.map(r -> (Collection<String>) r.get(Constants.NAME_OF_LABELS))
						.orElseGet(Collections::emptyList),
					(Collection<String>) propertyAccessor.getProperty(p));
		}).orElse(DynamicLabels.EMPTY);
	}

	@Override
	public <T> List<T> saveAll(Iterable<T> instances) {
		return execute(tx -> saveAllImpl(instances, Collections.emptySet(), null));
	}

	private boolean requiresSingleStatements(boolean heterogeneousCollection, Neo4jPersistentEntity<?> entityMetaData) {
		return heterogeneousCollection || entityMetaData.isUsingInternalIds() || entityMetaData.hasVersionProperty()
				|| entityMetaData.getDynamicLabelsProperty().isPresent();
	}

	private <T> List<T> saveAllImpl(Iterable<T> instances,
			@Nullable Collection<PropertyFilter.ProjectedPath> includedProperties,
			@Nullable BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		Set<Class<?>> types = new HashSet<>();
		List<T> entities = new ArrayList<>();
		Map<Class<?>, Collection<PropertyFilter.ProjectedPath>> includedPropertiesByClass = new HashMap<>();
		instances.forEach(instance -> {
			entities.add(instance);
			types.add(instance.getClass());
			includedPropertiesByClass.put(instance.getClass(), PropertyFilterSupport
				.getInputPropertiesForAggregateBoundary(instance.getClass(), this.neo4jMappingContext));
		});

		if (entities.isEmpty()) {
			return Collections.emptyList();
		}

		boolean heterogeneousCollection = types.size() > 1;
		Class<?> domainClass = types.iterator().next();

		Collection<PropertyFilter.ProjectedPath> pps = (includeProperty != null) ? TemplateSupport
			.computeIncludedPropertiesFromPredicate(this.neo4jMappingContext, domainClass, includeProperty)
				: Objects.requireNonNullElseGet(includedProperties, List::of);

		Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainClass);

		if (requiresSingleStatements(heterogeneousCollection, entityMetaData)) {
			log.debug("Saving entities using single statements.");

			NestedRelationshipProcessingStateMachine stateMachine = new NestedRelationshipProcessingStateMachine(
					this.neo4jMappingContext);
			return entities.stream()
				.map(e -> saveImpl(e,
						((includedProperties != null && !includedProperties.isEmpty()) || includeProperty != null) ? pps
								: includedPropertiesByClass.get(e.getClass()),
						stateMachine))
				.collect(Collectors.toList());
		}

		class Tuple3<T> {

			final T originalInstance;

			final boolean wasNew;

			final T modifiedInstance;

			Tuple3(T originalInstance, boolean wasNew, T modifiedInstance) {
				this.originalInstance = originalInstance;
				this.wasNew = wasNew;
				this.modifiedInstance = modifiedInstance;
			}

		}

		List<Tuple3<T>> entitiesToBeSaved = entities.stream()
			.map(e -> new Tuple3<>(e, entityMetaData.isNew(e), this.eventSupport.maybeCallBeforeBind(e)))
			.collect(Collectors.toList());

		// Save roots
		@SuppressWarnings("unchecked") // We can safely assume here that we have a
										// humongous collection with only one single type
										// being either T or extending it
		Function<T, Map<String, Object>> binderFunction = this.neo4jMappingContext
			.getRequiredBinderFunctionFor((Class<T>) domainClass);
		binderFunction = TemplateSupport.createAndApplyPropertyFilter(pps, entityMetaData, binderFunction);
		List<Map<String, Object>> entityList = entitiesToBeSaved.stream()
			.map(h -> h.modifiedInstance)
			.map(binderFunction)
			.collect(Collectors.toList());
		var statement = this.cypherGenerator.prepareSaveOfMultipleInstancesOf(entityMetaData);
		Map<Value, String> idToInternalIdMapping = this.neo4jClient.query(() -> this.renderer.render(statement))
			.bind(entityList)
			.to(Constants.NAME_OF_ENTITY_LIST_PARAM)
			.bindAll(statement.getCatalog().getParameters())
			.fetchAs(Map.Entry.class)
			.mappedBy((t, r) -> new AbstractMap.SimpleEntry<>(r.get(Constants.NAME_OF_ID),
					TemplateSupport.convertIdOrElementIdToString(r.get(Constants.NAME_OF_ELEMENT_ID))))
			.all()
			.stream()
			.collect(Collectors.toMap(m -> (Value) m.getKey(), m -> (String) m.getValue()));

		// Save related
		var stateMachine = new NestedRelationshipProcessingStateMachine(this.neo4jMappingContext, null, null);
		return entitiesToBeSaved.stream().map(t -> {
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(t.modifiedInstance);
			Neo4jPersistentProperty idProperty = entityMetaData.getRequiredIdProperty();
			Object id = TemplateSupport.convertIdValues(this.neo4jMappingContext, idProperty,
					propertyAccessor.getProperty(idProperty));
			String internalId = Objects.requireNonNull(idToInternalIdMapping.get(id));
			stateMachine.registerInitialObject(t.originalInstance, internalId);
			return this.<T>processRelations(entityMetaData, propertyAccessor, t.wasNew, stateMachine,
					TemplateSupport.computeIncludePropertyPredicate(
							((includedProperties != null && !includedProperties.isEmpty()) || includeProperty != null)
									? pps : includedPropertiesByClass.get(t.modifiedInstance.getClass()),
							entityMetaData));
		}).collect(Collectors.toList());
	}

	@Override
	public <T> List<T> saveAllAs(Iterable<T> instances,
			BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		return execute(tx -> saveAllImpl(instances, null, includeProperty));
	}

	@Override
	public <T, R> List<R> saveAllAs(Iterable<T> instances, Class<R> resultType) {

		Assert.notNull(resultType, "ResultType must not be null");

		return execute(tx -> {

			Class<?> commonElementType = TemplateSupport.findCommonElementType(instances);

			if (commonElementType == null) {
				throw new IllegalArgumentException(
						"Could not determine a common element of an heterogeneous collection");
			}

			if (commonElementType == TemplateSupport.EmptyIterable.class) {
				return Collections.emptyList();
			}

			if (resultType.isAssignableFrom(commonElementType)) {
				@SuppressWarnings("unchecked") // Nicer to live with this than streaming,
												// mapping and collecting to avoid the
												// cast. It's easier on the reactive side.
				List<R> saveElements = (List<R>) saveAll(instances);
				return saveElements;
			}

			ProjectionFactory localProjectionFactory = getProjectionFactory();
			ProjectionInformation projectionInformation = localProjectionFactory.getProjectionInformation(resultType);

			Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(commonElementType,
					resultType, localProjectionFactory, this.neo4jMappingContext);

			List<T> savedInstances = saveAllImpl(instances, pps, null);

			if (projectionInformation.isClosed()) {
				return savedInstances.stream()
					.map(instance -> localProjectionFactory.createProjection(resultType, instance))
					.collect(Collectors.toList());
			}

			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext
				.getRequiredPersistentEntity(commonElementType);
			Neo4jPersistentProperty idProperty = entityMetaData.getRequiredIdProperty();

			List<Object> ids = savedInstances.stream().map(savedInstance -> {
				PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
				return propertyAccessor.getProperty(idProperty);
			}).collect(Collectors.toList());

			return findAllById(ids, commonElementType).stream()
				.map(instance -> localProjectionFactory.createProjection(resultType, instance))
				.collect(Collectors.toList());
		});
	}

	@Override
	public <T> void deleteById(Object id, Class<T> domainType) {

		executeWithoutResult(tx -> {

			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);
			String nameOfParameter = "id";
			Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

			log.debug(() -> String.format("Deleting entity with id %s ", id));

			Statement statement = this.cypherGenerator.prepareDeleteOf(entityMetaData, condition);
			ResultSummary summary = this.neo4jClient.query(this.renderer.render(statement))
				.bind(TemplateSupport.convertIdValues(this.neo4jMappingContext, entityMetaData.getRequiredIdProperty(),
						id))
				.to(nameOfParameter)
				.bindAll(statement.getCatalog().getParameters())
				.run();

			log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
					summary.counters().relationshipsDeleted()));
		});
	}

	@Override
	public <T> void deleteByIdWithVersion(Object id, Class<T> domainType, Neo4jPersistentProperty versionProperty,
			@Nullable Object versionValue) {

		executeWithoutResult(tx -> {
			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);

			String nameOfParameter = "id";
			Condition condition = entityMetaData.getIdExpression()
				.isEqualTo(parameter(nameOfParameter))
				.and(Cypher
					.property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData),
							versionProperty.getPropertyName())
					.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))
					.or(Cypher
						.property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData),
								versionProperty.getPropertyName())
						.isNull()));

			Statement statement = this.cypherGenerator.prepareMatchOf(entityMetaData, condition)
				.returning(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData))
				.build();

			Map<String, Object> parameters = new HashMap<>();
			parameters.put(nameOfParameter, TemplateSupport.convertIdValues(this.neo4jMappingContext,
					entityMetaData.getRequiredIdProperty(), id));
			parameters.put(Constants.NAME_OF_VERSION_PARAM, versionValue);

			var lockedEntity = createExecutableQuery(domainType, null, statement, parameters, false).getSingleResult();
			if (lockedEntity.isEmpty()) {
				throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
			}

			deleteById(id, domainType);
		});
	}

	@Override
	public <T> void deleteAllById(Iterable<?> ids, Class<T> domainType) {

		executeWithoutResult(tx -> {

			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);
			String nameOfParameter = "ids";
			Condition condition = entityMetaData.getIdExpression().in(parameter(nameOfParameter));

			log.debug(() -> String.format("Deleting all entities with the following ids: %s ", ids));

			Statement statement = this.cypherGenerator.prepareDeleteOf(entityMetaData, condition);
			ResultSummary summary = this.neo4jClient.query(this.renderer.render(statement))
				.bind(TemplateSupport.convertIdValues(this.neo4jMappingContext, entityMetaData.getRequiredIdProperty(),
						ids))
				.to(nameOfParameter)
				.bindAll(statement.getCatalog().getParameters())
				.run();

			log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
					summary.counters().relationshipsDeleted()));
		});
	}

	@Override
	public void deleteAll(Class<?> domainType) {

		executeWithoutResult(tx -> {

			Neo4jPersistentEntity<?> entityMetaData = this.neo4jMappingContext.getRequiredPersistentEntity(domainType);
			log.debug(
					() -> String.format("Deleting all nodes with primary label %s", entityMetaData.getPrimaryLabel()));

			Statement statement = this.cypherGenerator.prepareDeleteOf(entityMetaData);
			ResultSummary summary = this.neo4jClient.query(this.renderer.render(statement)).run();

			log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
					summary.counters().relationshipsDeleted()));
		});
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Statement statement, boolean readOnly) {
		return createExecutableQuery(domainType, null, statement, Collections.emptyMap(), readOnly);
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, String cypherQuery, boolean readOnly) {
		return createExecutableQuery(domainType, null, cypherQuery, Collections.emptyMap(), readOnly);
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType,
			Statement statement, Map<String, Object> parameters, boolean readOnly) {

		return createExecutableQuery(domainType, resultType, this.renderer.render(statement),
				TemplateSupport.mergeParameters(statement, parameters), readOnly);
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType,
			String cypherStatement, Map<String, Object> parameters, boolean readOnly) {

		Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction = TemplateSupport
			.getAndDecorateMappingFunction(this.neo4jMappingContext, domainType, resultType);
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
			.withCypherQuery(cypherStatement)
			.withParameters(parameters)
			.usingMappingFunction(mappingFunction)
			.build();

		return toExecutableQuery(preparedQuery, readOnly);
	}

	/**
	 * Starts of processing of the relationships.
	 * @param neo4jPersistentEntity the description of the instance to save
	 * @param parentPropertyAccessor the property accessor of the parent, to modify the
	 * relationships
	 * @param isParentObjectNew a flag if the parent was new
	 * @param stateMachine initial state of entity processing
	 * @param includeProperty a predicate telling to include a relationship property or
	 * not
	 * @param <T> the type of the object being initially processed
	 * @return the owner of the relations being processed
	 */
	private <T> T processRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			PersistentPropertyAccessor<?> parentPropertyAccessor, boolean isParentObjectNew,
			NestedRelationshipProcessingStateMachine stateMachine, PropertyFilter includeProperty) {

		PropertyFilter.RelaxedPropertyPath startingPropertyPath = PropertyFilter.RelaxedPropertyPath
			.withRootType(neo4jPersistentEntity.getUnderlyingClass());
		return processNestedRelations(neo4jPersistentEntity, parentPropertyAccessor, isParentObjectNew, stateMachine,
				includeProperty, startingPropertyPath);
	}

	@SuppressWarnings("deprecation")
	private <T> T processNestedRelations(Neo4jPersistentEntity<?> sourceEntity,
			PersistentPropertyAccessor<?> propertyAccessor, boolean isParentObjectNew,
			NestedRelationshipProcessingStateMachine stateMachine, PropertyFilter includeProperty,
			PropertyFilter.RelaxedPropertyPath previousPath) {

		Object fromId = propertyAccessor.getProperty(sourceEntity.getRequiredIdProperty());

		AssociationHandlerSupport.of(sourceEntity).doWithAssociations(association -> {

			// create context to bundle parameters
			NestedRelationshipContext relationshipContext = NestedRelationshipContext.of(association, propertyAccessor,
					sourceEntity);
			if (relationshipContext.isReadOnly()) {
				return;
			}

			Object rawValue = relationshipContext.getValue();
			Collection<?> relatedValuesToStore = MappingSupport.unifyRelationshipValue(relationshipContext.getInverse(),
					rawValue);

			RelationshipDescription relationshipDescription = relationshipContext.getRelationship();

			PropertyFilter.RelaxedPropertyPath currentPropertyPath = previousPath
				.append(relationshipDescription.getFieldName());

			if (!includeProperty.isNotFiltering() && !includeProperty.contains(currentPropertyPath)) {
				return;
			}
			Neo4jPersistentProperty idProperty;
			if (!relationshipDescription.hasInternalIdProperty()) {
				idProperty = null;
			}
			else {
				Neo4jPersistentEntity<?> relationshipPropertiesEntity = (Neo4jPersistentEntity<?>) relationshipDescription
					.getRelationshipPropertiesEntity();
				idProperty = (relationshipPropertiesEntity != null) ? relationshipPropertiesEntity.getIdProperty()
						: null;
			}

			// break recursive procession and deletion of previously created relationships
			ProcessState processState = stateMachine.getStateOf(fromId, relationshipDescription, relatedValuesToStore);
			if (processState == ProcessState.PROCESSED_ALL_RELATIONSHIPS
					|| processState == ProcessState.PROCESSED_BOTH) {
				return;
			}

			// Remove all relationships before creating all new if the entity is not new
			// and the relationship
			// has not been processed before.
			// This avoids the usage of cache but might have significant impact on overall
			// performance
			boolean canUseElementId = TemplateSupport.rendererRendersElementId(this.renderer);
			if (!isParentObjectNew && !stateMachine.hasProcessedRelationship(fromId, relationshipDescription)) {

				List<Object> knownRelationshipsIds = new ArrayList<>();
				if (idProperty != null) {
					for (Object relatedValueToStore : relatedValuesToStore) {
						if (relatedValueToStore == null) {
							continue;
						}

						PersistentPropertyAccessor<?> relationshipPropertiesPropertyAccessor = relationshipContext
							.getRelationshipPropertiesPropertyAccessor(relatedValueToStore);
						if (relationshipPropertiesPropertyAccessor == null) {
							continue;
						}
						Object id = relationshipPropertiesPropertyAccessor.getProperty(idProperty);
						if (id != null) {
							knownRelationshipsIds.add(id);
						}
					}
				}

				Statement relationshipRemoveQuery = this.cypherGenerator.prepareDeleteOf(sourceEntity,
						relationshipDescription, canUseElementId);

				this.neo4jClient.query(this.renderer.render(relationshipRemoveQuery))
					.bind(TemplateSupport.convertIdValues(this.neo4jMappingContext, sourceEntity.getIdProperty(),
							fromId)) //
					.to(Constants.FROM_ID_PARAMETER_NAME) //
					.bind(knownRelationshipsIds) //
					.to(Constants.NAME_OF_KNOWN_RELATIONSHIPS_PARAM) //
					.bindAll(relationshipRemoveQuery.getCatalog().getParameters())
					.run();
			}

			// nothing to do because there is nothing to map
			if (relationshipContext.inverseValueIsEmpty()) {
				return;
			}

			stateMachine.markRelationshipAsProcessed(fromId, relationshipDescription);

			Neo4jPersistentProperty relationshipProperty = association.getInverse();

			RelationshipHandler relationshipHandler = RelationshipHandler.forProperty(relationshipProperty, rawValue);
			List<Object> plainRelationshipRows = new ArrayList<>();
			List<Map<String, Object>> relationshipPropertiesRows = new ArrayList<>();
			List<Map<String, Object>> newRelationshipPropertiesRows = new ArrayList<>();
			List<Object> updateRelatedValuesToStore = new ArrayList<>();
			List<Object> newRelationshipPropertiesToStore = new ArrayList<>();

			for (Object relatedValueToStore : relatedValuesToStore) {

				// here a map entry is not always anymore a dynamic association
				Object relatedObjectBeforeCallbacksApplied = relationshipContext
					.identifyAndExtractRelationshipTargetNode(relatedValueToStore);
				Neo4jPersistentEntity<?> targetEntity = this.neo4jMappingContext
					.getRequiredPersistentEntity(relatedObjectBeforeCallbacksApplied.getClass());
				var isNewEntity = targetEntity.isNew(relatedObjectBeforeCallbacksApplied);
				var skipUpdateOfEntity = !isNewEntity;
				if (relatedValueToStore instanceof MappingSupport.RelationshipPropertiesWithEntityHolder rpweh) {
					var relatedEntity = rpweh.getRelatedEntity();
					skipUpdateOfEntity &= this.userDefinedChangeEvaluators.containsKey(relatedEntity.getClass())
							&& !this.userDefinedChangeEvaluators.get(relatedEntity.getClass()).needsUpdate(relatedEntity);
				}
				else {
					skipUpdateOfEntity &= this.userDefinedChangeEvaluators.containsKey(relatedValueToStore.getClass())
							&& !this.userDefinedChangeEvaluators.get(relatedValueToStore.getClass()).needsUpdate(relatedValueToStore);
				}
				Object newRelatedObject = stateMachine.hasProcessedValue(relatedObjectBeforeCallbacksApplied)
						? stateMachine.getProcessedAs(relatedObjectBeforeCallbacksApplied)
						: skipUpdateOfEntity
							? relatedObjectBeforeCallbacksApplied
							: this.eventSupport.maybeCallBeforeBind(relatedObjectBeforeCallbacksApplied);

				Object relatedInternalId;
				Entity savedEntity = null;
				// No need to save values if processed
				if (stateMachine.hasProcessedValue(relatedValueToStore)) {
					relatedInternalId = stateMachine.getObjectId(relatedValueToStore);
				}
				else {
					if ((isNewEntity || relationshipDescription.cascadeUpdates()) && !skipUpdateOfEntity) {
						savedEntity = saveRelatedNode(newRelatedObject, targetEntity, includeProperty,
								currentPropertyPath);
					}
					else {
						var targetPropertyAccessor = targetEntity.getPropertyAccessor(newRelatedObject);
						var requiredIdProperty = targetEntity.getRequiredIdProperty();
						savedEntity = loadRelatedNode(targetEntity,
								targetPropertyAccessor.getProperty(requiredIdProperty));
					}
					relatedInternalId = TemplateSupport.rendererCanUseElementIdIfPresent(this.renderer, targetEntity)
							? savedEntity.elementId() : savedEntity.id();
					stateMachine.markEntityAsProcessed(relatedValueToStore, relatedInternalId);
					if (relatedValueToStore instanceof MappingSupport.RelationshipPropertiesWithEntityHolder) {
						Object entity = ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValueToStore)
							.getRelatedEntity();
						stateMachine.markAsAliased(entity, relatedInternalId);
					}
				}

				Neo4jPersistentProperty requiredIdProperty = targetEntity.getRequiredIdProperty();
				PersistentPropertyAccessor<?> targetPropertyAccessor = targetEntity
					.getPropertyAccessor(newRelatedObject);
				Object possibleInternalLongId = targetPropertyAccessor.getProperty(requiredIdProperty);
				relatedInternalId = TemplateSupport.retrieveOrSetRelatedId(targetEntity, targetPropertyAccessor,
						Optional.ofNullable(savedEntity), relatedInternalId);
				if (savedEntity != null) {
					TemplateSupport.updateVersionPropertyIfPossible(targetEntity, targetPropertyAccessor, savedEntity);
				}
				stateMachine.markAsAliased(relatedObjectBeforeCallbacksApplied, targetPropertyAccessor.getBean());
				stateMachine.markRelationshipAsProcessed(
						(possibleInternalLongId != null) ? possibleInternalLongId : relatedInternalId,
						relationshipDescription.getRelationshipObverse());

				Object idValue;
				PersistentPropertyAccessor<?> relationshipPropertiesPropertyAccessor = relationshipContext
					.getRelationshipPropertiesPropertyAccessor(relatedValueToStore);
				if (idProperty == null || relationshipPropertiesPropertyAccessor == null) {
					idValue = null;
				}
				else {
					idValue = relationshipPropertiesPropertyAccessor.getProperty(idProperty);
				}

				Map<String, Object> properties = new HashMap<>();
				properties.put(Constants.FROM_ID_PARAMETER_NAME, TemplateSupport
					.convertIdValues(this.neo4jMappingContext, sourceEntity.getRequiredIdProperty(), fromId));
				properties.put(Constants.TO_ID_PARAMETER_NAME, relatedInternalId);
				properties.put(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM, idValue);
				boolean isNewRelationship = idValue == null;
				if (relationshipDescription.isDynamic()) {
					// create new dynamic relationship properties
					if (relationshipDescription.hasRelationshipProperties() && isNewRelationship
							&& idProperty != null) {
						CreateRelationshipStatementHolder statementHolder = this.neo4jMappingContext
							.createStatementForSingleRelationship(sourceEntity, relationshipDescription,
									relatedValueToStore, true, canUseElementId);

						List<Object> row = Collections.singletonList(properties);
						statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, row);

						Optional<Object> relationshipInternalId = this.neo4jClient
							.query(this.renderer.render(statementHolder.getStatement()))
							.bind(TemplateSupport.convertIdValues(this.neo4jMappingContext,
									sourceEntity.getRequiredIdProperty(), fromId)) //
							.to(Constants.FROM_ID_PARAMETER_NAME) //
							.bind(relatedInternalId) //
							.to(Constants.TO_ID_PARAMETER_NAME) //
							.bind(idValue) //
							.to(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM) //
							.bindAll(statementHolder.getProperties())
							.fetchAs(Object.class)
							.mappedBy((t, r) -> IdentitySupport.mapperForRelatedIdValues(idProperty).apply(r))
							.one();

						assignIdToRelationshipProperties(relationshipContext, relatedValueToStore, idProperty,
								relationshipInternalId.orElseThrow());
					}
					else { // plain (new or to update) dynamic relationship or dynamic
							// relationships with properties to update

						CreateRelationshipStatementHolder statementHolder = this.neo4jMappingContext
							.createStatementForSingleRelationship(sourceEntity, relationshipDescription,
									relatedValueToStore, false, canUseElementId);

						List<Object> row = Collections.singletonList(properties);
						statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, row);
						this.neo4jClient.query(this.renderer.render(statementHolder.getStatement()))
							.bind(TemplateSupport.convertIdValues(this.neo4jMappingContext,
									sourceEntity.getRequiredIdProperty(), fromId)) //
							.to(Constants.FROM_ID_PARAMETER_NAME) //
							.bind(relatedInternalId) //
							.to(Constants.TO_ID_PARAMETER_NAME) //
							.bind(idValue)
							.to(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM) //
							.bindAll(statementHolder.getProperties())
							.run();
					}
				}
				else if (relationshipDescription.hasRelationshipProperties() && fromId != null) {
					// check if bidi mapped already
					var hlp = ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValueToStore);
					var hasProcessedRelationshipEntity = stateMachine.hasProcessedRelationshipEntity(
							propertyAccessor.getBean(), hlp.getRelatedEntity(), relationshipContext.getRelationship());
					if (hasProcessedRelationshipEntity) {
						stateMachine.requireIdUpdate(sourceEntity, relationshipDescription, canUseElementId, fromId,
								relatedInternalId, relationshipContext, relatedValueToStore, idProperty);
					}
					else {
						if (isNewRelationship && idProperty != null) {
							newRelationshipPropertiesRows.add(properties);
							if (!skipUpdateOfEntity) {
								newRelationshipPropertiesToStore.add(relatedValueToStore);
							}
						}
						else {
							this.neo4jMappingContext.getEntityConverter()
								.write(hlp.getRelationshipProperties(), properties);
							relationshipPropertiesRows.add(properties);
						}
						stateMachine.storeProcessRelationshipEntity(hlp, propertyAccessor.getBean(),
								hlp.getRelatedEntity(), relationshipContext.getRelationship());
					}
				}
				else {
					// non-dynamic relationship or relationship with properties
					plainRelationshipRows.add(properties);
				}

				if (processState != ProcessState.PROCESSED_ALL_VALUES) {
					processNestedRelations(targetEntity, targetPropertyAccessor, isNewEntity, stateMachine,
							includeProperty, currentPropertyPath);
				}

				Object potentiallyRecreatedNewRelatedObject = MappingSupport
					.getRelationshipOrRelationshipPropertiesObject(this.neo4jMappingContext,
							relationshipDescription.hasRelationshipProperties(),
							relationshipProperty.isDynamicAssociation(), relatedValueToStore, targetPropertyAccessor);
				relationshipHandler.handle(relatedValueToStore, relatedObjectBeforeCallbacksApplied,
						potentiallyRecreatedNewRelatedObject);
			}
			// batch operations
			if (!(relationshipDescription.hasRelationshipProperties() || relationshipDescription.isDynamic()
					|| plainRelationshipRows.isEmpty())) {
				CreateRelationshipStatementHolder statementHolder = this.neo4jMappingContext
					.createStatementForImperativeSimpleRelationshipBatch(sourceEntity, relationshipDescription,
							plainRelationshipRows, canUseElementId);
				statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM,
						plainRelationshipRows);
				this.neo4jClient.query(this.renderer.render(statementHolder.getStatement()))
					.bindAll(statementHolder.getProperties())
					.bindAll(statementHolder.getStatement().getCatalog().getParameters())
					.run();
			}
			else if (relationshipDescription.hasRelationshipProperties()) {
				if (!relationshipPropertiesRows.isEmpty()) {
					CreateRelationshipStatementHolder statementHolder = this.neo4jMappingContext
						.createStatementForImperativeRelationshipsWithPropertiesBatch(false, sourceEntity,
								relationshipDescription, updateRelatedValuesToStore, relationshipPropertiesRows,
								canUseElementId);
					statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM,
							relationshipPropertiesRows);

					this.neo4jClient.query(this.renderer.render(statementHolder.getStatement()))
						.bindAll(statementHolder.getProperties())
						.bindAll(statementHolder.getStatement().getCatalog().getParameters())
						.run();
				}
				if (!(newRelationshipPropertiesToStore.isEmpty() || idProperty == null)) {
					CreateRelationshipStatementHolder statementHolder = this.neo4jMappingContext
						.createStatementForImperativeRelationshipsWithPropertiesBatch(true, sourceEntity,
								relationshipDescription, newRelationshipPropertiesToStore,
								newRelationshipPropertiesRows, canUseElementId);
					List<Object> all = new ArrayList<>(
							this.neo4jClient.query(this.renderer.render(statementHolder.getStatement()))
								.bindAll(statementHolder.getProperties())
								.bindAll(statementHolder.getStatement().getCatalog().getParameters())
								.fetchAs(Object.class)
								.mappedBy((t, r) -> IdentitySupport.mapperForRelatedIdValues(idProperty).apply(r))
								.all());
					// assign new ids
					for (int i = 0; i < all.size(); i++) {
						Object anId = all.get(i);
						assignIdToRelationshipProperties(relationshipContext, newRelationshipPropertiesToStore.get(i),
								idProperty, anId);
					}
				}
			}

			// Possible grab missing relationship ids now for bidirectional ones, with
			// properties, mapped in opposite directions
			stateMachine.updateRelationshipIds(this::getRelationshipId);

			relationshipHandler.applyFinalResultToOwner(propertyAccessor);
		});

		@SuppressWarnings("unchecked")
		T finalSubgraphRoot = (T) propertyAccessor.getBean();
		return finalSubgraphRoot;
	}

	private Optional<Object> getRelationshipId(Statement statement, @Nullable Neo4jPersistentProperty idProperty,
			Object fromId, Object toId) {

		return this.neo4jClient.query(this.renderer.render(statement))
			.bind(TemplateSupport.convertIdValues(this.neo4jMappingContext, idProperty, fromId)) //
			.to(Constants.FROM_ID_PARAMETER_NAME) //
			.bind(toId) //
			.to(Constants.TO_ID_PARAMETER_NAME) //
			.bindAll(statement.getCatalog().getParameters())
			.fetchAs(Object.class)
			.mappedBy((t, r) -> IdentitySupport.mapperForRelatedIdValues(idProperty).apply(r))
			.one();
	}

	// The pendant to {@link #saveRelatedNode(Object, NodeDescription, PropertyFilter,
	// PropertyFilter.RelaxedPropertyPath)}
	// We can't do without a query, as we need to refresh the internal id
	private Entity loadRelatedNode(NodeDescription<?> targetNodeDescription, @Nullable Object relatedInternalId) {

		var targetPersistentEntity = (Neo4jPersistentEntity<?>) targetNodeDescription;
		var queryFragmentsAndParameters = QueryFragmentsAndParameters
			.forFindById(targetPersistentEntity,
					TemplateSupport.convertIdValues(this.neo4jMappingContext,
							targetPersistentEntity.getRequiredIdProperty(), relatedInternalId),
					this.neo4jMappingContext);
		var nodeName = Constants.NAME_OF_TYPED_ROOT_NODE.apply(targetNodeDescription).getValue();

		return this.neo4jClient
			.query(() -> this.renderer
				.render(this.cypherGenerator
					.prepareFindOf(targetNodeDescription, queryFragmentsAndParameters.getQueryFragments().getMatchOn(),
							queryFragmentsAndParameters.getQueryFragments().getCondition())
					.returning(nodeName)
					.build()))
			.bindAll(queryFragmentsAndParameters.getParameters())
			.fetchAs(Entity.class)
			.mappedBy((t, r) -> r.get(nodeName).asNode())
			.one()
			.orElseThrow();
	}

	private void assignIdToRelationshipProperties(NestedRelationshipContext relationshipContext,
			Object relatedValueToStore, Neo4jPersistentProperty idProperty, Object relationshipInternalId) {
		PersistentPropertyAccessor<?> relationshipPropertiesPropertyAccessor = relationshipContext
			.getRelationshipPropertiesPropertyAccessor(relatedValueToStore);
		if (relationshipPropertiesPropertyAccessor != null) {
			relationshipPropertiesPropertyAccessor.setProperty(idProperty, relationshipInternalId);
		}
	}

	private Entity saveRelatedNode(Object entity, NodeDescription<?> targetNodeDescription,
			PropertyFilter includeProperty, PropertyFilter.RelaxedPropertyPath currentPropertyPath) {

		Neo4jPersistentEntity<?> targetPersistentEntity = (Neo4jPersistentEntity<?>) targetNodeDescription;
		DynamicLabels dynamicLabels = determineDynamicLabels(entity, targetPersistentEntity);
		@SuppressWarnings("rawtypes")
		Class entityType = targetPersistentEntity.getType();
		@SuppressWarnings("unchecked")
		Function<Object, Map<String, Object>> binderFunction = this.neo4jMappingContext
			.getRequiredBinderFunctionFor(entityType);
		binderFunction = binderFunction.andThen(tree -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) tree.get(Constants.NAME_OF_PROPERTIES_PARAM);
			String idPropertyName = targetPersistentEntity.getRequiredIdProperty().getPropertyName();
			IdDescription idDescription = targetPersistentEntity.getIdDescription();
			boolean assignedId = idDescription != null
					&& (idDescription.isAssignedId() || idDescription.isExternallyGeneratedId());
			if (properties != null && !includeProperty.isNotFiltering()) {
				properties.entrySet().removeIf(e -> {
					// we cannot skip the id property if it is an assigned id
					boolean isIdProperty = e.getKey().equals(idPropertyName);
					return !(assignedId && isIdProperty)
							&& !includeProperty.contains(currentPropertyPath.append(e.getKey()));
				});
			}
			return tree;
		});
		var statement = this.cypherGenerator.prepareSaveOf(targetNodeDescription, dynamicLabels,
				TemplateSupport.rendererRendersElementId(this.renderer));
		Optional<Entity> optionalSavedNode = this.neo4jClient.query(() -> this.renderer.render(statement))
			.bind(entity)
			.with(binderFunction)
			.bindAll(statement.getCatalog().getParameters())
			.fetchAs(Entity.class)
			.one();

		if (targetPersistentEntity.hasVersionProperty() && !optionalSavedNode.isPresent()) {
			throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
		}

		// It is checked above, god dammit.
		// noinspection OptionalGetWithoutIsPresent
		return optionalSavedNode.get();
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		// noinspection ConstantValue
		this.beanClassLoader = (beanClassLoader != null) ? beanClassLoader
				: org.springframework.util.ClassUtils.getDefaultClassLoader();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		this.eventSupport = EventSupport.discoverCallbacks(this.neo4jMappingContext, beanFactory);

		SpelAwareProxyProjectionFactory spelAwareProxyProjectionFactory = new SpelAwareProxyProjectionFactory();
		spelAwareProxyProjectionFactory.setBeanClassLoader(Objects.requireNonNull(this.beanClassLoader));
		spelAwareProxyProjectionFactory.setBeanFactory(beanFactory);
		this.projectionFactory = spelAwareProxyProjectionFactory;

		Configuration cypherDslConfiguration = beanFactory.getBeanProvider(Configuration.class)
			.getIfAvailable(Configuration::defaultConfig);
		this.renderer = Renderer.getRenderer(cypherDslConfiguration);
		this.elementIdOrIdFunction = SpringDataCypherDsl.elementIdOrIdFunction
			.apply(cypherDslConfiguration.getDialect());
		this.cypherGenerator.setElementIdOrIdFunction(this.elementIdOrIdFunction);

		if (this.transactionTemplate != null && this.transactionTemplateReadOnly != null) {
			return;
		}

		PlatformTransactionManager transactionManager = null;
		var it = beanFactory.getBeanProvider(PlatformTransactionManager.class).stream().iterator();
		while (it.hasNext()) {
			PlatformTransactionManager transactionManagerCandidate = it.next();
			if (transactionManagerCandidate instanceof Neo4jTransactionManager neo4jTransactionManager) {
				if (transactionManager != null) {
					throw new IllegalStateException("Multiple Neo4jTransactionManagers are defined in this context. "
							+ "If this in intended, please pass the transaction manager to use with this Neo4jTemplate in the constructor");
				}
				transactionManager = neo4jTransactionManager;
			}
		}
		setTransactionManager(transactionManager);
		this.userDefinedChangeEvaluators.putAll(beanFactory.getBeanProvider(UserDefinedChangeEvaluator.class).stream()
				.collect(Collectors.toMap(e -> e.getEvaluatingClass(), e -> e)));
	}

	// only used for the CDI configuration
	public void setCypherRenderer(Renderer rendererFromCdiConfiguration) {
		this.renderer = rendererFromCdiConfiguration;
	}

	public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
		if (transactionManager == null) {
			return;
		}
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.transactionTemplateReadOnly = new TransactionTemplate(transactionManager, readOnlyTransactionDefinition);
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(Class<T> domainType,
			QueryFragmentsAndParameters queryFragmentsAndParameters) {

		return createExecutableQuery(domainType, null, queryFragmentsAndParameters, false);
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType,
			QueryFragmentsAndParameters queryFragmentsAndParameters, boolean readOnlyTransaction) {

		Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction = TemplateSupport
			.getAndDecorateMappingFunction(this.neo4jMappingContext, domainType, resultType);
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
			.withQueryFragmentsAndParameters(queryFragmentsAndParameters)
			.usingMappingFunction(mappingFunction)
			.build();
		return toExecutableQuery(preparedQuery, readOnlyTransaction);
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {
		return toExecutableQuery(preparedQuery, false);
	}

	private <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery, boolean readOnly) {
		return new DefaultExecutableQuery<>(preparedQuery, readOnly);
	}

	@Override
	public <T> ExecutableSave<T> save(Class<T> domainType) {

		return new FluentOperationSupport(this).save(domainType);
	}

	<T, R> List<R> doSave(Iterable<R> instances, Class<T> domainType) {
		if (!instances.iterator().hasNext()) {
			return Collections.emptyList();
		}

		Class<?> resultType = Objects.requireNonNull(TemplateSupport.findCommonElementType(instances),
				() -> "Could not find a common type element to store and then project multiple instances of type %s"
					.formatted(domainType));

		return execute(tx -> {
			Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(domainType,
					resultType, getProjectionFactory(), this.neo4jMappingContext);

			NestedRelationshipProcessingStateMachine stateMachine = new NestedRelationshipProcessingStateMachine(
					this.neo4jMappingContext);
			List<R> results = new ArrayList<>();
			EntityFromDtoInstantiatingConverter<T> converter = new EntityFromDtoInstantiatingConverter<>(domainType,
					this.neo4jMappingContext);
			for (R instance : instances) {
				T domainObject = converter.convert(instance);
				if (domainObject == null) {
					continue;
				}
				T savedEntity = saveImpl(domainObject, pps, stateMachine);

				@SuppressWarnings("unchecked")
				R convertedBack = (R) new DtoInstantiatingConverter(resultType, this.neo4jMappingContext)
					.convertDirectly(savedEntity);
				results.add(convertedBack);
			}
			return results;
		});
	}

	String render(Statement statement) {
		return this.renderer.render(statement);
	}

	final class DefaultExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;

		private final TransactionTemplate txTemplate;

		DefaultExecutableQuery(PreparedQuery<T> preparedQuery, boolean readOnly) {
			this.preparedQuery = preparedQuery;
			// At this time, both must be initialized
			this.txTemplate = Objects.requireNonNull(
					readOnly ? Neo4jTemplate.this.transactionTemplateReadOnly : Neo4jTemplate.this.transactionTemplate);
		}

		@Override
		@SuppressWarnings({ "unchecked", "NullAway" })
		public List<T> getResults() {
			return this.txTemplate.execute(tx -> {
				Collection<T> all = createFetchSpec().map(Neo4jClient.RecordFetchSpec::all)
					.orElse(Collections.emptyList());
				if (this.preparedQuery.resultsHaveBeenAggregated()) {
					return all.stream()
						.flatMap(nested -> ((Collection<T>) nested).stream())
						.distinct()
						.collect(Collectors.toList());
				}
				return new ArrayList<>(all);
			});
		}

		@Override
		@SuppressWarnings({ "unchecked", "NullAway" })
		public Optional<T> getSingleResult() {
			return this.txTemplate.execute(tx -> {
				try {
					Optional<T> one = createFetchSpec().flatMap(Neo4jClient.RecordFetchSpec::one);
					if (this.preparedQuery.resultsHaveBeenAggregated()) {
						return one.map(aggregatedResults -> ((LinkedHashSet<T>) aggregatedResults).iterator().next());
					}
					return one;
				}
				catch (NoSuchRecordException ex) {
					// This exception is thrown by the driver in both cases when there are
					// 0 or 1+n records
					// So there has been an incorrect result size, but not too few results
					// but too many.
					throw new IncorrectResultSizeDataAccessException(ex.getMessage(), 1);
				}
			});
		}

		@Override
		@SuppressWarnings({ "unchecked", "NullAway" })
		public T getRequiredSingleResult() {
			return this.txTemplate.execute(tx -> {
				Optional<T> one = createFetchSpec().flatMap(Neo4jClient.RecordFetchSpec::one);
				if (this.preparedQuery.resultsHaveBeenAggregated()) {
					one = one.map(aggregatedResults -> ((LinkedHashSet<T>) aggregatedResults).iterator().next());
				}
				return one.orElseThrow(() -> new NoResultException(1,
						this.preparedQuery.getQueryFragmentsAndParameters().getCypherQuery()));
			});
		}

		private Optional<Neo4jClient.RecordFetchSpec<T>> createFetchSpec() {
			QueryFragmentsAndParameters queryFragmentsAndParameters = this.preparedQuery
				.getQueryFragmentsAndParameters();
			String cypherQuery = queryFragmentsAndParameters.getCypherQuery();
			Map<String, Object> finalParameters = queryFragmentsAndParameters.getParameters();

			QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();
			Neo4jPersistentEntity<?> entityMetaData = (Neo4jPersistentEntity<?>) queryFragmentsAndParameters
				.getNodeDescription();

			boolean containsPossibleCircles = entityMetaData != null
					&& entityMetaData.containsPossibleCircles(queryFragments::includeField);
			if (cypherQuery == null || containsPossibleCircles) {
				Statement statement;
				// The null check for the metadata is superfluous, but the easiest way to
				// make NullAway happy
				if (entityMetaData != null && containsPossibleCircles && !queryFragments.isScalarValueReturn()) {
					NodesAndRelationshipsByIdStatementProvider nodesAndRelationshipsById = createNodesAndRelationshipsByIdStatementProvider(
							entityMetaData, queryFragments, queryFragmentsAndParameters.getParameters());

					if (!nodesAndRelationshipsById.hasRootNodeIds()) {
						return Optional.empty();
					}
					statement = nodesAndRelationshipsById.toStatement(entityMetaData);
				}
				else {
					statement = queryFragmentsAndParameters.toStatement();
				}
				cypherQuery = Neo4jTemplate.this.renderer.render(statement);
				finalParameters = TemplateSupport.mergeParameters(statement, finalParameters);
			}

			Neo4jClient.MappingSpec<T> newMappingSpec = Neo4jTemplate.this.neo4jClient
				.query(Objects.requireNonNull(cypherQuery, "Could not compute a query"))
				.bindAll(finalParameters)
				.fetchAs(this.preparedQuery.getResultType());
			return this.preparedQuery.getOptionalMappingFunction()
				.map(newMappingSpec::mappedBy)
				.or(() -> Optional.of(newMappingSpec));
		}

		private NodesAndRelationshipsByIdStatementProvider createNodesAndRelationshipsByIdStatementProvider(
				Neo4jPersistentEntity<?> entityMetaData, QueryFragments queryFragments,
				Map<String, Object> parameters) {

			// first check if the root node(s) exist(s) at all
			Statement rootNodesStatement = Neo4jTemplate.this.cypherGenerator
				.prepareMatchOf(entityMetaData, queryFragments.getMatchOn(), queryFragments.getCondition())
				.returning(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE)
				.build();

			Map<String, Object> usedParameters = new HashMap<>(parameters);
			usedParameters.putAll(rootNodesStatement.getCatalog().getParameters());

			final Collection<String> rootNodeIds = new HashSet<>(
					Neo4jTemplate.this.neo4jClient.query(Neo4jTemplate.this.renderer.render(rootNodesStatement))
						.bindAll(usedParameters)
						.fetchAs(Value.class)
						.mappedBy((t, r) -> r.get(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE))
						.one()
						.map(value -> value.asList(TemplateSupport::convertIdOrElementIdToString))
						.orElseThrow());

			if (rootNodeIds.isEmpty()) {
				// fast return if no matching root node(s) are found
				return NodesAndRelationshipsByIdStatementProvider.EMPTY;
			}
			// load first level relationships
			final Map<String, Set<String>> relationshipsToRelatedNodeIds = new HashMap<>();

			for (RelationshipDescription relationshipDescription : entityMetaData
				.getRelationshipsInHierarchy(queryFragments::includeField)) {

				Statement statement = Neo4jTemplate.this.cypherGenerator
					.prepareMatchOf(entityMetaData, relationshipDescription, queryFragments.getMatchOn(),
							queryFragments.getCondition())
					.returning(Neo4jTemplate.this.cypherGenerator.createReturnStatementForMatch(entityMetaData))
					.build();

				usedParameters = new HashMap<>(parameters);
				usedParameters.putAll(statement.getCatalog().getParameters());
				Neo4jTemplate.this.neo4jClient.query(Neo4jTemplate.this.renderer.render(statement))
					.bindAll(usedParameters)
					.fetch()
					.one()
					.ifPresent(iterateAndMapNextLevel(relationshipsToRelatedNodeIds, relationshipDescription,
							PropertyPathWalkStep.empty()));
			}

			return new NodesAndRelationshipsByIdStatementProvider(rootNodeIds, relationshipsToRelatedNodeIds.keySet(),
					relationshipsToRelatedNodeIds.values().stream().flatMap(Collection::stream).toList(),
					queryFragments, Neo4jTemplate.this.elementIdOrIdFunction);
		}

		private void iterateNextLevel(Collection<String> nodeIds, RelationshipDescription sourceRelationshipDescription,
				Map<String, Set<String>> relationshipsToRelatedNodes, PropertyPathWalkStep currentPathStep) {

			Neo4jPersistentEntity<?> target = (Neo4jPersistentEntity<?>) sourceRelationshipDescription.getTarget();

			@SuppressWarnings("unchecked")
			String fieldName = ((Association<@NonNull Neo4jPersistentProperty>) sourceRelationshipDescription)
				.getInverse()
				.getFieldName();
			PropertyPathWalkStep nextPathStep;
			Neo4jPersistentEntity<?> relationshipPropertiesEntity = (Neo4jPersistentEntity<?>) sourceRelationshipDescription
				.getRelationshipPropertiesEntity();
			if (sourceRelationshipDescription.hasRelationshipProperties() && relationshipPropertiesEntity != null) {
				var targetNodeProperty = Objects.requireNonNull(
						relationshipPropertiesEntity.getPersistentProperty(TargetNode.class),
						() -> "Could not get target node property on %s"
							.formatted(relationshipPropertiesEntity.getType()));
				nextPathStep = currentPathStep.with(fieldName + "." + targetNodeProperty.getFieldName());
			}
			else {
				nextPathStep = currentPathStep.with(fieldName);
			}

			Collection<RelationshipDescription> relationships = target
				.getRelationshipsInHierarchy(relaxedPropertyPath -> {

					PropertyFilter.RelaxedPropertyPath prepend = relaxedPropertyPath.prepend(nextPathStep.path);
					prepend = PropertyFilter.RelaxedPropertyPath.withRootType(this.preparedQuery.getResultType())
						.append(prepend.toDotPath());
					return this.preparedQuery.getQueryFragmentsAndParameters()
						.getQueryFragments()
						.includeField(prepend);
				});

			for (RelationshipDescription relationshipDescription : relationships) {

				Node node = anyNode(Constants.NAME_OF_TYPED_ROOT_NODE.apply(target));

				Statement statement = Neo4jTemplate.this.cypherGenerator
					.prepareMatchOf(target, relationshipDescription, null,
							Neo4jTemplate.this.elementIdOrIdFunction.apply(node)
								.in(Cypher.parameter(Constants.NAME_OF_IDS)))
					.returning(Neo4jTemplate.this.cypherGenerator.createGenericReturnStatement())
					.build();

				Neo4jTemplate.this.neo4jClient.query(Neo4jTemplate.this.renderer.render(statement))
					.bindAll(Collections.singletonMap(Constants.NAME_OF_IDS,
							TemplateSupport.convertToLongIdOrStringElementId(nodeIds)))
					.bindAll(statement.getCatalog().getParameters())
					.fetch()
					.one()
					.ifPresent(
							iterateAndMapNextLevel(relationshipsToRelatedNodes, relationshipDescription, nextPathStep));
			}
		}

		private Consumer<Map<String, Object>> iterateAndMapNextLevel(
				Map<String, Set<String>> relationshipsToRelatedNodes, RelationshipDescription relationshipDescription,
				PropertyPathWalkStep currentPathStep) {

			return record -> {

				Map<String, Set<String>> relatedNodesVisited = new HashMap<>(relationshipsToRelatedNodes);
				@SuppressWarnings("unchecked")
				var sr = (List<Object>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATIONS);
				List<String> newRelationshipIds = (sr != null)
						? sr.stream().map(TemplateSupport::convertIdOrElementIdToString).toList() : List.of();
				@SuppressWarnings("unchecked")
				var srn = (List<Object>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES);
				Set<String> relatedIds = (srn != null)
						? new HashSet<>(srn.stream().map(TemplateSupport::convertIdOrElementIdToString).toList())
						: Set.of();

				// use this list to get down the road
				// 1. remove already visited ones;
				// we don't know which id came with which node, so we need to assume that
				// a relationshipId connects to all related nodes
				for (String newRelationshipId : newRelationshipIds) {
					relatedNodesVisited.put(newRelationshipId, relatedIds);
					Set<String> knownRelatedNodesBefore = relationshipsToRelatedNodes.get(newRelationshipId);
					if (knownRelatedNodesBefore != null) {
						Set<String> mergedKnownRelatedNodes = new HashSet<>(knownRelatedNodesBefore);
						// there are already existing nodes in there for this relationship
						mergedKnownRelatedNodes.addAll(relatedIds);
						relatedNodesVisited.put(newRelationshipId, mergedKnownRelatedNodes);
						relatedIds.removeAll(knownRelatedNodesBefore);
					}
				}

				relationshipsToRelatedNodes.putAll(relatedNodesVisited);
				// 2. for the rest start the exploration
				if (!relatedIds.isEmpty()) {
					iterateNextLevel(relatedIds, relationshipDescription, relationshipsToRelatedNodes, currentPathStep);
				}
			};
		}

	}

}
