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
import java.util.function.BiFunction;
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
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.neo4j.core.TemplateSupport.NodesAndRelationshipsByIdStatementProvider;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CreateRelationshipStatementHolder;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.DtoInstantiatingConverter;
import org.springframework.data.neo4j.core.mapping.EntityFromDtoInstantiatingConverter;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
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
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.neo4j.repository.query.QueryFragments;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @author Gerrit Meier
 * @soundtrack Motörhead - We Are Motörhead
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class Neo4jTemplate implements
		Neo4jOperations, FluentNeo4jOperations, FluentSaveOperation,
		BeanClassLoaderAware, BeanFactoryAware {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(Neo4jTemplate.class));

	private static final String OPTIMISTIC_LOCKING_ERROR_MESSAGE = "An entity with the required version does not exist.";

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final Neo4jClient neo4jClient;

	private final Neo4jMappingContext neo4jMappingContext;

	private final CypherGenerator cypherGenerator;

	private ClassLoader beanClassLoader;

	private EventSupport eventSupport;

	private ProjectionFactory projectionFactory;

	@Deprecated
	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
						 DatabaseSelectionProvider databaseSelectionProvider) {

		this(neo4jClient, neo4jMappingContext, EntityCallbacks.create());

		if (databaseSelectionProvider != neo4jClient.getDatabaseSelectionProvider()) {
			throw new IllegalStateException(
					"The provided database selection provider differs from the Neo4jClient's one.");
		}
	}

	@Deprecated
	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
						 DatabaseSelectionProvider databaseSelectionProvider, EntityCallbacks entityCallbacks) {

		this(neo4jClient, neo4jMappingContext, entityCallbacks);

		if (databaseSelectionProvider != neo4jClient.getDatabaseSelectionProvider()) {
			throw new IllegalStateException(
					"The provided database selection provider differs from the Neo4jClient's one.");
		}
	}

	public Neo4jTemplate(Neo4jClient neo4jClient) {
		this(neo4jClient, new Neo4jMappingContext());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {
		this(neo4jClient, neo4jMappingContext, EntityCallbacks.create());
	}

	public Neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
						 EntityCallbacks entityCallbacks) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
		this.eventSupport = EventSupport.useExistingCallbacks(neo4jMappingContext, entityCallbacks);
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

		return count(renderer.render(statement), TemplateSupport.mergeParameters(statement, parameters));
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

		return doFindAll(domainType, null);
	}

	private <T> List<T> doFindAll(Class<T> domainType, Class<?> resultType) {
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		return createExecutableQuery(domainType, resultType, QueryFragmentsAndParameters.forFindAll(entityMetaData))
				.getResults();
	}

	@Override
	public <T> List<T> findAll(Statement statement, Class<T> domainType) {
		return createExecutableQuery(domainType, statement).getResults();
	}

	@Override
	public <T> List<T> findAll(Statement statement, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, null, statement, parameters).getResults();
	}

	@Override
	public <T> Optional<T> findOne(Statement statement, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, null, statement, parameters).getSingleResult();
	}

	@Override
	public <T> List<T> findAll(String cypherQuery, Class<T> domainType) {
		return createExecutableQuery(domainType, cypherQuery).getResults();
	}

	@Override
	public <T> List<T> findAll(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, null, cypherQuery, parameters).getResults();
	}

	@Override
	public <T> Optional<T> findOne(String cypherQuery, Map<String, Object> parameters, Class<T> domainType) {
		return createExecutableQuery(domainType, null, cypherQuery, parameters).getSingleResult();
	}

	@Override
	public <T> ExecutableFind<T> find(Class<T> domainType) {
		return new FluentOperationSupport(this).find(domainType);
	}

	@SuppressWarnings("unchecked")
	<T, R> List<R> doFind(@Nullable String cypherQuery, @Nullable Map<String, Object> parameters, Class<T> domainType, Class<R> resultType, TemplateSupport.FetchType fetchType) {

		List<T> intermediaResults = Collections.emptyList();
		if (cypherQuery == null && fetchType == TemplateSupport.FetchType.ALL) {
			intermediaResults = doFindAll(domainType, resultType);
		} else {
			ExecutableQuery<T> executableQuery = createExecutableQuery(domainType, resultType, cypherQuery,
					parameters == null ? Collections.emptyMap() : parameters);
			switch (fetchType) {
				case ALL:
					intermediaResults = executableQuery.getResults();
					break;
				case ONE:
					intermediaResults = executableQuery.getSingleResult().map(Collections::singletonList)
							.orElseGet(Collections::emptyList);
					break;
			}
		}

		if (resultType.isAssignableFrom(domainType)) {
			return (List<R>) intermediaResults;
		}

		if (resultType.isInterface()) {
			return intermediaResults.stream()
					.map(instance -> projectionFactory.createProjection(resultType, instance))
					.collect(Collectors.toList());
		}

		DtoInstantiatingConverter converter = new DtoInstantiatingConverter(resultType, neo4jMappingContext);
		return intermediaResults.stream()
				.map(EntityInstanceWithSource.class::cast)
				.map(converter::convert)
				.map(v -> (R) v)
				.collect(Collectors.toList());
	}

	@Override
	public <T> Optional<T> findById(Object id, Class<T> domainType) {
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);

		return createExecutableQuery(domainType, null,
				QueryFragmentsAndParameters.forFindById(entityMetaData,
						convertIdValues(entityMetaData.getRequiredIdProperty(), id)))
				.getSingleResult();
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> domainType) {
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);

		return createExecutableQuery(domainType, null,
				QueryFragmentsAndParameters.forFindByAllId(
						entityMetaData, convertIdValues(entityMetaData.getRequiredIdProperty(), ids)))
				.getResults();
	}

	private Object convertIdValues(@Nullable Neo4jPersistentProperty idProperty, Object idValues) {

		if (((Neo4jPersistentEntity<?>) idProperty.getOwner()).isUsingInternalIds()) {
			return idValues;
		}

		return neo4jMappingContext.getConversionService().writeValue(idValues,
				ClassTypeInformation.from(idValues.getClass()),
				idProperty == null ? null : idProperty.getOptionalWritingConverter());
	}

	@Override
	public <T> T save(T instance) {

		return saveImpl(instance, Collections.emptyList());
	}

	@Override
	public <T, R> R saveAs(T instance, Class<R> resultType) {

		Assert.notNull(resultType, "ResultType must not be null!");

		if (instance == null) {
			return null;
		}

		if (resultType.isInstance(instance)) {
			return (R) save(instance);
		}

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(resultType);
		Collection<PropertyPath> pps = PropertyFilterSupport.addPropertiesFrom(resultType, resultType,
				projectionFactory, neo4jMappingContext);

		T savedInstance = saveImpl(instance, pps);
		if (projectionInformation.isClosed()) {
			return projectionFactory.createProjection(resultType, savedInstance);
		}

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(savedInstance.getClass());
		Neo4jPersistentProperty idProperty = entityMetaData.getIdProperty();
		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
		return projectionFactory.createProjection(resultType,
				this.findById(propertyAccessor.getProperty(idProperty), savedInstance.getClass()).get());
	}

	private <T> T saveImpl(T instance, Collection<PropertyPath> includedProperties) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(instance.getClass());
		boolean isEntityNew = entityMetaData.isNew(instance);

		T entityToBeSaved = eventSupport.maybeCallBeforeBind(instance);

		DynamicLabels dynamicLabels = determineDynamicLabels(entityToBeSaved, entityMetaData);

		Function<T, Map<String, Object>> binderFunction = neo4jMappingContext
				.getRequiredBinderFunctionFor((Class<T>) entityToBeSaved.getClass());

		PropertyFilter includeProperty = TemplateSupport.computeIncludePropertyPredicate(includedProperties, entityMetaData);
		binderFunction = binderFunction.andThen(tree -> {
			Map<String, Object> properties = (Map<String, Object>) tree.get(Constants.NAME_OF_PROPERTIES_PARAM);

			if (!includeProperty.isNotFiltering()) {
				properties.entrySet().removeIf(e -> !includeProperty.contains(e.getKey(), entityMetaData.getUnderlyingClass()));
			}
			return tree;
		});
		Optional<Entity> newOrUpdatedNode = neo4jClient
				.query(() -> renderer.render(cypherGenerator.prepareSaveOf(entityMetaData, dynamicLabels)))
				.bind(entityToBeSaved)
				.with(binderFunction)
				.fetchAs(Entity.class)
				.one();

		if (!newOrUpdatedNode.isPresent()) {
			if (entityMetaData.hasVersionProperty()) {
				throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
			}
			// defensive exception throwing
			throw new IllegalStateException("Could not retrieve an internal id while saving.");
		}

		Long internalId = newOrUpdatedNode.get().id();

		PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
		if (entityMetaData.isUsingInternalIds()) {
			propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), internalId);
		}
		TemplateSupport.updateVersionPropertyIfPossible(entityMetaData, propertyAccessor, newOrUpdatedNode.get());
		processRelations(entityMetaData, instance, internalId, propertyAccessor, isEntityNew, includeProperty);

		return propertyAccessor.getBean();
	}

	private <T> DynamicLabels determineDynamicLabels(T entityToBeSaved, Neo4jPersistentEntity<?> entityMetaData) {
		return entityMetaData.getDynamicLabelsProperty().map(p -> {

			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entityToBeSaved);
			Neo4jPersistentProperty idProperty = entityMetaData.getRequiredIdProperty();
			Neo4jClient.RunnableSpecTightToDatabase runnableQuery = neo4jClient
					.query(() -> renderer.render(cypherGenerator.createStatementReturningDynamicLabels(entityMetaData)))
					.bind(convertIdValues(idProperty, propertyAccessor.getProperty(idProperty)))
					.to(Constants.NAME_OF_ID).bind(entityMetaData.getStaticLabels())
					.to(Constants.NAME_OF_STATIC_LABELS_PARAM);

			if (entityMetaData.hasVersionProperty()) {
				runnableQuery = runnableQuery
						.bind((Long) propertyAccessor.getProperty(entityMetaData.getRequiredVersionProperty()))
						.to(Constants.NAME_OF_VERSION_PARAM);
			}

			Optional<Map<String, Object>> optionalResult = runnableQuery.fetch().one();
			return new DynamicLabels(optionalResult.map(r -> (Collection<String>) r.get(Constants.NAME_OF_LABELS))
					.orElseGet(Collections::emptyList), (Collection<String>) propertyAccessor.getProperty(p));
		}).orElse(DynamicLabels.EMPTY);
	}

	@Override
	public <T> List<T> saveAll(Iterable<T> instances) {
		return saveAllImpl(instances, Collections.emptyList());
	}

	private <T> List<T> saveAllImpl(Iterable<T> instances, List<PropertyPath> includedProperties) {

		List<T> entities;
		if (instances instanceof Collection) {
			entities = new ArrayList<>((Collection<T>) instances);
		} else {
			entities = new ArrayList<>();
			instances.forEach(entities::add);
		}

		if (entities.isEmpty()) {
			return Collections.emptyList();
		}

		Class<T> domainClass = (Class<T>) TemplateSupport.findCommonElementType(entities);
		Assert.notNull(domainClass, "Could not determine common domain class to save.");
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainClass);
		if (entityMetaData.isUsingInternalIds() || entityMetaData.hasVersionProperty()
				|| entityMetaData.getDynamicLabelsProperty().isPresent()) {
			log.debug("Saving entities using single statements.");

			return entities.stream().map(e -> saveImpl(e, includedProperties)).collect(Collectors.toList());
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
				.map(e -> new Tuple3<>(e, neo4jMappingContext.getPersistentEntity(e.getClass()).isNew(e), eventSupport.maybeCallBeforeBind(e)))
				.collect(Collectors.toList());

		// Save roots
		Function<T, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(domainClass);
		List<Map<String, Object>> entityList = entitiesToBeSaved.stream().map(h -> h.modifiedInstance).map(binderFunction)
				.collect(Collectors.toList());
		ResultSummary resultSummary = neo4jClient
				.query(() -> renderer.render(cypherGenerator.prepareSaveOfMultipleInstancesOf(entityMetaData)))
				.bind(entityList).to(Constants.NAME_OF_ENTITY_LIST_PARAM).run();

		SummaryCounters counters = resultSummary.counters();
		log.debug(() -> String.format(
				"Created %d and deleted %d nodes, created %d and deleted %d relationships and set %d properties.",
				counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
				counters.relationshipsDeleted(), counters.propertiesSet()));

		// Save related
		return entitiesToBeSaved.stream().map(t -> {
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(t.modifiedInstance);
			return processRelations(entityMetaData, t.originalInstance, propertyAccessor, t.wasNew, TemplateSupport.computeIncludePropertyPredicate(includedProperties, entityMetaData));
		}).collect(Collectors.toList());
	}

	@Override
	public <T, R> List<R> saveAllAs(Iterable<T> instances, Class<R> resultType) {

		Assert.notNull(resultType, "ResultType must not be null!");

		Class<?> commonElementType = TemplateSupport.findCommonElementType(instances);

		if (resultType.isAssignableFrom(commonElementType)) {
			return (List<R>) saveAll(instances);
		}

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(resultType);

		Collection<PropertyPath> pps = PropertyFilterSupport.addPropertiesFrom(resultType, commonElementType,
				projectionFactory, neo4jMappingContext);

		List<T> savedInstances = saveAllImpl(instances, new ArrayList<>(pps));

		if (projectionInformation.isClosed()) {
			return savedInstances.stream().map(instance -> projectionFactory.createProjection(resultType, instance))
					.collect(Collectors.toList());
		}

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(commonElementType);
		Neo4jPersistentProperty idProperty = entityMetaData.getIdProperty();

		List<Object> ids = savedInstances.stream().map(savedInstance -> {
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(savedInstance);
			return propertyAccessor.getProperty(idProperty);
		}).collect(Collectors.toList());

		return findAllById(ids, commonElementType)
				.stream().map(instance -> projectionFactory.createProjection(resultType, instance))
				.collect(Collectors.toList());
	}

	@Override
	public <T> void deleteById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		String nameOfParameter = "id";
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

		log.debug(() -> String.format("Deleting entity with id %s ", id));

		Statement statement = cypherGenerator.prepareDeleteOf(entityMetaData, condition);
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
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

		createExecutableQuery(domainType, null, statement, parameters).getSingleResult().orElseThrow(
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
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
				.bind(convertIdValues(entityMetaData.getRequiredIdProperty(), ids))
				.to(nameOfParameter).run();

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

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Statement statement) {
		return createExecutableQuery(domainType, null, statement, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, String cyperQuery) {
		return createExecutableQuery(domainType, null, cyperQuery, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType, Statement statement, Map<String, Object> parameters) {

		return createExecutableQuery(domainType, resultType, renderer.render(statement), TemplateSupport.mergeParameters(statement, parameters));
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, @Nullable Class<?> resultType,  @Nullable String cypherStatement,
			Map<String, Object> parameters) {

		BiFunction<TypeSystem, MapAccessor, ?> mappingFunction = TemplateSupport
				.getAndDecorateMappingFunction(neo4jMappingContext, domainType, resultType);
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
				.withCypherQuery(cypherStatement)
				.withParameters(parameters)
				.usingMappingFunction(mappingFunction)
				.build();

		return toExecutableQuery(preparedQuery);
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
	 */
	private <T> void processRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, T originalInstance, Long internalId,
								   PersistentPropertyAccessor<?> parentPropertyAccessor,
								   boolean isParentObjectNew, PropertyFilter includeProperty) {

		PropertyFilter.RelaxedPropertyPath startingPropertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(neo4jPersistentEntity.getUnderlyingClass());
		processNestedRelations(neo4jPersistentEntity, parentPropertyAccessor, isParentObjectNew,
				new NestedRelationshipProcessingStateMachine(originalInstance, internalId), includeProperty, startingPropertyPath);
	}

	private <T> T processRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, T originalInstance,
			PersistentPropertyAccessor<?> parentPropertyAccessor,
			boolean isParentObjectNew, PropertyFilter includeProperty) {

		PropertyFilter.RelaxedPropertyPath startingPropertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(neo4jPersistentEntity.getUnderlyingClass());
		return processNestedRelations(neo4jPersistentEntity, parentPropertyAccessor, isParentObjectNew,
				new NestedRelationshipProcessingStateMachine(originalInstance), includeProperty, startingPropertyPath);
	}

	private <T> T processNestedRelations(Neo4jPersistentEntity<?> sourceEntity, PersistentPropertyAccessor<?> propertyAccessor,
										 boolean isParentObjectNew, NestedRelationshipProcessingStateMachine stateMachine, PropertyFilter includeProperty, PropertyFilter.RelaxedPropertyPath previousPath) {

		Object fromId = propertyAccessor.getProperty(sourceEntity.getRequiredIdProperty());

		sourceEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) association -> {

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
			boolean dynamicRelationship = relationshipDescription.isDynamic();
			if (!includeProperty.isNotFiltering() && !dynamicRelationship && !includeProperty.contains(currentPropertyPath)) {
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
			if (!isParentObjectNew && !stateMachine.hasProcessedRelationship(fromId, relationshipDescription)) {

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

				neo4jClient.query(renderer.render(relationshipRemoveQuery))
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

			stateMachine.markRelationshipAsProcessed(fromId, relationshipDescription);

			Neo4jPersistentProperty relationshipProperty = association.getInverse();

			RelationshipHandler relationshipHandler = RelationshipHandler.forProperty(relationshipProperty, rawValue);

			for (Object relatedValueToStore : relatedValuesToStore) {

				// here a map entry is not always anymore a dynamic association
				Object relatedObjectBeforeCallbacksApplied = relationshipContext.identifyAndExtractRelationshipTargetNode(relatedValueToStore);
				Neo4jPersistentEntity<?> targetEntity = neo4jMappingContext.getPersistentEntity(relatedObjectBeforeCallbacksApplied.getClass());

				boolean isEntityNew = targetEntity.isNew(relatedObjectBeforeCallbacksApplied);

				Object newRelatedObject = stateMachine.hasProcessedValue(relatedObjectBeforeCallbacksApplied)
						? stateMachine.getProcessedAs(relatedObjectBeforeCallbacksApplied)
						: eventSupport.maybeCallBeforeBind(relatedObjectBeforeCallbacksApplied);

				Long relatedInternalId;
				Entity savedEntity = null;
				// No need to save values if processed
				if (stateMachine.hasProcessedValue(relatedValueToStore)) {
					relatedInternalId = stateMachine.getInternalId(relatedValueToStore);
				} else {
					savedEntity = saveRelatedNode(newRelatedObject, targetEntity, includeProperty, currentPropertyPath);
					relatedInternalId = savedEntity.id();
					stateMachine.markValueAsProcessed(relatedValueToStore, relatedInternalId);
					if (relatedValueToStore instanceof MappingSupport.RelationshipPropertiesWithEntityHolder) {
						Object entity = ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValueToStore).getRelatedEntity();
						stateMachine.markValueAsProcessedAs(entity, relatedInternalId);
					}
				}

				PersistentPropertyAccessor<?> targetPropertyAccessor = targetEntity.getPropertyAccessor(newRelatedObject);
				// if an internal id is used this must be set to link this entity in the next iteration
				if (targetEntity.isUsingInternalIds()) {
					Neo4jPersistentProperty requiredIdProperty = targetEntity.getRequiredIdProperty();
					if (relatedInternalId == null && targetPropertyAccessor.getProperty(requiredIdProperty) != null) {
						relatedInternalId = (Long) targetPropertyAccessor.getProperty(requiredIdProperty);
					} else if (targetPropertyAccessor.getProperty(requiredIdProperty) == null) {
						targetPropertyAccessor.setProperty(requiredIdProperty, relatedInternalId);
					}
				}
				if (savedEntity != null) {
					TemplateSupport.updateVersionPropertyIfPossible(targetEntity, targetPropertyAccessor, savedEntity);
				}
				stateMachine.markValueAsProcessedAs(relatedObjectBeforeCallbacksApplied, targetPropertyAccessor.getBean());
				stateMachine.markRelationshipAsProcessed(relatedInternalId, relationshipDescription.getRelationshipObverse());

				Object idValue = idProperty != null
						? relationshipContext
							.getRelationshipPropertiesPropertyAccessor(relatedValueToStore).getProperty(idProperty)
						: null;

				boolean isNewRelationship = idValue == null;

				CreateRelationshipStatementHolder statementHolder = neo4jMappingContext.createStatement(
						sourceEntity, relationshipContext, relatedValueToStore, isNewRelationship);

				Optional<Long> relationshipInternalId = neo4jClient.query(renderer.render(statementHolder.getStatement()))
						.bind(convertIdValues(sourceEntity.getRequiredIdProperty(), fromId)) //
							.to(Constants.FROM_ID_PARAMETER_NAME) //
						.bind(relatedInternalId) //
							.to(Constants.TO_ID_PARAMETER_NAME) //
						.bind(idValue) //
							.to(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM) //
						.bindAll(statementHolder.getProperties())
						.fetchAs(Long.class).one();

				if (idProperty != null && isNewRelationship) {
					relationshipContext
							.getRelationshipPropertiesPropertyAccessor(relatedValueToStore)
							.setProperty(idProperty, relationshipInternalId.get());
				}

				if (processState != ProcessState.PROCESSED_ALL_VALUES) {
					processNestedRelations(targetEntity, targetPropertyAccessor, isEntityNew, stateMachine, dynamicRelationship ? PropertyFilter.acceptAll() : includeProperty, currentPropertyPath);
				}

				Object potentiallyRecreatedNewRelatedObject = MappingSupport.getRelationshipOrRelationshipPropertiesObject(neo4jMappingContext,
								relationshipDescription.hasRelationshipProperties(),
								relationshipProperty.isDynamicAssociation(),
								relatedValueToStore,
								targetPropertyAccessor);

				relationshipHandler.handle(relatedValueToStore, relatedObjectBeforeCallbacksApplied, potentiallyRecreatedNewRelatedObject);
			}

			relationshipHandler.applyFinalResultToOwner(propertyAccessor);
		});

		return (T) propertyAccessor.getBean();
	}

	private <Y> Entity saveRelatedNode(Object entity, NodeDescription<?> targetNodeDescription, PropertyFilter includeProperty, PropertyFilter.RelaxedPropertyPath currentPropertyPath) {

		DynamicLabels dynamicLabels = determineDynamicLabels(entity, (Neo4jPersistentEntity) targetNodeDescription);
		Class<Y> entityType = (Class<Y>) ((Neo4jPersistentEntity<?>) targetNodeDescription).getType();
		Function<Y, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(entityType);
		binderFunction = binderFunction.andThen(tree -> {
			Map<String, Object> properties = (Map<String, Object>) tree.get(Constants.NAME_OF_PROPERTIES_PARAM);

			if (!includeProperty.isNotFiltering()) {
				properties.entrySet().removeIf(e -> !includeProperty.contains(currentPropertyPath.append(e.getKey())));
			}
			return tree;
		});
		Optional<Entity> optionalSavedNode = neo4jClient
				.query(() -> renderer.render(cypherGenerator.prepareSaveOf(targetNodeDescription, dynamicLabels)))
				.bind((Y) entity).with(binderFunction)
				.fetchAs(Entity.class)
				.one();

		if (((Neo4jPersistentEntity) targetNodeDescription).hasVersionProperty() && !optionalSavedNode.isPresent()) {
			throw new OptimisticLockingFailureException(OPTIMISTIC_LOCKING_ERROR_MESSAGE);
		}

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
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(Class<T> domainType,
													QueryFragmentsAndParameters queryFragmentsAndParameters) {

		return createExecutableQuery(domainType, null, queryFragmentsAndParameters);
	}


	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Class<?> resultType,
 			QueryFragmentsAndParameters queryFragmentsAndParameters) {

		BiFunction<TypeSystem, MapAccessor, ?> mappingFunction = TemplateSupport
				.getAndDecorateMappingFunction(neo4jMappingContext, domainType, resultType);
		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
				.withQueryFragmentsAndParameters(queryFragmentsAndParameters)
				.usingMappingFunction(mappingFunction)
				.build();
		return toExecutableQuery(preparedQuery);
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		return new DefaultExecutableQuery<>(preparedQuery);
	}

	@Override
	public <T> ExecutableSave<T> save(Class<T> domainType) {

		return new FluentOperationSupport(this).save(domainType);
	}

	<T, R> List<R> doSave(Iterable<R> instances, Class<T> domainType) {
		// empty check
		if (!instances.iterator().hasNext()) {
			return Collections.emptyList();
		}

		Class<?> resultType = TemplateSupport.findCommonElementType(instances);

		Collection<PropertyPath> pps = PropertyFilterSupport.addPropertiesFrom(resultType, domainType,
				projectionFactory, neo4jMappingContext);

		List<R> results = new ArrayList<>();
		for (R instance : instances) {
			EntityFromDtoInstantiatingConverter<T> converter = new EntityFromDtoInstantiatingConverter<>(domainType, neo4jMappingContext);
			T domainObject = converter.convert(instance);

			T savedEntity = saveImpl(domainObject, pps);

			R convertedBack = (R) new DtoInstantiatingConverter(resultType, neo4jMappingContext).convertDirectly(savedEntity);
			results.add(convertedBack);
		}
		return results;
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
			Map<String, Object> finalParameters = queryFragmentsAndParameters.getParameters();

			QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();
			Neo4jPersistentEntity<?> entityMetaData = (Neo4jPersistentEntity<?>) queryFragmentsAndParameters.getNodeDescription();

			boolean containsPossibleCircles = entityMetaData != null && entityMetaData.containsPossibleCircles(queryFragments::includeField);
			if (cypherQuery == null || containsPossibleCircles) {

				if (containsPossibleCircles && !queryFragments.isScalarValueReturn()) {
					NodesAndRelationshipsByIdStatementProvider nodesAndRelationshipsById =
							createNodesAndRelationshipsByIdStatementProvider(entityMetaData, queryFragments, queryFragmentsAndParameters.getParameters());

					if (nodesAndRelationshipsById.hasRootNodeIds()) {
						return Optional.empty();
					}
					cypherQuery = renderer.render(nodesAndRelationshipsById.toStatement());
					finalParameters = nodesAndRelationshipsById.getParameters();
				} else {
					Statement statement = queryFragments.toStatement();
					cypherQuery = renderer.render(statement);
					finalParameters = TemplateSupport.mergeParameters(statement, finalParameters);
				}
			}

			Neo4jClient.MappingSpec<T> newMappingSpec = neo4jClient.query(cypherQuery)
					.bindAll(finalParameters).fetchAs(preparedQuery.getResultType());
			return Optional.of(preparedQuery.getOptionalMappingFunction()
					.map(f -> newMappingSpec.mappedBy(f)).orElse(newMappingSpec));
		}

		private NodesAndRelationshipsByIdStatementProvider createNodesAndRelationshipsByIdStatementProvider(Neo4jPersistentEntity<?> entityMetaData,
						   QueryFragments queryFragments, Map<String, Object> parameters) {

			// first check if the root node(s) exist(s) at all
			Statement rootNodesStatement = cypherGenerator
					.prepareMatchOf(entityMetaData, queryFragments.getMatchOn(), queryFragments.getCondition())
					.returning(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE).build();

			Map<String, Object> usedParameters = new HashMap<>(parameters);
			usedParameters.putAll(rootNodesStatement.getParameters());
			final Collection<Long> rootNodeIds = new HashSet<>((Collection<Long>) neo4jClient
					.query(renderer.render(rootNodesStatement))
					.bindAll(usedParameters)
					.fetch()
					.one()
					.map(values -> values.get(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE))
					.get());

			if (rootNodeIds.isEmpty()) {
				// fast return if no matching root node(s) are found
				return NodesAndRelationshipsByIdStatementProvider.EMPTY;
			}
			// load first level relationships
			final Set<Long> relationshipIds = new HashSet<>();
			final Set<Long> relatedNodeIds = new HashSet<>();

			for (RelationshipDescription relationshipDescription : entityMetaData.getRelationshipsInHierarchy(fieldName -> queryFragments.includeField(fieldName))) {

				Statement statement = cypherGenerator
						.prepareMatchOf(entityMetaData, relationshipDescription, queryFragments.getMatchOn(), queryFragments.getCondition())
						.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

				usedParameters = new HashMap<>(parameters);
				usedParameters.putAll(statement.getParameters());
				neo4jClient.query(renderer.render(statement))
						.bindAll(parameters)
						.fetch()
						.one()
						.ifPresent(iterateAndMapNextLevel(relationshipIds, relatedNodeIds, relationshipDescription));
			}

			return new NodesAndRelationshipsByIdStatementProvider(rootNodeIds, relationshipIds, relatedNodeIds, queryFragments);
		}

		private void iterateNextLevel(Collection<Long> nodeIds, Neo4jPersistentEntity<?> target, Set<Long> relationshipIds,
									  Set<Long> relatedNodeIds) {

			Collection<RelationshipDescription> relationships = target.getRelationshipsInHierarchy(preparedQuery.getQueryFragmentsAndParameters().getQueryFragments()::includeField);
			for (RelationshipDescription relationshipDescription : relationships) {

				Node node = anyNode(Constants.NAME_OF_ROOT_NODE);

				Statement statement = cypherGenerator
						.prepareMatchOf(target, relationshipDescription, null,
								Functions.id(node).in(Cypher.parameter(Constants.NAME_OF_IDS)))
						.returning(cypherGenerator.createGenericReturnStatement()).build();

				neo4jClient.query(renderer.render(statement))
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
