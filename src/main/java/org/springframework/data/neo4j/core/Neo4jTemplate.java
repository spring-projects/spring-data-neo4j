/*
 * Copyright 2011-present the original author or authors.
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
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyPath;
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
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.NoResultException;
import org.springframework.data.neo4j.repository.query.QueryFragments;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @author Gerrit Meier
 * @author Corey Beres
 * @soundtrack Motörhead - We Are Motörhead
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
@SuppressWarnings("DataFlowIssue")
public final class Neo4jTemplate implements
		Neo4jOperations, FluentNeo4jOperations,
		BeanClassLoaderAware, BeanFactoryAware {

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

	private ClassLoader beanClassLoader;

	private EventSupport eventSupport;

	private ProjectionFactory projectionFactory;

	private Renderer renderer;

	private Function<Named, FunctionInvocation> elementIdOrIdFunction;

	private TransactionTemplate transactionTemplate;

	private TransactionTemplate transactionTemplateReadOnly;

	public Neo4jTemplate(Neo4jClient neo4jClient) {
		this(neo4jClient, new Neo4jMappingContext());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {
		this(neo4jClient, neo4jMappingContext, EntityCallbacks.create());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext, PlatformTransactionManager transactionManager) {
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
		return Objects.requireNonNull(this.projectionFactory, "Projection support for the Neo4j template is only available when the template is a proper and fully initialized Spring bean.");
	}

	@Override
	public long count(Class<?> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData).returning(Cypher.count(asterisk()))
				.build();

		return count(statement);
	}

	@Override
	public long count(Statement statement) {
		return count(statement, Collections.emptyMap());
	}

	@Override
	public long count(Statement statement, Map<String, Object> parameters) {

		return count(renderer.render(statement), TemplateSupport.mergeParameters(statement, parameters));
	}

	@Override
	public long count(String cypherQuery) {
		return count(cypherQuery, Collections.emptyMap());
	}

	@Override
	public long count(String cypherQuery, Map<String, Object> parameters) {
		return transactionTemplateReadOnly.execute(tx -> {
			PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class).withCypherQuery(cypherQuery)
					.withParameters(parameters).build();
			return toExecutableQuery(preparedQuery, true).getRequiredSingleResult();
		});
	}

	@Override
	public <T> List<T> findAll(Class<T> domainType) {

		return doFindAll(domainType, null);
	}

	private <T> List<T> doFindAll(Class<T> domainType, @Nullable Class<?> resultType) {
		return transactionTemplateReadOnly
				.execute(tx -> {
					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
					return createExecutableQuery(
							domainType, resultType, QueryFragmentsAndParameters.forFindAll(entityMetaData), true)
							.getResults();
				});
	}

	@Override
	public <T> List<T> findAll(Statement statement, Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> createExecutableQuery(domainType, statement, true).getResults());
	}

	@Override
	public <T> List<T> findAll(Statement statement, Map<String, Object> parameters, Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> createExecutableQuery(domainType, null, statement, parameters, true).getResults());
	}

	@Override
	public <T> Optional<T> findOne(Statement statement, Map<String, Object> parameters, Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> createExecutableQuery(domainType, null, statement, parameters, true).getSingleResult());
	}

	@Override
	public <T> List<T> findAll(String cypherQuery, Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> createExecutableQuery(domainType, cypherQuery, true).getResults());
	}

	@Override
	public <T> List<T> findAll(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> createExecutableQuery(domainType, null, cypherQuery, parameters, true).getResults());
	}

	@Override
	public <T> Optional<T> findOne(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> createExecutableQuery(domainType, null, cypherQuery, parameters, true).getSingleResult());
	}

	@Override
	public <T> ExecutableFind<T> find(Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> new FluentOperationSupport(this).find(domainType));
	}

	@SuppressWarnings("unchecked")
	<T, R> List<R> doFind(@Nullable String cypherQuery, @Nullable Map<String, Object> parameters, Class<T> domainType, Class<R> resultType, TemplateSupport.FetchType fetchType, @Nullable QueryFragmentsAndParameters queryFragmentsAndParameters) {

		return transactionTemplateReadOnly.execute(tx -> {
			List<T> intermediaResults = Collections.emptyList();
			if (cypherQuery == null && queryFragmentsAndParameters == null && fetchType == TemplateSupport.FetchType.ALL) {
				intermediaResults = doFindAll(domainType, resultType);
			} else {
				ExecutableQuery<T> executableQuery;
				if (queryFragmentsAndParameters == null) {
					executableQuery = createExecutableQuery(domainType, resultType, cypherQuery,
							parameters == null ? Collections.emptyMap() : parameters,
							true);
				} else {
					executableQuery = createExecutableQuery(domainType, resultType, queryFragmentsAndParameters, true);
				}
				intermediaResults = switch (fetchType) {
					case ALL -> executableQuery.getResults();
					case ONE -> executableQuery.getSingleResult().map(Collections::singletonList)
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

			DtoInstantiatingConverter converter = new DtoInstantiatingConverter(resultType, neo4jMappingContext);
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

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);

		QueryFragmentsAndParameters fragmentsAndParameters = QueryFragmentsAndParameters
				.forExistsById(entityMetaData, convertIdValues(entityMetaData.getRequiredIdProperty(), id));

		Statement statement = fragmentsAndParameters.getQueryFragments().toStatement();
		Map<String, Object> parameters = fragmentsAndParameters.getParameters();

		return count(statement, parameters) > 0;
	}

	@Override
	public <T> Optional<T> findById(Object id, Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> {
					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);

					return createExecutableQuery(domainType, null,
							QueryFragmentsAndParameters.forFindById(entityMetaData,
									convertIdValues(entityMetaData.getRequiredIdProperty(), id)),
							true)
							.getSingleResult();
				});
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> domainType) {
		return transactionTemplateReadOnly
				.execute(tx -> {
					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);

					return createExecutableQuery(domainType, null,
							QueryFragmentsAndParameters.forFindByAllId(
									entityMetaData, convertIdValues(entityMetaData.getRequiredIdProperty(), ids)),
							true)
							.getResults();
				});
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
	public <T> T save(T instance) {

		return transactionTemplate
				.execute(tx -> saveImpl(instance, Collections.emptySet(), null));

	}

	@Override
	public <T> T saveAs(T instance, BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		if (instance == null) {
			return null;
		}
		return transactionTemplate
				.execute(tx -> saveImpl(instance, TemplateSupport.computeIncludedPropertiesFromPredicate(this.neo4jMappingContext, instance.getClass(), includeProperty), null));
	}

	@Override
	public <T, R> R saveAs(T instance, Class<R> resultType) {

		return transactionTemplate.execute(tx -> {

					Assert.notNull(resultType, "ResultType must not be null");

					if (instance == null) {
						return null;
					}

					if (resultType.equals(instance.getClass())) {
						return resultType.cast(save(instance));
					}

					ProjectionFactory localProjectionFactory = getProjectionFactory();
					ProjectionInformation projectionInformation = localProjectionFactory.getProjectionInformation(resultType);
					Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(instance.getClass(), resultType,
							localProjectionFactory, neo4jMappingContext);

					T savedInstance = saveImpl(instance, pps, null);
					if (!resultType.isInterface()) {
						@SuppressWarnings("unchecked") R result = (R) new DtoInstantiatingConverter(resultType, neo4jMappingContext).convertDirectly(savedInstance);
						return result;
					}
					if (projectionInformation.isClosed()) {
						return localProjectionFactory.createProjection(resultType, savedInstance);
					}

					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(savedInstance.getClass());
					Neo4jPersistentProperty idProperty = entityMetaData.getIdProperty();
					PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
					return localProjectionFactory.createProjection(resultType,
							this.findById(propertyAccessor.getProperty(idProperty), savedInstance.getClass()).get());
				});
	}

	private <T> T saveImpl(T instance, @Nullable Collection<PropertyFilter.ProjectedPath> includedProperties, @Nullable NestedRelationshipProcessingStateMachine stateMachine) {

		if (stateMachine != null && stateMachine.hasProcessedValue(instance)) {
			return instance;
		}

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(instance.getClass());
		boolean isEntityNew = entityMetaData.isNew(instance);

		T entityToBeSaved = eventSupport.maybeCallBeforeBind(instance);

		DynamicLabels dynamicLabels = determineDynamicLabels(entityToBeSaved, entityMetaData);

		@SuppressWarnings("unchecked") // Applies to retrieving the meta data
		TemplateSupport.FilteredBinderFunction<T> binderFunction = TemplateSupport.createAndApplyPropertyFilter(
				includedProperties, entityMetaData,
				neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass())
		);
		Statement statement = cypherGenerator.prepareSaveOf(entityMetaData, dynamicLabels, TemplateSupport.rendererRendersElementId(renderer));
		Optional<Entity> newOrUpdatedNode = neo4jClient
				.query(() -> renderer.render(statement))
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
			if (!entityMetaData.isUsingDeprecatedInternalId() && TemplateSupport.rendererRendersElementId(renderer)) {
				return IdentitySupport.getElementId(node);
			}
			return node.id();
		}).get();

		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
		TemplateSupport.setGeneratedIdIfNecessary(entityMetaData, propertyAccessor, elementId, newOrUpdatedNode);
		TemplateSupport.updateVersionPropertyIfPossible(entityMetaData, propertyAccessor, newOrUpdatedNode.get());

		if (stateMachine == null) {
			stateMachine = new NestedRelationshipProcessingStateMachine(neo4jMappingContext, instance, elementId);
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
			var statementReturningDynamicLabels = cypherGenerator.createStatementReturningDynamicLabels(entityMetaData);
			Neo4jClient.RunnableSpec runnableQuery = neo4jClient
					.query(() -> renderer.render(statementReturningDynamicLabels))
					.bind(convertIdValues(idProperty, propertyAccessor.getProperty(idProperty)))
					.to(Constants.NAME_OF_ID).bind(entityMetaData.getStaticLabels())
					.to(Constants.NAME_OF_STATIC_LABELS_PARAM)
					.bindAll(statementReturningDynamicLabels.getCatalog().getParameters());

			if (entityMetaData.hasVersionProperty()) {
				runnableQuery = runnableQuery
						.bind((Long) propertyAccessor.getProperty(entityMetaData.getRequiredVersionProperty()))
						.to(Constants.NAME_OF_VERSION_PARAM);
			}

			Optional<Map<String, Object>> optionalResult = runnableQuery.fetch().one();
			return new DynamicLabels(entityMetaData, optionalResult.map(r -> (Collection<String>) r.get(Constants.NAME_OF_LABELS))
					.orElseGet(Collections::emptyList), (Collection<String>) propertyAccessor.getProperty(p));
		}).orElse(DynamicLabels.EMPTY);
	}

	@Override
	public <T> List<T> saveAll(Iterable<T> instances) {
		return transactionTemplate
				.execute(tx -> saveAllImpl(instances, Collections.emptySet(), null));
	}

	private boolean requiresSingleStatements(boolean heterogeneousCollection, Neo4jPersistentEntity<?> entityMetaData) {
		return heterogeneousCollection
				|| entityMetaData.isUsingInternalIds()
				|| entityMetaData.hasVersionProperty()
				|| entityMetaData.getDynamicLabelsProperty().isPresent();
	}

	private <T> List<T> saveAllImpl(Iterable<T> instances, @Nullable Collection<PropertyFilter.ProjectedPath> includedProperties, @Nullable BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		Set<Class<?>> types = new HashSet<>();
		List<T> entities = new ArrayList<>();
		instances.forEach(instance -> {
			entities.add(instance);
			types.add(instance.getClass());
		});

		if (entities.isEmpty()) {
			return Collections.emptyList();
		}

		boolean heterogeneousCollection = types.size() > 1;
		Class<?> domainClass = types.iterator().next();

		Collection<PropertyFilter.ProjectedPath> pps = includeProperty == null ?
				includedProperties :
				TemplateSupport.computeIncludedPropertiesFromPredicate(this.neo4jMappingContext, domainClass,
						includeProperty);

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainClass);

		if (requiresSingleStatements(heterogeneousCollection, entityMetaData)) {
			log.debug("Saving entities using single statements.");

			NestedRelationshipProcessingStateMachine stateMachine = new NestedRelationshipProcessingStateMachine(neo4jMappingContext);
			return entities.stream().map(e -> saveImpl(e, pps, stateMachine)).collect(Collectors.toList());
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
				.map(e -> new Tuple3<>(e, entityMetaData.isNew(e), eventSupport.maybeCallBeforeBind(e)))
				.collect(Collectors.toList());

		// Save roots
		@SuppressWarnings("unchecked") // We can safely assume here that we have a humongous collection with only one single type being either T or extending it
		Function<T, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) domainClass);
		binderFunction = TemplateSupport.createAndApplyPropertyFilter(pps, entityMetaData, binderFunction);
		List<Map<String, Object>> entityList = entitiesToBeSaved.stream().map(h -> h.modifiedInstance).map(binderFunction)
				.collect(Collectors.toList());
		var statement = cypherGenerator.prepareSaveOfMultipleInstancesOf(entityMetaData);
		Map<Value, String> idToInternalIdMapping = neo4jClient
				.query(() -> renderer.render(statement))
				.bind(entityList).to(Constants.NAME_OF_ENTITY_LIST_PARAM)
				.bindAll(statement.getCatalog().getParameters())
				.fetchAs(Map.Entry.class)
				.mappedBy((t, r) -> new AbstractMap.SimpleEntry<>(r.get(Constants.NAME_OF_ID), TemplateSupport.convertIdOrElementIdToString(r.get(Constants.NAME_OF_ELEMENT_ID))))
				.all()
				.stream()
				.collect(Collectors.toMap(m -> (Value) m.getKey(), m -> (String) m.getValue()));

		// Save related
		var stateMachine = new NestedRelationshipProcessingStateMachine(neo4jMappingContext, null, null);
		return entitiesToBeSaved.stream().map(t -> {
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(t.modifiedInstance);
			Neo4jPersistentProperty idProperty = entityMetaData.getRequiredIdProperty();
			Object id = convertIdValues(idProperty, propertyAccessor.getProperty(idProperty));
			String internalId = idToInternalIdMapping.get(id);
			stateMachine.registerInitialObject(t.originalInstance, internalId);
			return this.<T>processRelations(entityMetaData, propertyAccessor, t.wasNew, stateMachine, TemplateSupport.computeIncludePropertyPredicate(pps, entityMetaData));
		}).collect(Collectors.toList());
	}

	@Override
	public <T> List<T> saveAllAs(Iterable<T> instances, BiPredicate<PropertyPath, Neo4jPersistentProperty> includeProperty) {

		return transactionTemplate
				.execute(tx -> saveAllImpl(instances, null, includeProperty));
	}

	@Override
	public <T, R> List<R> saveAllAs(Iterable<T> instances, Class<R> resultType) {

		return transactionTemplate
				.execute(tx -> {

					Assert.notNull(resultType, "ResultType must not be null");

					Class<?> commonElementType = TemplateSupport.findCommonElementType(instances);

					if (commonElementType == null) {
						throw new IllegalArgumentException("Could not determine a common element of an heterogeneous collection");
					}

					if (commonElementType == TemplateSupport.EmptyIterable.class) {
						return Collections.emptyList();
					}

					if (resultType.isAssignableFrom(commonElementType)) {
						@SuppressWarnings("unchecked") // Nicer to live with this than streaming, mapping and collecting to avoid the cast. It's easier on the reactive side.
						List<R> saveElements = (List<R>) saveAll(instances);
						return saveElements;
					}

					ProjectionFactory localProjectionFactory = getProjectionFactory();
					ProjectionInformation projectionInformation = localProjectionFactory.getProjectionInformation(resultType);

					Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(commonElementType, resultType,
							localProjectionFactory, neo4jMappingContext);

					List<T> savedInstances = saveAllImpl(instances, pps, null);

					if (projectionInformation.isClosed()) {
						return savedInstances.stream().map(instance -> localProjectionFactory.createProjection(resultType, instance))
								.collect(Collectors.toList());
					}

					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(commonElementType);
					Neo4jPersistentProperty idProperty = entityMetaData.getIdProperty();

					List<Object> ids = savedInstances.stream().map(savedInstance -> {
						PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
						return propertyAccessor.getProperty(idProperty);
					}).collect(Collectors.toList());

					return findAllById(ids, commonElementType)
							.stream().map(instance -> localProjectionFactory.createProjection(resultType, instance))
							.collect(Collectors.toList());
				});
	}

	@Override
	public <T> void deleteById(Object id, Class<T> domainType) {

		transactionTemplate
				.executeWithoutResult(tx -> {

					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
					String nameOfParameter = "id";
					Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

					log.debug(() -> String.format("Deleting entity with id %s ", id));

					Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
					ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
							.bind(convertIdValues(entityMetaData.getRequiredIdProperty(), id))
							.to(nameOfParameter)
							.bindAll(statement.getCatalog().getParameters())
							.run();

					log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
							summary.counters().relationshipsDeleted()));
				});
	}

	@Override
	public <T> void deleteByIdWithVersion(Object id, Class<T> domainType, Neo4jPersistentProperty versionProperty,
										  Object versionValue) {

		transactionTemplate
				.executeWithoutResult(tx -> {
					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);

					String nameOfParameter = "id";
					Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter))
							.and(Cypher.property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData), versionProperty.getPropertyName())
									.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))
									.or(Cypher.property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData), versionProperty.getPropertyName()).isNull()));

					Statement statement = cypherGenerator.prepareMatchOf(entityMetaData, condition)
							.returning(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData)).build();

					Map<String, Object> parameters = new HashMap<>();
					parameters.put(nameOfParameter, convertIdValues(entityMetaData.getRequiredIdProperty(), id));
					parameters.put(Constants.NAME_OF_VERSION_PARAM, versionValue);

					createExecutableQuery(domainType, null, statement, parameters, false).getSingleResult().orElseThrow(
							() -> new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE)
					);

					deleteById(id, domainType);
				});
	}

	@Override
	public <T> void deleteAllById(Iterable<?> ids, Class<T> domainType) {

		transactionTemplate
				.executeWithoutResult(tx -> {

					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
					String nameOfParameter = "ids";
					Condition condition = entityMetaData.getIdExpression().in(parameter(nameOfParameter));

					log.debug(() -> String.format("Deleting all entities with the following ids: %s ", ids));

					Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
					ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
							.bind(convertIdValues(entityMetaData.getRequiredIdProperty(), ids))
							.to(nameOfParameter)
							.bindAll(statement.getCatalog().getParameters())
							.run();

					log.debug(() -> String.format("Deleted %d nodes and %d relationships.", summary.counters().nodesDeleted(),
							summary.counters().relationshipsDeleted()));
				});
	}

	@Override
	public void deleteAll(Class<?> domainType) {

		transactionTemplate
				.executeWithoutResult(tx -> {

					Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getRequiredPersistentEntity(domainType);
					log.debug(() -> String.format("Deleting all nodes with primary label %s", entityMetaData.getPrimaryLabel()));

					Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData);
					ResultSummary summary = this.neo4jClient.query(renderer.render(statement)).run();

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

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType, Statement statement, Map<String, Object> parameters, boolean readOnly) {

		return createExecutableQuery(domainType, resultType, renderer.render(statement), TemplateSupport.mergeParameters(statement, parameters), readOnly);
	}

	private <T> ExecutableQuery<T> createExecutableQuery(
			Class<T> domainType, @Nullable Class<?> resultType,
			@Nullable String cypherStatement,
			Map<String, Object> parameters,
			boolean readOnly) {

		Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction = TemplateSupport
				.getAndDecorateMappingFunction(neo4jMappingContext, domainType, resultType);
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
				.withCypherQuery(cypherStatement)
				.withParameters(parameters)
				.usingMappingFunction(mappingFunction)
				.build();

		return toExecutableQuery(preparedQuery, readOnly);
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
	 * @return The owner of the relations being processed
	 */
	private <T> T processRelations(
			Neo4jPersistentEntity<?> neo4jPersistentEntity,
			PersistentPropertyAccessor<?> parentPropertyAccessor,
			boolean isParentObjectNew,
			NestedRelationshipProcessingStateMachine stateMachine,
			PropertyFilter includeProperty
	) {

		PropertyFilter.RelaxedPropertyPath startingPropertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(neo4jPersistentEntity.getUnderlyingClass());
		return processNestedRelations(neo4jPersistentEntity, parentPropertyAccessor, isParentObjectNew,
				stateMachine, includeProperty, startingPropertyPath);
	}

	private <T> T processNestedRelations(
			Neo4jPersistentEntity<?> sourceEntity,
			PersistentPropertyAccessor<?> propertyAccessor,
			boolean isParentObjectNew,
			NestedRelationshipProcessingStateMachine stateMachine,
			PropertyFilter includeProperty,
			PropertyFilter.RelaxedPropertyPath previousPath
	) {

		Object fromId = propertyAccessor.getProperty(sourceEntity.getRequiredIdProperty());

		AssociationHandlerSupport.of(sourceEntity).doWithAssociations(association -> {

			// create context to bundle parameters
			NestedRelationshipContext relationshipContext = NestedRelationshipContext.of(association, propertyAccessor, sourceEntity);
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

				List<Object> knownRelationshipsIds = new ArrayList<>();
				if (idProperty != null) {
					for (Object relatedValueToStore : relatedValuesToStore) {
						if (relatedValueToStore == null) {
							continue;
						}

						Object id = relationshipContext.getRelationshipPropertiesPropertyAccessor(relatedValueToStore).getProperty(idProperty);
						if (id != null) {
							knownRelationshipsIds.add(id);
						}
					}
				}

				Statement relationshipRemoveQuery = cypherGenerator.prepareDeleteOf(sourceEntity, relationshipDescription, canUseElementId);

				neo4jClient.query(renderer.render(relationshipRemoveQuery))
						.bind(convertIdValues(sourceEntity.getIdProperty(), fromId)) //
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
				Object relatedObjectBeforeCallbacksApplied = relationshipContext.identifyAndExtractRelationshipTargetNode(relatedValueToStore);
				Neo4jPersistentEntity<?> targetEntity = neo4jMappingContext.getRequiredPersistentEntity(relatedObjectBeforeCallbacksApplied.getClass());
				boolean isNewEntity = targetEntity.isNew(relatedObjectBeforeCallbacksApplied);

				Object newRelatedObject = stateMachine.hasProcessedValue(relatedObjectBeforeCallbacksApplied)
						? stateMachine.getProcessedAs(relatedObjectBeforeCallbacksApplied)
						: eventSupport.maybeCallBeforeBind(relatedObjectBeforeCallbacksApplied);

				Object relatedInternalId;
				Entity savedEntity = null;
				// No need to save values if processed
				if (stateMachine.hasProcessedValue(relatedValueToStore)) {
					relatedInternalId = stateMachine.getObjectId(relatedValueToStore);
				} else {
					if (isNewEntity || relationshipDescription.cascadeUpdates()) {
						savedEntity = saveRelatedNode(newRelatedObject, targetEntity, includeProperty, currentPropertyPath);
					} else {
						var targetPropertyAccessor = targetEntity.getPropertyAccessor(newRelatedObject);
						var requiredIdProperty = targetEntity.getRequiredIdProperty();
						savedEntity = loadRelatedNode(targetEntity, targetPropertyAccessor.getProperty(requiredIdProperty));
					}
					relatedInternalId = TemplateSupport.rendererCanUseElementIdIfPresent(renderer, targetEntity) ? savedEntity.elementId() : savedEntity.id();
					stateMachine.markEntityAsProcessed(relatedValueToStore, relatedInternalId);
					if (relatedValueToStore instanceof MappingSupport.RelationshipPropertiesWithEntityHolder) {
						Object entity = ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValueToStore).getRelatedEntity();
						stateMachine.markAsAliased(entity, relatedInternalId);
					}
				}

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

				Map<String, Object> properties = new HashMap<>();
				properties.put(Constants.FROM_ID_PARAMETER_NAME, convertIdValues(sourceEntity.getRequiredIdProperty(), fromId));
				properties.put(Constants.TO_ID_PARAMETER_NAME, relatedInternalId);
				properties.put(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM, idValue);
				boolean isNewRelationship = idValue == null;
				if (relationshipDescription.isDynamic()) {
					// create new dynamic relationship properties
					if (relationshipDescription.hasRelationshipProperties() && isNewRelationship && idProperty != null) {
						CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatementForSingleRelationship(
								sourceEntity, relationshipDescription, relatedValueToStore, true, canUseElementId);

						List<Object> row = Collections.singletonList(properties);
						statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, row);

						Optional<Object> relationshipInternalId = neo4jClient.query(renderer.render(statementHolder.getStatement()))
								.bind(convertIdValues(sourceEntity.getRequiredIdProperty(), fromId)) //
								.to(Constants.FROM_ID_PARAMETER_NAME) //
								.bind(relatedInternalId) //
								.to(Constants.TO_ID_PARAMETER_NAME) //
								.bind(idValue) //
								.to(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM) //
								.bindAll(statementHolder.getProperties())
								.fetchAs(Object.class)
								.mappedBy((t, r) -> IdentitySupport.mapperForRelatedIdValues(idProperty).apply(r))
								.one();

						assignIdToRelationshipProperties(relationshipContext, relatedValueToStore, idProperty, relationshipInternalId.orElseThrow());
					} else { // plain (new or to update) dynamic relationship or dynamic relationships with properties to update

						CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatementForSingleRelationship(
								sourceEntity, relationshipDescription, relatedValueToStore, false, canUseElementId);

						List<Object> row = Collections.singletonList(properties);
						statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, row);
						neo4jClient.query(renderer.render(statementHolder.getStatement()))
								.bind(convertIdValues(sourceEntity.getRequiredIdProperty(), fromId)) //
								.to(Constants.FROM_ID_PARAMETER_NAME) //
								.bind(relatedInternalId) //
								.to(Constants.TO_ID_PARAMETER_NAME) //
								.bind(idValue)
								.to(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM) //
								.bindAll(statementHolder.getProperties())
								.run();
					}
				} else if (relationshipDescription.hasRelationshipProperties()) {
					// check if bidi mapped already
					var hlp = ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValueToStore);
					var hasProcessedRelationshipEntity = stateMachine.hasProcessedRelationshipEntity(propertyAccessor.getBean(), hlp.getRelatedEntity(), relationshipContext.getRelationship());
					if (hasProcessedRelationshipEntity) {
						stateMachine.requireIdUpdate(sourceEntity, relationshipDescription, canUseElementId, fromId, relatedInternalId, relationshipContext, relatedValueToStore, idProperty);
					} else {
						if (isNewRelationship && idProperty != null) {
							newRelationshipPropertiesRows.add(properties);
							newRelationshipPropertiesToStore.add(relatedValueToStore);
						} else {
							neo4jMappingContext.getEntityConverter().write(hlp.getRelationshipProperties(), properties);
							relationshipPropertiesRows.add(properties);
						}
						stateMachine.storeProcessRelationshipEntity(hlp, propertyAccessor.getBean(), hlp.getRelatedEntity(), relationshipContext.getRelationship());
					}
				} else {
					// non-dynamic relationship or relationship with properties
					plainRelationshipRows.add(properties);
				}

				if (processState != ProcessState.PROCESSED_ALL_VALUES) {
					processNestedRelations(targetEntity, targetPropertyAccessor, isNewEntity, stateMachine, includeProperty, currentPropertyPath);
				}

				Object potentiallyRecreatedNewRelatedObject = MappingSupport.getRelationshipOrRelationshipPropertiesObject(neo4jMappingContext,
								relationshipDescription.hasRelationshipProperties(),
								relationshipProperty.isDynamicAssociation(),
								relatedValueToStore,
								targetPropertyAccessor);
				relationshipHandler.handle(relatedValueToStore, relatedObjectBeforeCallbacksApplied, potentiallyRecreatedNewRelatedObject);
			}
			// batch operations
			if (!(relationshipDescription.hasRelationshipProperties() || relationshipDescription.isDynamic() || plainRelationshipRows.isEmpty())) {
				CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatementForImperativeSimpleRelationshipBatch(
						sourceEntity, relationshipDescription, plainRelationshipRows, canUseElementId);
				statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, plainRelationshipRows);
				neo4jClient.query(renderer.render(statementHolder.getStatement()))
						.bindAll(statementHolder.getProperties())
						.bindAll(statementHolder.getStatement().getCatalog().getParameters())
						.run();
			} else if (relationshipDescription.hasRelationshipProperties()) {
				if (!relationshipPropertiesRows.isEmpty()) {
					CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatementForImperativeRelationshipsWithPropertiesBatch(false,
							sourceEntity, relationshipDescription, updateRelatedValuesToStore, relationshipPropertiesRows, canUseElementId);
					statementHolder = statementHolder.addProperty(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, relationshipPropertiesRows);

					neo4jClient.query(renderer.render(statementHolder.getStatement()))
							.bindAll(statementHolder.getProperties())
							.bindAll(statementHolder.getStatement().getCatalog().getParameters())
							.run();
				}
				if (!newRelationshipPropertiesToStore.isEmpty()) {
					CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatementForImperativeRelationshipsWithPropertiesBatch(true,
							sourceEntity, relationshipDescription, newRelationshipPropertiesToStore, newRelationshipPropertiesRows, canUseElementId);
					List<Object> all = new ArrayList<>(neo4jClient.query(renderer.render(statementHolder.getStatement()))
							.bindAll(statementHolder.getProperties())
							.bindAll(statementHolder.getStatement().getCatalog().getParameters())
							.fetchAs(Object.class)
							.mappedBy((t, r) -> IdentitySupport.mapperForRelatedIdValues(idProperty).apply(r))
							.all());
					// assign new ids
					for (int i = 0; i < all.size(); i++) {
						Object anId = all.get(i);
						assignIdToRelationshipProperties(relationshipContext, newRelationshipPropertiesToStore.get(i), idProperty, anId);
					}
				}
			}

			// Possible grab missing relationship ids now for bidirectional ones, with properties, mapped in opposite directions
			stateMachine.updateRelationshipIds(this::getRelationshipId);

			relationshipHandler.applyFinalResultToOwner(propertyAccessor);
		});

		@SuppressWarnings("unchecked")
		T finalSubgraphRoot = (T) propertyAccessor.getBean();
		return finalSubgraphRoot;
	}

	private Optional<Object> getRelationshipId(Statement statement, Neo4jPersistentProperty idProperty, Object fromId, Object toId) {

		return neo4jClient.query(renderer.render(statement))
				.bind(convertIdValues(idProperty, fromId)) //
				.to(Constants.FROM_ID_PARAMETER_NAME) //
				.bind(toId) //
				.to(Constants.TO_ID_PARAMETER_NAME) //
				.bindAll(statement.getCatalog().getParameters())
				.fetchAs(Object.class)
				.mappedBy((t, r) -> IdentitySupport.mapperForRelatedIdValues(idProperty).apply(r))
				.one();
	}


	// The pendant to {@link #saveRelatedNode(Object, NodeDescription, PropertyFilter, PropertyFilter.RelaxedPropertyPath)}
	// We can't do without a query, as we need to refresh the internal id
	private Entity loadRelatedNode(NodeDescription<?> targetNodeDescription, Object relatedInternalId) {

		var targetPersistentEntity = (Neo4jPersistentEntity<?>) targetNodeDescription;
		var queryFragmentsAndParameters = QueryFragmentsAndParameters.forFindById(targetPersistentEntity, convertIdValues(targetPersistentEntity.getRequiredIdProperty(), relatedInternalId));
		var nodeName = Constants.NAME_OF_TYPED_ROOT_NODE.apply(targetNodeDescription).getValue();

		return neo4jClient
				.query(() -> renderer.render(
						cypherGenerator.prepareFindOf(targetNodeDescription, queryFragmentsAndParameters.getQueryFragments().getMatchOn(),
								queryFragmentsAndParameters.getQueryFragments().getCondition()).returning(nodeName).build()))
				.bindAll(queryFragmentsAndParameters.getParameters())
				.fetchAs(Entity.class).mappedBy((t, r) -> r.get(nodeName).asNode())
				.one().orElseThrow();
	}

	private void assignIdToRelationshipProperties(
			NestedRelationshipContext relationshipContext,
			Object relatedValueToStore,
			Neo4jPersistentProperty idProperty,
			Object relationshipInternalId
	) {
		relationshipContext
				.getRelationshipPropertiesPropertyAccessor(relatedValueToStore)
				.setProperty(idProperty, relationshipInternalId);
	}

	private Entity saveRelatedNode(Object entity, NodeDescription<?> targetNodeDescription, PropertyFilter includeProperty, PropertyFilter.RelaxedPropertyPath currentPropertyPath) {

		Neo4jPersistentEntity<?> targetPersistentEntity = (Neo4jPersistentEntity<?>) targetNodeDescription;
		DynamicLabels dynamicLabels = determineDynamicLabels(entity, targetPersistentEntity);
		@SuppressWarnings("rawtypes")
		Class entityType = targetPersistentEntity.getType();
		@SuppressWarnings("unchecked")
		Function<Object, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(entityType);
		binderFunction = binderFunction.andThen(tree -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) tree.get(Constants.NAME_OF_PROPERTIES_PARAM);
			String idPropertyName = targetPersistentEntity.getIdProperty().getPropertyName();
			IdDescription idDescription = targetPersistentEntity.getIdDescription();
			boolean assignedId = idDescription.isAssignedId() || idDescription.isExternallyGeneratedId();
			if (!includeProperty.isNotFiltering()) {
				properties.entrySet()
						.removeIf(e -> {
							// we cannot skip the id property if it is an assigned id
							boolean isIdProperty = e.getKey().equals(idPropertyName);
							return !(assignedId && isIdProperty) && !includeProperty.contains(currentPropertyPath.append(e.getKey()));
						});
			}
			return tree;
		});
		var statement = cypherGenerator.prepareSaveOf(targetNodeDescription, dynamicLabels, TemplateSupport.rendererRendersElementId(renderer));
		Optional<Entity> optionalSavedNode = neo4jClient
				.query(() -> renderer.render(statement))
				.bind(entity).with(binderFunction)
				.bindAll(statement.getCatalog().getParameters())
				.fetchAs(Entity.class)
				.one();

		if (targetPersistentEntity.hasVersionProperty() && !optionalSavedNode.isPresent()) {
			throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
		}

		// It is checked above, god dammit.
		//noinspection OptionalGetWithoutIsPresent
		return optionalSavedNode.get();
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader == null ? org.springframework.util.ClassUtils.getDefaultClassLoader() : beanClassLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		this.eventSupport = EventSupport.discoverCallbacks(neo4jMappingContext, beanFactory);

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

		if (this.transactionTemplate != null && this.transactionTemplateReadOnly != null) {
			return;
		}
		PlatformTransactionManager transactionManager = null;
		var it = beanFactory.getBeanProvider(PlatformTransactionManager.class).stream().iterator();
		while (it.hasNext()) {
			PlatformTransactionManager transactionManagerCandidate = it.next();
			if (transactionManagerCandidate instanceof Neo4jTransactionManager neo4jTransactionManager) {
				if (transactionManager != null) {
					throw new IllegalStateException("Multiple Neo4jTransactionManagers are defined in this context. " +
							"If this in intended, please pass the transaction manager to use with this Neo4jTemplate in the constructor");
				}
				transactionManager = neo4jTransactionManager;
			}
		}
		setTransactionManager(transactionManager);
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


	private <T> ExecutableQuery<T> createExecutableQuery(
			Class<T> domainType, @Nullable Class<?> resultType,
 			QueryFragmentsAndParameters queryFragmentsAndParameters,
			boolean readOnlyTransaction) {

		Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction = TemplateSupport
				.getAndDecorateMappingFunction(neo4jMappingContext, domainType, resultType);
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
		return transactionTemplate
				.execute(tx -> {
					// empty check
					if (!instances.iterator().hasNext()) {
						return Collections.emptyList();
					}

					Class<?> resultType = TemplateSupport.findCommonElementType(instances);

					Collection<PropertyFilter.ProjectedPath> pps = PropertyFilterSupport.addPropertiesFrom(domainType, resultType,
							getProjectionFactory(), neo4jMappingContext);

					NestedRelationshipProcessingStateMachine stateMachine = new NestedRelationshipProcessingStateMachine(neo4jMappingContext);
					List<R> results = new ArrayList<>();
					EntityFromDtoInstantiatingConverter<T> converter = new EntityFromDtoInstantiatingConverter<>(domainType, neo4jMappingContext);
					for (R instance : instances) {
						T domainObject = converter.convert(instance);

						T savedEntity = saveImpl(domainObject, pps, stateMachine);

						@SuppressWarnings("unchecked")
						R convertedBack = (R) new DtoInstantiatingConverter(resultType, neo4jMappingContext).convertDirectly(savedEntity);
						results.add(convertedBack);
					}
					return results;
				});
	}

	String render(Statement statement) {
		return renderer.render(statement);
	}

	final class DefaultExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;
		private final TransactionTemplate txTemplate;

		DefaultExecutableQuery(PreparedQuery<T> preparedQuery, boolean readOnly) {
			this.preparedQuery = preparedQuery;
			this.txTemplate = readOnly ? transactionTemplateReadOnly : transactionTemplate;
		}


		@SuppressWarnings("unchecked")
		public List<T> getResults() {
			return txTemplate
					.execute(tx -> {
						Collection<T> all = createFetchSpec().map(Neo4jClient.RecordFetchSpec::all).orElse(Collections.emptyList());
						if (preparedQuery.resultsHaveBeenAggregated()) {
							return all.stream().flatMap(nested -> ((Collection<T>) nested).stream()).distinct().collect(Collectors.toList());
						}
						return new ArrayList<>(all);
					});
		}

		@SuppressWarnings("unchecked")
		public Optional<T> getSingleResult() {
			return txTemplate.execute(tx -> {
				try {
					Optional<T> one = createFetchSpec().flatMap(Neo4jClient.RecordFetchSpec::one);
					if (preparedQuery.resultsHaveBeenAggregated()) {
						return one.map(aggregatedResults -> ((LinkedHashSet<T>) aggregatedResults).iterator().next());
					}
					return one;
				} catch (NoSuchRecordException e) {
					// This exception is thrown by the driver in both cases when there are 0 or 1+n records
					// So there has been an incorrect result size, but not too few results but too many.
					throw new IncorrectResultSizeDataAccessException(e.getMessage(), 1);
				}
			});
		}

		@SuppressWarnings("unchecked")
		public T getRequiredSingleResult() {
			return txTemplate.execute(tx -> {
				Optional<T> one = createFetchSpec().flatMap(Neo4jClient.RecordFetchSpec::one);
				if (preparedQuery.resultsHaveBeenAggregated()) {
					one = one.map(aggregatedResults -> ((LinkedHashSet<T>) aggregatedResults).iterator().next());
				}
				return one.orElseThrow(() -> new NoResultException(1, preparedQuery.getQueryFragmentsAndParameters().getCypherQuery()));
			});
		}

		private Optional<Neo4jClient.RecordFetchSpec<T>> createFetchSpec() {
			QueryFragmentsAndParameters queryFragmentsAndParameters = preparedQuery.getQueryFragmentsAndParameters();
			String cypherQuery = queryFragmentsAndParameters.getCypherQuery();
			Map<String, Object> finalParameters = queryFragmentsAndParameters.getParameters();

			QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();
			Neo4jPersistentEntity<?> entityMetaData = (Neo4jPersistentEntity<?>) queryFragmentsAndParameters.getNodeDescription();

			boolean containsPossibleCircles = entityMetaData != null && entityMetaData.containsPossibleCircles(queryFragments::includeField);
			if (cypherQuery == null || containsPossibleCircles) {
				Statement statement;
				if (containsPossibleCircles && !queryFragments.isScalarValueReturn()) {
					NodesAndRelationshipsByIdStatementProvider nodesAndRelationshipsById =
							createNodesAndRelationshipsByIdStatementProvider(entityMetaData, queryFragments, queryFragmentsAndParameters.getParameters());

					if (nodesAndRelationshipsById.hasRootNodeIds()) {
						return Optional.empty();
					}
					statement = nodesAndRelationshipsById.toStatement(entityMetaData);
				} else {
					statement = queryFragments.toStatement();
				}
				cypherQuery = renderer.render(statement);
				finalParameters = TemplateSupport.mergeParameters(statement, finalParameters);
			}

			Neo4jClient.MappingSpec<T> newMappingSpec = neo4jClient.query(cypherQuery)
					.bindAll(finalParameters).fetchAs(preparedQuery.getResultType());
			return preparedQuery.getOptionalMappingFunction()
					.map(newMappingSpec::mappedBy).or(() -> Optional.of(newMappingSpec));
		}

		private NodesAndRelationshipsByIdStatementProvider createNodesAndRelationshipsByIdStatementProvider(Neo4jPersistentEntity<?> entityMetaData,
						   QueryFragments queryFragments, Map<String, Object> parameters) {

			// first check if the root node(s) exist(s) at all
			Statement rootNodesStatement = cypherGenerator
					.prepareMatchOf(entityMetaData, queryFragments.getMatchOn(), queryFragments.getCondition())
					.returning(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE).build();

			Map<String, Object> usedParameters = new HashMap<>(parameters);
			usedParameters.putAll(rootNodesStatement.getCatalog().getParameters());

			final Collection<String> rootNodeIds = new HashSet<>(neo4jClient
					.query(renderer.render(rootNodesStatement))
					.bindAll(usedParameters)
					.fetchAs(Value.class).mappedBy((t, r) -> r.get(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE))
					.one()
					.map(value -> value.asList(TemplateSupport::convertIdOrElementIdToString))
					.get());

			if (rootNodeIds.isEmpty()) {
				// fast return if no matching root node(s) are found
				return NodesAndRelationshipsByIdStatementProvider.EMPTY;
			}
			// load first level relationships
			final Map<String, Set<String>> relationshipsToRelatedNodeIds = new HashMap<>();

			for (RelationshipDescription relationshipDescription : entityMetaData.getRelationshipsInHierarchy(queryFragments::includeField)) {

				Statement statement = cypherGenerator
						.prepareMatchOf(entityMetaData, relationshipDescription, queryFragments.getMatchOn(), queryFragments.getCondition())
						.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

				usedParameters = new HashMap<>(parameters);
				usedParameters.putAll(statement.getCatalog().getParameters());
				neo4jClient.query(renderer.render(statement))
						.bindAll(usedParameters)
						.fetch()
						.one()
						.ifPresent(iterateAndMapNextLevel(relationshipsToRelatedNodeIds, relationshipDescription, PropertyPathWalkStep.empty()));
			}

			return new NodesAndRelationshipsByIdStatementProvider(rootNodeIds, relationshipsToRelatedNodeIds.keySet(), relationshipsToRelatedNodeIds.values().stream().flatMap(Collection::stream).toList(), queryFragments, elementIdOrIdFunction);
		}

		private void iterateNextLevel(Collection<String> nodeIds, RelationshipDescription sourceRelationshipDescription,
									  Map<String, Set<String>> relationshipsToRelatedNodes, PropertyPathWalkStep currentPathStep) {

			Neo4jPersistentEntity<?> target = (Neo4jPersistentEntity<?>) sourceRelationshipDescription.getTarget();

			@SuppressWarnings("unchecked")
			String fieldName = ((Association<Neo4jPersistentProperty>) sourceRelationshipDescription).getInverse().getFieldName();
			PropertyPathWalkStep nextPathStep = currentPathStep.with((sourceRelationshipDescription.hasRelationshipProperties() ?
					fieldName + "." + ((Neo4jPersistentEntity<?>) sourceRelationshipDescription.getRelationshipPropertiesEntity())
							.getPersistentProperty(TargetNode.class).getFieldName() : fieldName));


			Collection<RelationshipDescription> relationships = target
					.getRelationshipsInHierarchy(
							relaxedPropertyPath -> {

								PropertyFilter.RelaxedPropertyPath prepend = relaxedPropertyPath.prepend(nextPathStep.path);
								prepend = PropertyFilter.RelaxedPropertyPath.withRootType(preparedQuery.getResultType()).append(prepend.toDotPath());
								return preparedQuery.getQueryFragmentsAndParameters().getQueryFragments().includeField(prepend);
							}
					);

			for (RelationshipDescription relationshipDescription : relationships) {

				Node node = anyNode(Constants.NAME_OF_TYPED_ROOT_NODE.apply(target));

				Statement statement = cypherGenerator
						.prepareMatchOf(target, relationshipDescription, null,
								elementIdOrIdFunction.apply(node).in(Cypher.parameter(Constants.NAME_OF_IDS)))
						.returning(cypherGenerator.createGenericReturnStatement()).build();

				neo4jClient.query(renderer.render(statement))
						.bindAll(Collections.singletonMap(Constants.NAME_OF_IDS, TemplateSupport.convertToLongIdOrStringElementId(nodeIds)))
						.bindAll(statement.getCatalog().getParameters())
						.fetch()
						.one()
						.ifPresent(iterateAndMapNextLevel(relationshipsToRelatedNodes, relationshipDescription, nextPathStep));
			}
		}

		@NonNull
		private Consumer<Map<String, Object>> iterateAndMapNextLevel(Map<String, Set<String>> relationshipsToRelatedNodes,
																	 RelationshipDescription relationshipDescription,
																	 PropertyPathWalkStep currentPathStep) {

			return record -> {

				Map<String, Set<String>> relatedNodesVisited = new HashMap<>(relationshipsToRelatedNodes);
				@SuppressWarnings("unchecked")
				List<String> newRelationshipIds = ((List<Object>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATIONS)).stream().map(TemplateSupport::convertIdOrElementIdToString).toList();
				@SuppressWarnings("unchecked")
				Set<String> relatedIds = new HashSet<>(((List<Object>) record.get(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES)).stream().map(TemplateSupport::convertIdOrElementIdToString).toList());

				// use this list to get down the road
				// 1. remove already visited ones;
				// we don't know which id came with which node, so we need to assume that a relationshipId connects to all related nodes
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
