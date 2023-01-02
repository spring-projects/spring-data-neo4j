/*
 * Copyright 2011-2023 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.ReactiveFluentFindOperation;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.FluentQuery.ReactiveFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;

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
@API(status = API.Status.INTERNAL, since = "6.2")
final class ReactiveFluentQueryByExample<S, R> extends FluentQuerySupport<R> implements ReactiveFluentQuery<R> {

	private final Neo4jMappingContext mappingContext;

	private final Example<S> example;

	private final ReactiveFluentFindOperation findOperation;

	private final Function<Example<S>, Mono<Long>> countOperation;

	private final Function<Example<S>, Mono<Boolean>> existsOperation;

	ReactiveFluentQueryByExample(
			Example<S> example,
			Class<R> resultType,
			Neo4jMappingContext mappingContext,
			ReactiveFluentFindOperation findOperation,
			Function<Example<S>, Mono<Long>> countOperation,
			Function<Example<S>, Mono<Boolean>> existsOperation
	) {
		this(example, resultType, mappingContext, findOperation, countOperation, existsOperation, Sort.unsorted(),
				null);
	}

	ReactiveFluentQueryByExample(
			Example<S> example,
			Class<R> resultType,
			Neo4jMappingContext mappingContext,
			ReactiveFluentFindOperation findOperation,
			Function<Example<S>, Mono<Long>> countOperation,
			Function<Example<S>, Mono<Boolean>> existsOperation,
			Sort sort,
			@Nullable Collection<String> properties
	) {
		super(resultType, sort, properties);
		this.mappingContext = mappingContext;
		this.example = example;
		this.findOperation = findOperation;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
	}

	@Override
	@SuppressWarnings("HiddenField")
	public ReactiveFluentQuery<R> sortBy(Sort sort) {

		return new ReactiveFluentQueryByExample<>(this.example, this.resultType, this.mappingContext, this.findOperation,
				this.countOperation, this.existsOperation, this.sort.and(sort), this.properties);
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

		return new ReactiveFluentQueryByExample<>(this.example, this.resultType, this.mappingContext, this.findOperation,
				this.countOperation, this.existsOperation, sort, mergeProperties(properties));
	}

	@Override
	public Mono<R> one() {

		return findOperation.find(example.getProbeType())
				.as(resultType)
				.matching(QueryFragmentsAndParameters.forExample(mappingContext, example, sort,
						createIncludedFieldsPredicate()))
				.one();
	}

	@Override
	public Mono<R> first() {

		return all().take(1).singleOrEmpty();
	}

	@Override
	public Flux<R> all() {

		return findOperation.find(example.getProbeType())
				.as(resultType)
				.matching(QueryFragmentsAndParameters.forExample(mappingContext, example, sort,
						createIncludedFieldsPredicate()))
				.all();
	}

	@Override
	public Mono<Page<R>> page(Pageable pageable) {

		Flux<R> results = findOperation.find(example.getProbeType())
				.as(resultType)
				.matching(QueryFragmentsAndParameters.forExample(mappingContext, example, pageable,
						createIncludedFieldsPredicate()))
				.all();
		return results.collectList().zipWith(countOperation.apply(example)).map(tuple -> {
			Page<R> page = PageableExecutionUtils.getPage(tuple.getT1(), pageable, () -> tuple.getT2());
			return page;
		});
	}

	@Override
	public Mono<Long> count() {
		return countOperation.apply(example);
	}

	@Override
	public Mono<Boolean> exists() {
		return existsOperation.apply(example);
	}
}
