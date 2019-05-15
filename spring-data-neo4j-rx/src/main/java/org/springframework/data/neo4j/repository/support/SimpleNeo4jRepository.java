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

import static org.springframework.data.neo4j.core.cypher.Cypher.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.cypher.Condition;
import org.springframework.data.neo4j.core.cypher.Conditions;
import org.springframework.data.neo4j.core.cypher.Cypher;
import org.springframework.data.neo4j.core.cypher.Expression;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.SortItem;
import org.springframework.data.neo4j.core.cypher.Statement;
import org.springframework.data.neo4j.core.cypher.StatementBuilder;
import org.springframework.data.neo4j.core.cypher.SymbolicName;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.cypher.renderer.Renderer;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.GraphPropertyDescription;
import org.springframework.data.neo4j.core.schema.IdDescription;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.support.PageableExecutionUtils;
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
class SimpleNeo4jRepository<T, ID> implements Neo4jRepository<T, ID> {

	private static final Renderer renderer = CypherRenderer.create();

	private final NodeManager nodeManager;

	private final Neo4jMappingContext mappingContext;

	private final Class<T> nodeClass;

	private final NodeDescription<?> nodeDescription;
	private final BiFunction<TypeSystem, Record, ?> mappingFunction;
	private Expression idExpression;

	SimpleNeo4jRepository(NodeManager nodeManager, Neo4jMappingContext mappingContext, Class<T> nodeClass) {

		this.nodeManager = nodeManager;
		this.mappingContext = mappingContext;
		this.nodeClass = nodeClass;

		this.nodeDescription = mappingContext.getRequiredNodeDescription(nodeClass);
		this.mappingFunction = mappingContext.getRequiredMappingFunctionFor(nodeClass);

		final SymbolicName rootNode = Cypher.symbolicName("n");
		final IdDescription idDescription = this.nodeDescription.getIdDescription();
		switch (idDescription.getIdStrategy()) {
			case INTERNAL:
				idExpression = Functions.id(rootNode);
				break;
			case ASSIGNED:
			case GENERATED:
				idExpression = idDescription.getGraphPropertyName()
					.map(propertyName -> property(rootNode.getName(), propertyName)).get();
				break;
			default:
				throw new IllegalStateException("Unsupported ID strategy: %s" + idDescription.getIdStrategy());
		}
	}

