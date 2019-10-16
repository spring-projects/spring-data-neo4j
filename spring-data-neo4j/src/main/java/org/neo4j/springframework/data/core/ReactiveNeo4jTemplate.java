/*
 * Copyright (c) 2019 "Neo4j,"
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
import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.schema.CypherGenerator.*;
import static org.neo4j.springframework.data.core.schema.NodeDescription.*;
import static org.neo4j.springframework.data.core.support.Relationships.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.driver.exceptions.NoSuchRecordException;
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
import org.neo4j.springframework.data.repository.event.ReactiveBeforeBindCallback;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public final class ReactiveNeo4jTemplate implements ReactiveNeo4jOperations, BeanFactoryAware {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(ReactiveNeo4jTemplate.class));

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jMappingContext neo4jMappingContext;

	private final CypherGenerator statementBuilder;

	private ReactiveNeo4jEvents eventSupport;

	public ReactiveNeo4jTemplate(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {

		Assert.notNull(neo4jClient, "The Neo4jClient is required");
		Assert.notNull(neo4jMappingContext, "The Neo4jMappingContext is required");

		this.neo4jClient = neo4jClient;
		this.neo4jMappingContext = neo4jMappingContext;
		this.statementBuilder = CypherGenerator.INSTANCE;
		this.eventSupport = new ReactiveNeo4jEvents(null);
	}

	@Override
	public Mono<Long> count(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(Functions.count(asterisk())).build();

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(renderer.render(statement))
			.build();
		return this.toExecutableQuery(preparedQuery).getSingleResult();
	}

	@Override
	public Mono<Long> count(Statement statement, Map<String, Object> parameters) {

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.build();
		return this.toExecutableQuery(preparedQuery).getSingleResult();
	}

	@Override
	public <T> Flux<T> findAll(Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(statementBuilder.createReturnStatementForMatch(entityMetaData)).build();
		return createExecutableQuery(domainType, statement).getResults();
	}

	@Override
	public <T> Flux<T> findAll(Statement statement, Class<T> domainType) {

		return createExecutableQuery(domainType, statement).getResults();
	}

	@Override public <T> Flux<T> findAll(Statement statement, Map<String, Object> parameters, Class<T> domainType) {

		return createExecutableQuery(domainType, statement, parameters).getResults();
	}

	@Override
	public <T> Mono<T> findOne(Statement statement, Map<String, Object> parameters, Class<T> domainType) {

		return createExecutableQuery(domainType, statement, parameters).getSingleResult();
	}

	@Override
	public <T> Mono<T> findById(Object id, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = statementBuilder
			.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().isEqualTo(literalOf(id)))
			.returning(statementBuilder.createReturnStatementForMatch(entityMetaData))
			.build();
		return createExecutableQuery(domainType, statement).getSingleResult();
	}

	@Override
	public <T> Flux<T> findAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = statementBuilder
			.prepareMatchOf(entityMetaData, entityMetaData.getIdExpression().in((parameter("ids"))))
			.returning(statementBuilder.createReturnStatementForMatch(entityMetaData))
			.build();
		return createExecutableQuery(domainType, statement, singletonMap("ids", ids)).getResults();
	}

	@Override
	public <T> Mono<T> save(T instance) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(instance.getClass());
		return Mono.just(instance)
			.flatMap(eventSupport::maybeCallBeforeBind)
			.flatMap(entity -> {
				Statement saveStatement = statementBuilder.prepareSaveOf(entityMetaData);

				Mono<Long> idMono =
					this.neo4jClient.query(() -> renderer.render(saveStatement))
						.bind((T) entity)
						.with(neo4jMappingContext.getRequiredBinderFunctionFor((Class<T>) entity.getClass()))
						.fetchAs(Long.class).one();

				if (!entityMetaData.isUsingInternalIds()) {
					return idMono.then(processNestedAssociations(entityMetaData, entity))
						.thenReturn(entity);
				} else {
					return idMono.map(internalId -> {
						PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entity);
						propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), internalId);

						return propertyAccessor.getBean();
					}).flatMap(savedEntity -> processNestedAssociations(entityMetaData, savedEntity)
						.thenReturn(savedEntity));
				}
			});
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

		if (entityMetaData.isUsingInternalIds()) {
			log.debug("Saving entities using single statements.");

			return Flux.fromIterable(entities).flatMap(this::save);
		}

		Function<T, Map<String, Object>> binderFunction = neo4jMappingContext.getRequiredBinderFunctionFor(domainClass);
		return Flux.fromIterable(entities)
			.flatMap(eventSupport::maybeCallBeforeBind)
			.collectList()
			.flatMapMany(
				entitiesToBeSaved -> Mono
					.defer(() -> { // Defer the actual save statement until the previous flux completes
						List<Map<String, Object>> boundedEntityList = entitiesToBeSaved.stream()
							.map(binderFunction)
							.collect(toList());

						return neo4jClient
							.query(() -> renderer
								.render(statementBuilder.prepareSaveOfMultipleInstancesOf(entityMetaData)))
							.bind(boundedEntityList).to(NAME_OF_ENTITY_LIST_PARAM).run();
					})
					.doOnNext(resultSummary -> {
						SummaryCounters counters = resultSummary.counters();
						log.debug(() -> String.format(
							"Created %d and deleted %d nodes, created %d and deleted %d relationships and set %d properties.",
							counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
							counters.relationshipsDeleted(), counters.propertiesSet()));
					})
					.thenMany(Flux.fromIterable(entitiesToBeSaved))
			);
	}

	@Override
	public <T> Mono<Void> deleteAllById(Iterable<?> ids, Class<T> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		String nameOfParameter = "ids";
		Condition condition = entityMetaData.getIdExpression().in(parameter(nameOfParameter));

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, condition);
		return this.neo4jClient.query(() -> renderer.render(statement)).bind(ids).to(nameOfParameter).run().then();
	}

	@Override
	public <T> Mono<Void> deleteById(Object id, Class<T> domainType) {

		Assert.notNull(id, "The given id must not be null!");

		String nameOfParameter = "id";
		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(nameOfParameter));

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, condition);
		return this.neo4jClient.query(() -> renderer.render(statement))
			.bind(id).to(nameOfParameter).run().then();
	}

	@Override
	public Mono<Void> deleteAll(Class<?> domainType) {

		Neo4jPersistentEntity entityMetaData = neo4jMappingContext.getPersistentEntity(domainType);
		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData);
		return this.neo4jClient.query(() -> renderer.render(statement)).run().then();
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Statement statement) {
		return createExecutableQuery(domainType, statement, Collections.emptyMap());
	}

	private <T> ExecutableQuery<T> createExecutableQuery(Class<T> domainType, Statement statement,
		Map<String, Object> parameters) {

		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(domainType)
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.usingMappingFunction(this.neo4jMappingContext.getRequiredMappingFunctionFor(domainType))
			.build();
		return this.toExecutableQuery(preparedQuery);
	}

	private Mono<Void> processNestedAssociations(Neo4jPersistentEntity<?> neo4jPersistentEntity, Object parentObject) {

		return Mono.defer(() -> {
			PersistentPropertyAccessor<?> propertyAccessor = neo4jPersistentEntity.getPropertyAccessor(parentObject);
			Object fromId = propertyAccessor.getProperty(neo4jPersistentEntity.getRequiredIdProperty());
			List<Mono<Void>> relationshipCreationMonos = new ArrayList<>();

			neo4jPersistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) handler -> {

				Neo4jPersistentProperty inverse = handler.getInverse();

				Object value = propertyAccessor.getProperty(inverse);

				Collection<RelationshipDescription> relationships = neo4jPersistentEntity.getRelationships();

				Class<?> associationTargetType = inverse.getAssociationTargetType();

				Neo4jPersistentEntity<?> targetNodeDescription = (Neo4jPersistentEntity<?>) neo4jMappingContext
					.getRequiredNodeDescription(associationTargetType);

				RelationshipDescription relationship = relationships.stream()
					.filter(r -> r.getFieldName().equals(inverse.getName()))
					.findFirst().get();

				// remove all relationships before creating all new if the entity is not new
				// this avoids the usage of cache but might have significant impact on overall performance
				if (!neo4jPersistentEntity.isNew(parentObject)) {
					Statement relationshipRemoveQuery = statementBuilder
						.createRelationshipRemoveQuery(neo4jPersistentEntity, relationship,
							targetNodeDescription.getPrimaryLabel());
					relationshipCreationMonos.add(
						neo4jClient.query(renderer.render(relationshipRemoveQuery))
							.bind(fromId).to(FROM_ID_PARAMETER_NAME)
							.run().then());
				}

				if (value == null) {
					return;
				}

				for (Object relatedValue : unifyRelationshipValue(inverse, value)) {

					Mono<Object> valueToBeSavedMono = eventSupport
						.maybeCallBeforeBind(relatedValue instanceof Map.Entry ?
							((Map.Entry) relatedValue).getValue() : relatedValue);

					relationshipCreationMonos.add(
						valueToBeSavedMono
							.flatMap(valueToBeSaved ->
								saveRelatedNode(valueToBeSaved, associationTargetType, targetNodeDescription)
									.flatMap(relatedInternalId -> {

										// if an internal id is used this must get set to link this entity in the next iteration
										if (targetNodeDescription.isUsingInternalIds()) {
											PersistentPropertyAccessor<?> targetPropertyAccessor = targetNodeDescription
												.getPropertyAccessor(valueToBeSaved);
											targetPropertyAccessor
												.setProperty(targetNodeDescription.getRequiredIdProperty(),
													relatedInternalId);
										}
										Statement relationshipCreationQuery = statementBuilder
											.createRelationshipCreationQuery(
												neo4jPersistentEntity, relationship,
												relatedValue instanceof Map.Entry ?
													((Map.Entry<String, ?>) relatedValue).getKey() :
													null,
												relatedInternalId);

										return
											neo4jClient.query(renderer.render(relationshipCreationQuery))
												.bind(fromId).to(FROM_ID_PARAMETER_NAME)
												.run()
												.then(processNestedAssociations(targetNodeDescription, valueToBeSaved));
									})));
				}
			});

			return Flux.concat(relationshipCreationMonos).then();
		});
	}

	private <Y> Mono<Long> saveRelatedNode(Object entity, Class<Y> entityType, NodeDescription targetNodeDescription) {
		return neo4jClient.query(() -> renderer.render(statementBuilder.prepareSaveOf(targetNodeDescription)))
			.bind((Y) entity)
			.with(neo4jMappingContext.getRequiredBinderFunctionFor(entityType)).fetchAs(Long.class).one();
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		Class<T> resultType = preparedQuery.getResultType();
		ReactiveNeo4jClient.MappingSpec<T> mappingSpec = this
			.neo4jClient.query(preparedQuery.getCypherQuery())
			.bindAll(preparedQuery.getParameters())
			.fetchAs(resultType);

		ReactiveNeo4jClient.RecordFetchSpec<T> fetchSpec = preparedQuery
			.getOptionalMappingFunction()
			.map(mappingFunction -> mappingSpec.mappedBy(mappingFunction))
			.orElse(mappingSpec);

		return new DefaultReactiveExecutableQuery<>(fetchSpec);
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
	 * Utility class that orchestrates {@link ReactiveEntityCallbacks}.
	 * All the methods provided here check for their availability and do nothing when an event cannot be published.
	 */
	final class ReactiveNeo4jEvents {

		private final @Nullable ReactiveEntityCallbacks entityCallbacks;

		ReactiveNeo4jEvents(@Nullable ReactiveEntityCallbacks entityCallbacks) {
			this.entityCallbacks = entityCallbacks;
		}

		<T> Mono<T> maybeCallBeforeBind(T object) {

			if (entityCallbacks != null) {
				return entityCallbacks.callback(ReactiveBeforeBindCallback.class, object);
			}

			return Mono.just(object);
		}
	}
}
