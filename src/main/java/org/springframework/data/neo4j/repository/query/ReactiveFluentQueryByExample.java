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

import java.util.Collection;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Condition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.neo4j.core.ReactiveFluentFindOperation;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.repository.query.FluentQuery.ReactiveFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;

/**
 * Immutable implementation of a {@link ReactiveFluentQuery}. All methods that return a
 * {@link ReactiveFluentQuery} return a new instance, the original instance won't be
 * modified.
 *
 * @param <S> the source type
 * @param <R> the result type
 * @author Michael J. Simons
 * @since 6.2
 */
@API(status = API.Status.INTERNAL, since = "6.2")
final class ReactiveFluentQueryByExample<S, R> extends FluentQuerySupport<R> implements ReactiveFluentQuery<R> {

	private final Neo4jMappingContext mappingContext;

	private final Example<S> example;

	private final ReactiveFluentFindOperation findOperation;

	private final Function<Example<S>, Mono<Long>> countOperation;

	private final Function<Example<S>, Mono<Boolean>> existsOperation;

	ReactiveFluentQueryByExample(Example<S> example, Class<R> resultType, Neo4jMappingContext mappingContext,
			ReactiveFluentFindOperation findOperation, Function<Example<S>, Mono<Long>> countOperation,
			Function<Example<S>, Mono<Boolean>> existsOperation) {
		this(example, resultType, mappingContext, findOperation, countOperation, existsOperation, Sort.unsorted(), null,
				null);
	}

	ReactiveFluentQueryByExample(Example<S> example, Class<R> resultType, Neo4jMappingContext mappingContext,
			ReactiveFluentFindOperation findOperation, Function<Example<S>, Mono<Long>> countOperation,
			Function<Example<S>, Mono<Boolean>> existsOperation, Sort sort, @Nullable Integer limit,
			@Nullable Collection<String> properties) {
		super(resultType, sort, limit, properties);
		this.mappingContext = mappingContext;
		this.example = example;
		this.findOperation = findOperation;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
	}

	@Override
	@SuppressWarnings("HiddenField")
	public ReactiveFluentQuery<R> sortBy(Sort sort) {

		return new ReactiveFluentQueryByExample<>(this.example, this.resultType, this.mappingContext,
				this.findOperation, this.countOperation, this.existsOperation, this.sort.and(sort), this.limit,
				this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public ReactiveFluentQuery<R> limit(int limit) {

		return new ReactiveFluentQueryByExample<>(this.example, this.resultType, this.mappingContext,
				this.findOperation, this.countOperation, this.existsOperation, this.sort, limit, this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public <NR> ReactiveFluentQuery<NR> as(Class<NR> resultType) {

		return new ReactiveFluentQueryByExample<>(this.example, resultType, this.mappingContext, this.findOperation,
				this.countOperation, this.existsOperation);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public ReactiveFluentQuery<R> project(Collection<String> properties) {

		return new ReactiveFluentQueryByExample<>(this.example, this.resultType, this.mappingContext,
				this.findOperation, this.countOperation, this.existsOperation, this.sort, this.limit,
				mergeProperties(extractAllPaths(properties)));
	}

	@Override
	public Mono<R> one() {

		return this.findOperation.find(this.example.getProbeType())
			.as(this.resultType)
			.matching(QueryFragmentsAndParameters.forExampleWithSort(this.mappingContext, this.example, this.sort,
					this.limit, createIncludedFieldsPredicate()))
			.one();
	}

	@Override
	public Mono<R> first() {

		return all().take(1).singleOrEmpty();
	}

	@Override
	public Flux<R> all() {

		return this.findOperation.find(this.example.getProbeType())
			.as(this.resultType)
			.matching(QueryFragmentsAndParameters.forExampleWithSort(this.mappingContext, this.example, this.sort,
					this.limit, createIncludedFieldsPredicate()))
			.all();
	}

	@Override
	public Mono<Page<R>> page(Pageable pageable) {

		Flux<R> results = this.findOperation.find(this.example.getProbeType())
			.as(this.resultType)
			.matching(QueryFragmentsAndParameters.forExampleWithPageable(this.mappingContext, this.example, pageable,
					createIncludedFieldsPredicate()))
			.all();
		return results.collectList()
			.zipWith(this.countOperation.apply(this.example))
			.map(tuple -> PageableExecutionUtils.getPage(tuple.getT1(), pageable, tuple::getT2));
	}

	@Override
	public Mono<Window<R>> scroll(ScrollPosition scrollPosition) {
		Class<S> domainType = this.example.getProbeType();
		Neo4jPersistentEntity<?> entity = this.mappingContext.getRequiredPersistentEntity(domainType);

		var skip = scrollPosition.isInitial() ? 0
				: (scrollPosition instanceof OffsetScrollPosition offsetScrollPosition)
						? offsetScrollPosition.getOffset() + 1 : 0;

		Condition condition = (scrollPosition instanceof KeysetScrollPosition keysetScrollPosition) ? CypherAdapterUtils
			.combineKeysetIntoCondition(this.mappingContext.getRequiredPersistentEntity(this.example.getProbeType()),
					keysetScrollPosition, this.sort, this.mappingContext.getConversionService())
				: null;

		return this.findOperation.find(domainType)
			.as(this.resultType)
			.matching(QueryFragmentsAndParameters.forExampleWithScrollPosition(this.mappingContext, this.example,
					condition, this.sort, (this.limit != null) ? this.limit + 1 : 1, skip, scrollPosition,
					createIncludedFieldsPredicate()))
			.all()
			.collectList()
			.map(rawResult -> scroll(scrollPosition, rawResult, entity));
	}

	@Override
	public Mono<Long> count() {
		return this.countOperation.apply(this.example);
	}

	@Override
	public Mono<Boolean> exists() {
		return this.existsOperation.apply(this.example);
	}

}