	@Override
	public Iterable<T> findAll(Sort sort) {

		Statement statement = mappingContext.prepareMatchOf(nodeDescription, Optional.empty())
			.returning(asterisk())
			.orderBy(createSort(sort))
			.build();

		return nodeManager.toExecutableQuery(prepareQuery(statement)).getResults();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		StatementBuilder.OngoingMatchAndReturn returning = mappingContext
			.prepareMatchOf(nodeDescription, Optional.empty()).returning(asterisk());

		StatementBuilder.BuildableMatch returningWithPaging = addPagingParameter(pageable, returning);

		Statement statement = returningWithPaging.build();

		List<T> allResult = nodeManager.toExecutableQuery(prepareQuery(statement)).getResults();
		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(allResult, pageable, totalCountSupplier);
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {

		StatementBuilder.OngoingMatchAndReturn returning = mappingContext
			.prepareMatchOf(nodeDescription, Optional.of(createAndConditions(example))).returning(asterisk());

		StatementBuilder.BuildableMatch returningWithPaging = addPagingParameter(pageable, returning);

		Statement statement = returningWithPaging.build();

		List<T> allResult = nodeManager.toExecutableQuery(prepareQuery(statement)).getResults();
		LongSupplier totalCountSupplier = this::count;
		return (Page<S>) PageableExecutionUtils.getPage(allResult, pageable, totalCountSupplier);
	}

	private StatementBuilder.BuildableMatch addPagingParameter(Pageable pageable, StatementBuilder.OngoingMatchAndReturn returning) {

		Sort sort = pageable.getSort();

		long skip = pageable.getOffset();

		int pageSize = pageable.getPageSize();

		return returning.orderBy(createSort(sort)).skip(skip).limit(pageSize);
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {
		return this.nodeManager.save(entity);
	}

	@Override
	@Transactional
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		throw new UnsupportedOperationException("Not there yet.");
	}

	@Override
	public Optional<T> findById(ID id) {

		Statement statement = mappingContext
			.prepareMatchOf(nodeDescription, Optional.of(idExpression.isEqualTo(literalOf(id))))
			.returning(asterisk())
			.build();
		return nodeManager.toExecutableQuery(prepareQuery(statement)).getSingleResult();
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	public Iterable<T> findAll() {

		Statement statement = mappingContext.prepareMatchOf(nodeDescription, Optional.empty()).returning(asterisk())
			.build();
		return nodeManager.toExecutableQuery(prepareQuery(statement)).getResults();
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {

		Statement statement = mappingContext
			.prepareMatchOf(nodeDescription, Optional.of(idExpression.isIn((Iterable<Long>) ids))).returning(asterisk())
			.build();
		return nodeManager.toExecutableQuery(prepareQuery(statement)).getResults();
	}

	@Override
	public long count() {

		Statement statement = mappingContext.prepareMatchOf(nodeDescription, Optional.empty())
			.returning(Functions.count(asterisk())).build();

		return nodeManager.toExecutableQuery(prepareQuery(Long.class, statement)).getRequiredSingleResult();
	}

	@Override
	@Transactional
	public void deleteById(ID id) {
		throw new UnsupportedOperationException("Not there yet.");
	}

	@Override
	@Transactional
	public void delete(T entity) {
		throw new UnsupportedOperationException("Not there yet.");
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {

		throw new UnsupportedOperationException("Not there yet.");
	}

	@Override
	@Transactional
	public void deleteAll() {

		for (T element : findAll()) {
			delete(element);
		}
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {

		NodeDescription<?> probeNodeDescription = mappingContext.getRequiredNodeDescription(example.getProbeType());
		Statement statement = mappingContext
			.prepareMatchOf(probeNodeDescription, Optional.of(createAndConditions(example)))
			.returning(asterisk())
			.build();

		return nodeManager.toExecutableQuery(prepareQuery(example.getProbeType(), statement))
			.getSingleResult();
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example) {

		NodeDescription<?> probeNodeDescription = mappingContext.getRequiredNodeDescription(example.getProbeType());
		Statement statement = mappingContext.prepareMatchOf(probeNodeDescription, Optional.of(createAndConditions(example)))
			.returning(asterisk())
			.build();

		return nodeManager.toExecutableQuery(
			prepareQuery(example.getProbeType(), statement)).getResults();
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example, Sort sort) {

		NodeDescription<?> probeNodeDescription = mappingContext.getRequiredNodeDescription(example.getProbeType());
		Statement statement = mappingContext
			.prepareMatchOf(probeNodeDescription, Optional.of(createAndConditions(example)))
			.returning(asterisk())
			.orderBy(createSort(sort)).build();

		return nodeManager.toExecutableQuery(prepareQuery(example.getProbeType(), statement)).getResults();
	}

	@Override
	public <S extends T> long count(Example<S> example) {

		NodeDescription<?> probeNodeDescription = mappingContext.getRequiredNodeDescription(example.getProbeType());
		Statement statement = mappingContext
			.prepareMatchOf(probeNodeDescription, Optional.of(createAndConditions(example)))
			.returning(Functions.count(asterisk()))
			.build();

		return nodeManager.toExecutableQuery(prepareQuery(Long.class, statement)).getRequiredSingleResult();
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		return findAll(example).iterator().hasNext();
	}

	private Condition createAndConditions(Example example) {

		List<Condition> conditionsFromProperties = createConditionsFromProperties(example);

		Condition result = Conditions.noCondition();
		for (Condition condition : conditionsFromProperties) {
			result = result.and(condition);
		}
		return result;

	}

	private <S extends T> List<Condition> createConditionsFromProperties(Example<S> example) {

		NodeDescription<?> probeNodeDescription = mappingContext.getRequiredNodeDescription(example.getProbeType());
		SymbolicName rootNode = Cypher.symbolicName("n");
		Collection<GraphPropertyDescription> graphProperties = probeNodeDescription.getGraphProperties();
		DirectFieldAccessFallbackBeanWrapper beanWrapper = new DirectFieldAccessFallbackBeanWrapper(example.getProbe());
		ExampleMatcher matcher = example.getMatcher();

		List<Condition> conditionList = new ArrayList<>();

		for (GraphPropertyDescription graphProperty : graphProperties) {
			String fieldName = graphProperty.getFieldName();
			if (matcher.isIgnoredPath(fieldName)) {
				continue;
			}

			Object propertyValue = beanWrapper.getPropertyValue(fieldName);

			if (matcher.getNullHandler() != ExampleMatcher.NullHandler.INCLUDE && propertyValue == null) {
				continue;
			}

			String propertyName = graphProperty.getPropertyName();

			Condition condition = property(rootNode, propertyName).isEqualTo(literalOf(propertyValue));
			conditionList.add(condition);
		}

		return conditionList;
	}

	private SortItem[] createSort(Sort sort) {

		SymbolicName rootNode = Cypher.symbolicName("n");

		return sort.stream().map(order -> {
			String property = order.getProperty();
			SortItem sortItem = Cypher.sort(property(rootNode, property));

			// Spring's Sort.Order defaults to ascending, so we just need to change this if we have descending order.
			if (order.isDescending()) {
				sortItem = sortItem.descending();
			}
			return sortItem;
		}).toArray(SortItem[]::new);
	}

	private PreparedQuery<T> prepareQuery(Statement statement) {
		return prepareQuery(nodeClass, statement);
	}

	private <T> PreparedQuery<T> prepareQuery(Class<T> resultType, Statement statement) {

		BiFunction<TypeSystem, Record, ?> mappingFunctionToUse = this.mappingFunction;
		if (!this.nodeClass.equals(resultType)) {
			mappingFunctionToUse = mappingContext.getMappingFunctionFor(resultType).orElse(null);
		}

		return PreparedQuery.queryFor(resultType)
			.withCypherQuery(renderer.render(statement))
			.usingMappingFunction(mappingFunctionToUse)
			.build();
	}
}
