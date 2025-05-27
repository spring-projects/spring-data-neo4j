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
package org.springframework.data.neo4j.repository.query;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Cypher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.ReactiveFluentFindOperation;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.repository.query.FluentQuery.ReactiveFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.types.Predicate;

/**
 * Immutable implementation of a {@link ReactiveFluentQuery}. All
 * methods that return a {@link ReactiveFluentQuery} return a new instance, the original instance won't be
 * modified.
 *
 * @author Michael J. Simons
 * @param <S> Source type
 * @param <R> Result type
 * @since 6.2
 */
@API(status = API.Status.INTERNAL, since = "6.2") final class ReactiveFluentQueryByPredicate<S, R>
		extends FluentQuerySupport<R> implements ReactiveFluentQuery<R> {

	private final Predicate predicate;

	private final Neo4jPersistentEntity<S> metaData;

	private final ReactiveFluentFindOperation findOperation;

	private final Function<Predicate, Mono<Long>> countOperation;

	private final Function<Predicate, Mono<Boolean>> existsOperation;

	private final Neo4jMappingContext mappingContext;

	ReactiveFluentQueryByPredicate(
			Predicate predicate,
			Neo4jMappingContext mappingContext,
			Neo4jPersistentEntity<S> metaData,
			Class<R> resultType,
			ReactiveFluentFindOperation findOperation,
			Function<Predicate, Mono<Long>> countOperation,
			Function<Predicate, Mono<Boolean>> existsOperation
	) {
		this(predicate, mappingContext, metaData, resultType, findOperation, countOperation, existsOperation, Sort.unsorted(), null, null);
	}

	ReactiveFluentQueryByPredicate(
			Predicate predicate,
			Neo4jMappingContext mappingContext,
			Neo4jPersistentEntity<S> metaData,
			Class<R> resultType,
			ReactiveFluentFindOperation findOperation,
			Function<Predicate, Mono<Long>> countOperation,
			Function<Predicate, Mono<Boolean>> existsOperation,
			Sort sort,
			@Nullable Integer limit,
			@Nullable Collection<String> properties
	) {
		super(resultType, sort, limit, properties);
		this.predicate = predicate;
		this.mappingContext = mappingContext;
		this.metaData = metaData;
		this.findOperation = findOperation;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
	}

	@Override
	@SuppressWarnings("HiddenField")
	public ReactiveFluentQuery<R> sortBy(Sort sort) {

		return new ReactiveFluentQueryByPredicate<>(this.predicate, this.mappingContext, this.metaData, this.resultType, this.findOperation,
				this.countOperation, this.existsOperation, this.sort.and(sort), this.limit, this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public ReactiveFluentQuery<R> limit(int limit) {
		return new ReactiveFluentQueryByPredicate<>(this.predicate, this.mappingContext, this.metaData, this.resultType, this.findOperation,
				this.countOperation, this.existsOperation, this.sort, limit, this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public <NR> ReactiveFluentQuery<NR> as(Class<NR> resultType) {

		return new ReactiveFluentQueryByPredicate<>(this.predicate, this.mappingContext, this.metaData, resultType, this.findOperation,
				this.countOperation, this.existsOperation);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public ReactiveFluentQuery<R> project(Collection<String> properties) {

		return new ReactiveFluentQueryByPredicate<>(this.predicate, this.mappingContext, this.metaData, resultType, this.findOperation,
				this.countOperation, this.existsOperation, this.sort, this.limit, mergeProperties(extractAllPaths(properties)));
	}

	@Override
	public Mono<R> one() {

		return findOperation.find(metaData.getType())
				.as(resultType)
				.matching(
						QueryFragmentsAndParameters.forConditionAndSort(metaData,
								Cypher.adapt(predicate).asCondition(),
								sort,
								limit,
								createIncludedFieldsPredicate()))
				.one();
	}

	@Override
	public Mono<R> first() {

		return all().take(1).singleOrEmpty();
	}

	@Override
	public Flux<R> all() {

		return findOperation.find(metaData.getType())
				.as(resultType)
				.matching(
						QueryFragmentsAndParameters.forConditionAndSort(metaData,
								Cypher.adapt(predicate).asCondition(),
								sort,
								limit,
								createIncludedFieldsPredicate()))
				.all();
	}

	@Override
	public Mono<Page<R>> page(Pageable pageable) {

		Flux<R> results = findOperation.find(metaData.getType())
				.as(resultType)
				.matching(
						QueryFragmentsAndParameters.forConditionAndPageable(metaData,
								Cypher.adapt(predicate).asCondition(),
								pageable,
								createIncludedFieldsPredicate()))
				.all();

		return results.collectList().zipWith(countOperation.apply(predicate)).map(tuple -> {
			Page<R> page = PageableExecutionUtils.getPage(tuple.getT1(), pageable, () -> tuple.getT2());
			return page;
		});
	}

	@Override
	public Mono<Window<R>> scroll(ScrollPosition scrollPosition) {
		QueryFragmentsAndParameters queryFragmentsAndParameters = QueryFragmentsAndParameters.forConditionWithScrollPosition(metaData,
				Cypher.adapt(predicate).asCondition(),
				(scrollPosition instanceof KeysetScrollPosition keysetScrollPosition
						? CypherAdapterUtils.combineKeysetIntoCondition(metaData, keysetScrollPosition, sort, mappingContext.getConversionService())
						: null),
				scrollPosition, sort,
				limit == null ? 1 : limit + 1,
				createIncludedFieldsPredicate());

		return findOperation.find(metaData.getType())
				.as(resultType)
				.matching(queryFragmentsAndParameters)
				.all()
				.collectList()
				.map(rawResult -> scroll(scrollPosition, rawResult, metaData));
	}

	@Override
	public Mono<Long> count() {
		return countOperation.apply(predicate);
	}

	@Override
	public Mono<Boolean> exists() {
		return existsOperation.apply(predicate);
	}
}
