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

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.TypeSystem;
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
import org.springframework.data.neo4j.repository.query.QueryFragments;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.asterisk;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

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
		return createExecutableQuery(domainType, cypherGenerator.createReturnStatementForMatch(entityMetaData)).getResults();
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

		Map<String, Object> parameters = Collections
				.singletonMap(Constants.NAME_OF_ID, convertIdValues(entityMetaData.getRequiredIdProperty(), id));

		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID));
		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.setMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpression(returnStatement);
		QueryFragmentsAndParameters f = new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
		return createExecutableQuery(domainType, f).getSingleResult();
	}

	private FinalQueryAndParameters bimsUndBums(Neo4jPersistentEntity<?> entityMetaData, QueryFragments queryFragments, Map<String, Object> parameters) {
		// load first level relationships
		final Collection<Long> rootNodeIds = new HashSet<>();
		Set<Long> relationshipIds = new HashSet<>();
		Set<Long> relatedNodeIds = new HashSet<>();

		for (RelationshipDescription relationship : entityMetaData.getRelationships()) {
			if (queryFragments.getReturnTuple() != null && !queryFragments.getReturnTuple().getIncludedProperties().isEmpty() && queryFragments.getReturnTuple().getIncludedProperties().contains(relationship.getFieldName())) {
				continue;
			}

			Statement statement = cypherGenerator.prepareMatchOf(entityMetaData, relationship, queryFragments.getMatchOn(), queryFragments.getCondition())
					.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData)).build();

			Optional<IntermediateQueryResult> one = neo4jClient.query(renderer.render(statement)).in(getDatabaseName())
					.bindAll(parameters)
					.fetchAs(IntermediateQueryResult.class)
					.mappedBy(mapToIntermediateResult(relationshipIds, relatedNodeIds, relationship))
					.one();

			IntermediateQueryResult v = one.get();
			if (v.equals(IntermediateQueryResult.NO_RESULT)) {
				return FinalQueryAndParameters.NO_RESULT;
			}

			if (rootNodeIds.isEmpty()) {
				rootNodeIds.addAll(v.rootNodeIds);
			}

		}

		Node chef = Cypher.anyNode("chef");
		Node rest = Cypher.anyNode("rest");
		Relationship relationship = Cypher.anyNode().relationshipBetween(Cypher.anyNode()).named("anyRel");
		String chefIds = "chefIds";
		String relIds = "relIds";
		String restIds = "restIds";
		Statement statement = Cypher.match(chef)
				.where(Functions.id(chef).in(Cypher.parameter(chefIds)))
				.optionalMatch(relationship).where(Functions.id(relationship).in(Cypher.parameter(relIds)))
				.optionalMatch(rest).where(Functions.id(rest).in(Cypher.parameter(restIds)))
				.returning(
						chef.as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE),
						Functions.collectDistinct(relationship).as(Constants.NAME_OF_SYNTHESIZED_RELATIONS),
						Functions.collectDistinct(rest).as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES)
				).build();

		Map<String, Object> bindableParameters = new HashMap<>();
		bindableParameters.put(chefIds, rootNodeIds);
		bindableParameters.put(relIds, relationshipIds);
		bindableParameters.put(restIds, relatedNodeIds);

		return new FinalQueryAndParameters(statement, bindableParameters);
	}

	private void ding(Collection<Long> nodeIds, Neo4jPersistentEntity<?> target, Set<Long> relationshipIds, Set<Long> relatedNodeIds) {

		Collection<RelationshipDescription> relationships = target.getRelationships();
		for (RelationshipDescription relationshipDescription : relationships) {

			Node node = anyNode(Constants.NAME_OF_ROOT_NODE);

			Statement statement = cypherGenerator
					.prepareMatchOf(target, relationshipDescription, null,
							Functions.id(node).in(Cypher.parameter(Constants.NAME_OF_ID)))
					.returning(cypherGenerator.createGenericReturnStatement()).build();
			Optional<IntermediateQueryResult> one = neo4jClient.query(renderer.render(statement)).in(getDatabaseName())
					.bindAll(Collections.singletonMap(Constants.NAME_OF_ID, nodeIds))
					.fetchAs(IntermediateQueryResult.class)
					.mappedBy(mapToIntermediateResult(relationshipIds, relatedNodeIds, relationshipDescription))
					.one();

			one.ifPresent(v -> {
				relationshipIds.addAll(v.relationshipIds);
			});
		}

	}

	@NotNull
	private BiFunction<TypeSystem, Record, IntermediateQueryResult> mapToIntermediateResult(Set<Long> relationshipIds, Set<Long> relatedNodeIds, RelationshipDescription relationshipDescription) {
		return (typeSystem, record) -> {
			List<Long> rootIds = record.get(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE).asList(Value::asLong);
			if (rootIds.isEmpty()) {
				return IntermediateQueryResult.NO_RESULT;
			}
			IntermediateQueryResult intermediateQueryResult = new IntermediateQueryResult(rootIds);
			Value value1 = record.get(Constants.NAME_OF_SYNTHESIZED_RELATIONS);
			if (!Values.NULL.equals(value1)) {
				relationshipIds.addAll(value1.asList(Value::asLong));
			}
			Value value = record.get(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES);
			if (!Values.NULL.equals(value)) {
				Set<Long> relatedIds = new HashSet<>(value.asList(Value::asLong));
				intermediateQueryResult.relatedNodeIds.addAll(relatedIds);
				// use this list to get down the road
				// 1. remove already visited ones;
				relatedIds.removeAll(relatedNodeIds);
				relatedNodeIds.addAll(relatedIds);
				// 2. for the rest start the exploration
				if (!relatedIds.isEmpty()) {
					ding(relatedIds, (Neo4jPersistentEntity<?>) relationshipDescription.getTarget(), relationshipIds, relatedNodeIds);
				}
			}

			return intermediateQueryResult;
		};
	}

	public static class FinalQueryAndParameters {

		private final Statement statement;
		private final Map<String, Object> parameters;

		final static FinalQueryAndParameters NO_RESULT = new FinalQueryAndParameters(null, null);

		public FinalQueryAndParameters(Statement statement, Map<String, Object> parameters) {
			this.statement = statement;
			this.parameters = parameters;
		}
	}

	private static class IntermediateQueryResult {
		private final Set<Long> rootNodeIds = new HashSet<>();
		private final Set<Long> relationshipIds = new HashSet<>();
		private final Set<Long> relatedNodeIds = new HashSet<>();

		static final IntermediateQueryResult NO_RESULT = new IntermediateQueryResult();

		private IntermediateQueryResult() { }

		private IntermediateQueryResult(Collection<Long> rootNodeIds) {
			this.rootNodeIds.addAll(rootNodeIds);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			IntermediateQueryResult that = (IntermediateQueryResult) o;
			return rootNodeIds.equals(that.rootNodeIds) && relationshipIds.equals(that.relationshipIds)
					&& relatedNodeIds.equals(that.relatedNodeIds);
		}

		@Override
		public int hashCode() {
			return Objects.hash(rootNodeIds, relationshipIds, relatedNodeIds);
		}
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> domainType) {
		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);

		Map<String, Object> parameters = Collections
				.singletonMap(Constants.NAME_OF_IDS, convertIdValues(entityMetaData.getRequiredIdProperty(), ids));

		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.setMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(entityMetaData.getIdExpression().in((parameter(Constants.NAME_OF_IDS))));
		queryFragments.setReturnExpression(returnStatement);
		QueryFragmentsAndParameters f = new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
		return createExecutableQuery(domainType, f).getResults();
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
		if (!entityMetaData.isUsingInternalIds()) {
			processRelations(entityMetaData, entityToBeSaved, isEntityNew, inDatabase);
			return entityToBeSaved;
		} else {
			propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), optionalInternalId.get());
			processRelations(entityMetaData, entityToBeSaved, isEntityNew, inDatabase);

			return propertyAccessor.getBean();
		}
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

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(nameOfParameter, convertIdValues(entityMetaData.getRequiredIdProperty(), id));
		parameters.put(Constants.NAME_OF_VERSION_PARAM, versionValue);

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpression(new Expression[]{Constants.NAME_OF_ROOT_NODE});

		QueryFragmentsAndParameters f = new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
		createExecutableQuery(domainType, f).getSingleResult().orElseThrow(
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

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Expression[] returnStatement) {
		return createExecutableQuery(domainType, returnStatement, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Statement statement) {
		return createExecutableQuery(domainType, statement, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, String cypherStatement) {
		return createExecutableQuery(domainType, cypherStatement, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Expression[] returnStatement,
														 Map<String, Object> parameters) {
		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.setMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(Conditions.noCondition());
		queryFragments.setReturnExpression(returnStatement);
		QueryFragmentsAndParameters f = new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
		return createExecutableQuery(domainType, f);
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, QueryFragmentsAndParameters queryFragmentsAndParameters) {

		Neo4jPersistentEntity<?> entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();
		Map<String, Object> parameters = queryFragmentsAndParameters.getParameters();

		if (entityMetaData.containsPossibleCircles(Collections.emptyList())) {
			FinalQueryAndParameters f = bimsUndBums(entityMetaData, queryFragments, parameters);
			if (f.equals(FinalQueryAndParameters.NO_RESULT)) {
				// todo empty result :(
				return (ExecutableQuery<T>) new DefaultExecutableQuery<>(null, null).EMPTY;
			}
			return createExecutableQuery(domainType, renderer.render(f.statement), f.parameters);
		}

		Statement statement = cypherGenerator.generateQuery(queryFragments);

		return createExecutableQuery(domainType, renderer.render(statement), parameters);
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

	private void processRelations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject,
			boolean isParentObjectNew, @Nullable String inDatabase) {

		processNestedRelations(neo4jPersistentEntity, parentObject, isParentObjectNew, inDatabase,
				new NestedRelationshipProcessingStateMachine());
	}

	private void processNestedRelations(Neo4jPersistentEntity<?> sourceEntity, Object parentObject,
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

				// if an internal id is used this must get set to link this entity in the next iteration
				if (targetEntity.isUsingInternalIds()) {
					PersistentPropertyAccessor<?> targetPropertyAccessor = targetEntity.getPropertyAccessor(relatedNode);
					targetPropertyAccessor.setProperty(targetEntity.getRequiredIdProperty(), relatedInternalId);
				}
				if (processState != ProcessState.PROCESSED_ALL_VALUES) {
					processNestedRelations(targetEntity, relatedNode, isEntityNew, inDatabase, stateMachine);
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

		this.eventSupport = EventSupport.discoverCallbacks(neo4jMappingContext, beanFactory);
	}

	@Override
	public <T> ExecutableQuery<T> findByExample(Class<T> domainType, QueryFragmentsAndParameters f) {
		return createExecutableQuery(domainType, f);
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		QueryFragmentsAndParameters queryFragmentsAndParameters = preparedQuery.getQueryFragmentsAndParameters();
		String cypherQuery = queryFragmentsAndParameters.getCypherQuery();
		Map<String, Object> finalParameters = preparedQuery.getQueryFragmentsAndParameters().getParameters();

		Neo4jClient.MappingSpec<T> mappingSpec = this.neo4jClient.query(cypherQuery)
				.in(getDatabaseName()).bindAll(finalParameters).fetchAs(preparedQuery.getResultType());
		Neo4jClient.RecordFetchSpec<T> fetchSpec = preparedQuery.getOptionalMappingFunction()
				.map(f -> mappingSpec.mappedBy(f)).orElse(mappingSpec);

		QueryFragments queryFragments = queryFragmentsAndParameters.getQueryFragments();
		Neo4jPersistentEntity<?> entityMetaData = (Neo4jPersistentEntity<?>) queryFragmentsAndParameters.getNodeDescription();

		QueryFragments.ReturnTuple returnTuple = queryFragments.getReturnTuple();
		boolean containsPossibleCircles = entityMetaData != null && entityMetaData.containsPossibleCircles(
				returnTuple != null
						? returnTuple.getIncludedProperties()
						: Collections.emptyList());
		if (cypherQuery == null || containsPossibleCircles) {

			Map<String, Object> parameters = queryFragmentsAndParameters.getParameters();

			if (containsPossibleCircles) {
				FinalQueryAndParameters f = bimsUndBums(entityMetaData, queryFragments, parameters);
				if (f.equals(FinalQueryAndParameters.NO_RESULT)) {
					// todo empty result :(
					return new DefaultExecutableQuery<>(preparedQuery, fetchSpec).EMPTY;
				}
				cypherQuery = renderer.render(f.statement);
				finalParameters = f.parameters;
			} else {
				cypherQuery = renderer.render(cypherGenerator.generateQuery(queryFragments));
			}

		}

		Neo4jClient.MappingSpec<T> newMappingSpec = this.neo4jClient.query(cypherQuery)
				.in(getDatabaseName()).bindAll(finalParameters).fetchAs(preparedQuery.getResultType());
		fetchSpec = preparedQuery.getOptionalMappingFunction()
				.map(f -> newMappingSpec.mappedBy(f)).orElse(newMappingSpec);

		return new DefaultExecutableQuery<>(preparedQuery, fetchSpec);
	}

	final class DefaultExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;
		private final Neo4jClient.RecordFetchSpec<T> fetchSpec;
		final ExecutableQuery<T> EMPTY = new ExecutableQuery<T>() {

			@Override
			public List<T> getResults() {
				return Collections.emptyList();
			}

			@Override
			public Optional<T> getSingleResult() {
				return Optional.empty();
			}

			@Override
			public T getRequiredSingleResult() {
				return getSingleResult().orElseThrow(() -> new NoResultException(1, preparedQuery.getQueryFragmentsAndParameters().getCypherQuery()));
			}
		};

		DefaultExecutableQuery(PreparedQuery<T> preparedQuery, Neo4jClient.RecordFetchSpec<T> fetchSpec) {
			this.preparedQuery = preparedQuery;
			this.fetchSpec = fetchSpec;
		}

		@SuppressWarnings("unchecked")
		public List<T> getResults() {

			Collection<T> all = fetchSpec.all();
			if (preparedQuery.resultsHaveBeenAggregated()) {
				return all.stream().flatMap(nested -> ((Collection<T>) nested).stream()).distinct().collect(Collectors.toList());
			}
			return all.stream().collect(Collectors.toList());
		}

		public Optional<T> getSingleResult() {
			try {
				Optional<T> one = fetchSpec.one();
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
			Optional<T> one = fetchSpec.one();
			if (preparedQuery.resultsHaveBeenAggregated()) {
				one = one.map(aggregatedResults -> (T) ((LinkedHashSet<?>) aggregatedResults).iterator().next());
			}
			return one.orElseThrow(() -> new NoResultException(1, preparedQuery.getQueryFragmentsAndParameters().getCypherQuery()));
		}
	}
}
