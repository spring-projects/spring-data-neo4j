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
package org.springframework.data.neo4j.repository.support;

import static java.util.Collections.*;
import static lombok.AccessLevel.*;
import static org.springframework.data.neo4j.core.cypher.Cypher.*;
import static org.springframework.data.neo4j.core.schema.NodeDescription.*;
import static org.springframework.data.neo4j.repository.query.CypherAdapterUtils.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.MatchMode;
import org.springframework.data.domain.ExampleMatcher.PropertyValueTransformer;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.cypher.Condition;
import org.springframework.data.neo4j.core.cypher.Conditions;
import org.springframework.data.neo4j.core.cypher.Cypher;
import org.springframework.data.neo4j.core.cypher.Expression;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.Statement;
import org.springframework.data.neo4j.core.cypher.SymbolicName;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.cypher.renderer.Renderer;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.GraphPropertyDescription;
import org.springframework.data.neo4j.core.schema.IdDescription;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.support.ExampleMatcherAccessor;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository base implementation for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Repository
@Transactional(readOnly = true)
@Slf4j
class SimpleReactiveNeo4jRepository<T, ID> implements ReactiveNeo4jRepository<T, ID> {

	private static final Renderer renderer = CypherRenderer.create();

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jMappingContext mappingContext;

	private final Class<T> nodeClass;

	private final NodeDescription<T> nodeDescription;
	private final BiFunction<TypeSystem, Record, ?> mappingFunction;

	private final SchemaBasedStatementBuilder statementBuilder;

	private final Expression idExpression;

	SimpleReactiveNeo4jRepository(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext mappingContext, Class<T> nodeClass) {

		this.neo4jClient = neo4jClient;
		this.mappingContext = mappingContext;
		this.nodeClass = nodeClass;

		this.nodeDescription = (NodeDescription<T>) mappingContext.getRequiredNodeDescription(nodeClass);
		this.mappingFunction = mappingContext.getRequiredMappingFunctionFor(nodeClass);

		this.statementBuilder = createSchemaBasedStatementBuilder(mappingContext);

		final SymbolicName rootNode = Cypher.symbolicName("n");
		final IdDescription idDescription = this.nodeDescription.getIdDescription();
		switch (idDescription.getIdStrategy()) {
			case INTERNAL:
				idExpression = Functions.id(rootNode);
				break;
			case ASSIGNED:
			case GENERATED:
				idExpression = idDescription.getOptionalGraphPropertyName()
					.map(propertyName -> property(rootNode.getName(), propertyName)).get();
				break;
			default:
				throw new IllegalStateException("Unsupported ID strategy: %s" + idDescription.getIdStrategy());
		}
	}

