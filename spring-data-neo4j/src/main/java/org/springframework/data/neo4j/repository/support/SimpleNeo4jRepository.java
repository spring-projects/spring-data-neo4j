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
import java.util.function.LongSupplier;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.cypher.Condition;
import org.springframework.data.neo4j.core.cypher.Conditions;
import org.springframework.data.neo4j.core.cypher.Cypher;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.Node;
import org.springframework.data.neo4j.core.cypher.SortItem;
import org.springframework.data.neo4j.core.cypher.Statement;
import org.springframework.data.neo4j.core.cypher.StatementBuilder;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.cypher.renderer.Renderer;
import org.springframework.data.neo4j.core.schema.GraphPropertyDescription;
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

	private final Class<T> nodeClass;

	private final NodeDescription nodeDescription;
	private Node node;

	SimpleNeo4jRepository(NodeManager nodeManager, Class<T> nodeClass) {
		this.nodeManager = nodeManager;
		this.nodeClass = nodeClass;

		this.nodeDescription = nodeManager.getSchema().getNodeDescription(nodeClass)
				.orElseThrow(() -> new IllegalArgumentException("unsupported class"));

		this.node = Cypher.node(nodeDescription.getPrimaryLabel()).named("n");
	}

	@Override
	public Iterable<T> findAll(Sort sort) {

		Statement statement = match(node).returning(node).orderBy(createSort(sort)).build();
		return nodeManager.executeTypedQueryForObjects(renderer.render(statement), nodeClass);
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		StatementBuilder.OngoingMatchAndReturn returning = match(node).returning(node);

		StatementBuilder.BuildableMatch returningWithPaging = addPagingParameter(pageable, returning);

		Statement statement = returningWithPaging.build();

		List<T> allResult = new ArrayList<>(nodeManager.executeTypedQueryForObjects(renderer.render(statement), nodeClass));
		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(allResult, pageable, totalCountSupplier);
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {

		StatementBuilder.OngoingMatchAndReturn returning = match(node).where(createAndConditions(example)).returning(node);

		StatementBuilder.BuildableMatch returningWithPaging = addPagingParameter(pageable, returning);

		Statement statement = returningWithPaging.build();

		List<T> allResult = new ArrayList<>(nodeManager.executeTypedQueryForObjects(renderer.render(statement), nodeClass));
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
		// todo only works for internal ids right now
		Statement statement = match(node).where(node.internalId().isEqualTo(literalOf((Number) id))).returning(node)
				.build();
		return nodeManager.executeTypedQueryForObject(renderer.render(statement), nodeClass);
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	public Iterable<T> findAll() {

		Statement statement = match(node).returning(node).build();
		return nodeManager.executeTypedQueryForObjects(renderer.render(statement), nodeClass);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {
		// todo only works for internal ids right now
		Statement statement = match(node).where(node.internalId().isIn((Iterable<Long>) ids)).returning(node)
				.build();
		return nodeManager.executeTypedQueryForObjects(renderer.render(statement), nodeClass);
	}

	@Override
	public long count() {

		Statement statement = match(node).returning(Functions.count(node)).build();

		return nodeManager.executeTypedQueryForObject(renderer.render(statement), Long.class).get();
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

		Statement statement = match(node).where(createAndConditions(example)).returning(node).build();

		return (Optional<S>) nodeManager.executeTypedQueryForObject(renderer.render(statement), nodeClass);
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example) {
		Statement statement = match(node).where(createAndConditions(example)).returning(node).build();

		return (Collection<S>) nodeManager.executeTypedQueryForObjects(renderer.render(statement), nodeClass);
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example, Sort sort) {
		Statement statement = match(node).where(createAndConditions(example)).returning(node).orderBy(createSort(sort)).build();

		return (Collection<S>) nodeManager.executeTypedQueryForObjects(renderer.render(statement), nodeClass);
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		Statement statement = match(node).where(createAndConditions(example)).returning(Functions.count(node)).build();

		return nodeManager.executeTypedQueryForObject(renderer.render(statement), Long.class).get();
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


	private List<Condition> createConditionsFromProperties(Example example) {

		Collection<GraphPropertyDescription> graphProperties = nodeDescription.getGraphProperties();
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

			Condition condition = node.property(propertyName).isEqualTo(literalOf(propertyValue));
			conditionList.add(condition);
		}

		return conditionList;
	}

	private SortItem[] createSort(Sort sort) {

		return sort.stream().map(order -> {
			String property = order.getProperty();
			SortItem sortItem = Cypher.sort(node.property(property));

			// Spring's Sort.Order defaults to ascending, so we just need to change this if we have descending order.
			if (order.isDescending()) {
				sortItem = sortItem.descending();
			}
			return sortItem;
		}).toArray(SortItem[]::new);
	}
}