	@Override
	public <S extends T> Mono<S> findOne(Example<S> example) {
		Predicate predicate = createPredicate(example);
		Statement statement = statementBuilder
			.prepareMatchOf(predicate.nodeDescription, Optional.of(predicate.condition))
			.returning(asterisk())
			.build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.parameters).getSingleResult();
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example) {
		Predicate predicate = createPredicate(example);
		Statement statement = statementBuilder
			.prepareMatchOf(predicate.nodeDescription, Optional.of(predicate.condition))
			.returning(asterisk())
			.build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.parameters).getResults();

	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example, Sort sort) {
		Predicate predicate = createPredicate(example);
		Statement statement = statementBuilder
			.prepareMatchOf(predicate.nodeDescription, Optional.of(predicate.condition))
			.returning(asterisk())
			.orderBy(toSortItems(nodeDescription, sort)).build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.parameters).getResults();
	}

	@Override
	public <S extends T> Mono<Long> count(Example<S> example) {
		Predicate predicate = createPredicate(example);
		Statement statement = statementBuilder
			.prepareMatchOf(predicate.nodeDescription, Optional.of(predicate.condition))
			.returning(Functions.count(asterisk()))
			.build();

		return createExecutableQuery(Long.class, statement, predicate.parameters).getSingleResult();

	}

	@Override
	public Mono<T> findById(ID id) {
		Statement statement = statementBuilder
			.prepareMatchOf(nodeDescription, Optional.of(idExpression.isEqualTo(literalOf(id))))
			.returning(asterisk())
			.build();
		return createExecutableQuery(statement).getSingleResult();
	}

	@Override
	public Mono<T> findById(Publisher<ID> idPublisher) {
		return Mono.from(idPublisher).flatMap(this::findById);
	}

	@Override
	public Flux<T> findAll(Sort sort) {
		Statement statement = statementBuilder.prepareMatchOf(nodeDescription, Optional.empty())
			.returning(asterisk())
			.orderBy(toSortItems(nodeDescription, sort))
			.build();

		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Mono<Boolean> existsById(ID id) {
		return findById(id).hasElement();
	}

	@Override
	public <S extends T> Mono<Boolean> exists(Example<S> example) {
		return findAll(example).hasElements();
	}

	@Override
	public Mono<Boolean> existsById(Publisher<ID> idPublisher) {
		return Mono.from(idPublisher).flatMap(this::existsById);
	}

	@Override
	public Flux<T> findAll() {
		Statement statement = statementBuilder.prepareMatchOf(nodeDescription, Optional.empty())
			.returning(asterisk()).build();
		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Flux<T> findAllById(Iterable<ID> ids) {
		Statement statement = statementBuilder
			.prepareMatchOf(nodeDescription, Optional.of(idExpression.in((parameter("ids")))))
			.returning(asterisk())
			.build();

		return createExecutableQuery(nodeClass, statement, singletonMap("ids", ids)).getResults();
	}

	@Override
	public Flux<T> findAllById(Publisher<ID> idStream) {
		return Flux.from(idStream).buffer().flatMap(this::findAllById);
	}

	@Override
	public Mono<Long> count() {
		Statement statement = statementBuilder.prepareMatchOf(nodeDescription, Optional.empty())
			.returning(Functions.count(asterisk())).build();

		return createExecutableQuery(Long.class, statement, Collections.emptyMap()).getSingleResult();

	}

	@Override public <S extends T> Mono<S> save(S entity) {
		return null;
	}

	@Override public <S extends T> Flux<S> saveAll(Iterable<S> entities) {
		return null;
	}

	@Override public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {
		return null;
	}

	@Override public Mono<Void> deleteById(ID id) {
		return null;
	}

	@Override public Mono<Void> deleteById(Publisher<ID> id) {
		return null;
	}

	@Override public Mono<Void> delete(T entity) {
		return null;
	}

	@Override public Mono<Void> deleteAll(Iterable<? extends T> entities) {
		return null;
	}

	@Override public Mono<Void> deleteAll(Publisher<? extends T> entityStream) {
		return null;
	}

	@Override public Mono<Void> deleteAll() {
		return null;
	}

	/**
	 * This wraps all information necessary to predicate a match: A root condition and actual parameters to fill
	 * in formal parameters inside the condition.
	 */
	@RequiredArgsConstructor(access = PRIVATE)
	private static class Predicate {

		private final NodeDescription<?> nodeDescription;

		private Condition condition = Conditions.noCondition();

		private final Map<String, Object> parameters = new HashMap<>();

		void add(MatchMode matchMode, Condition additionalCondition) {

			switch (matchMode) {
				case ALL:
					this.condition = this.condition.and(additionalCondition);
					break;
				case ANY:
					this.condition = this.condition.or(additionalCondition);
					break;
				default:
					throw new IllegalArgumentException("Unsupported match mode: " + matchMode);
			}
		}
	}

	private <S extends T> Predicate createPredicate(Example<S> example) {

		NodeDescription<?> probeNodeDescription = mappingContext.getRequiredNodeDescription(example.getProbeType());
		SymbolicName rootNode = Cypher.symbolicName(NAME_OF_ROOT_NODE);
		Collection<GraphPropertyDescription> graphProperties = probeNodeDescription.getGraphProperties();
		DirectFieldAccessFallbackBeanWrapper beanWrapper = new DirectFieldAccessFallbackBeanWrapper(example.getProbe());
		ExampleMatcher matcher = example.getMatcher();
		MatchMode mode = matcher.getMatchMode();
		ExampleMatcherAccessor matcherAccessor = new ExampleMatcherAccessor(matcher);

		Predicate predicate = new Predicate(probeNodeDescription);
		for (GraphPropertyDescription graphProperty : graphProperties) {

			// TODO Relationships are not traversed.

			String currentPath = graphProperty.getFieldName();
			if (matcherAccessor.isIgnoredPath(currentPath)) {
				continue;
			}

			boolean internalId = graphProperty.isIdProperty() && probeNodeDescription.useInternalIds();
			String propertyName = graphProperty.getPropertyName();

			PropertyValueTransformer transformer = matcherAccessor.getValueTransformerForPath(currentPath);
			Optional<Object> optionalValue = transformer
				.apply(Optional.ofNullable(beanWrapper.getPropertyValue(currentPath)));

			if (!optionalValue.isPresent()) {
				if (!internalId && matcherAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
					predicate.add(mode, property(rootNode, propertyName).isNull());
				}
				continue;
			}

			if (graphProperty.isRelationship()) {
				log.error("Querying by example does not support traversing of relationships.");
			} else if (graphProperty.isIdProperty() && probeNodeDescription.useInternalIds()) {
				predicate.add(mode, idExpression.isEqualTo(literalOf(optionalValue.get())));
			} else {
				Expression property = property(rootNode, propertyName);
				Expression parameter = parameter(propertyName);
				Condition condition = property.isEqualTo(parameter);

				if (String.class.equals(graphProperty.getActualType())) {

					if (matcherAccessor.isIgnoreCaseForPath(currentPath)) {
						property = Functions.toLower(property);
						parameter = Functions.toLower(parameter);
					}

					switch (matcherAccessor.getStringMatcherForPath(currentPath)) {
						case DEFAULT:
						case EXACT:
							// This needs to be recreated as both property and parameter might have changed above
							condition = property.isEqualTo(parameter);
							break;
						case CONTAINING:
							condition = property.contains(parameter);
							break;
						case STARTING:
							condition = property.startsWith(parameter);
							break;
						case ENDING:
							condition = property.endsWith(parameter);
							break;
						case REGEX:
							condition = property.matches(parameter);
							break;
						default:
							throw new IllegalArgumentException(
								"Unsupported StringMatcher " + matcherAccessor.getStringMatcherForPath(currentPath));
					}
				}
				predicate.add(mode, condition);
				predicate.parameters.put(propertyName, optionalValue.get());
			}
		}

		return predicate;
	}

	private ExecutableReactiveQuery<T> createExecutableQuery(Statement statement) {
		return createExecutableQuery(nodeClass, statement, Collections.emptyMap());
	}

	private <RS> ExecutableReactiveQuery<RS> createExecutableQuery(Class<RS> resultType, Statement statement,
		Map<String, Object> parameters) {

		BiFunction<TypeSystem, Record, ?> mappingFunctionToUse = this.mappingFunction;
		if (!this.nodeClass.equals(resultType)) {
			mappingFunctionToUse = mappingContext.getMappingFunctionFor(resultType).orElse(null);
		}

		PreparedQuery queryDescription = PreparedQuery.queryFor(resultType)
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.usingMappingFunction(mappingFunctionToUse)
			.build();

		return ExecutableReactiveQuery.create(queryDescription, neo4jClient);
	}
}
