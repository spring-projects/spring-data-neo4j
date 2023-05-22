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

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.neo4j.core.FluentFindOperation;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * Immutable implementation of a {@link FetchableFluentQuery}. All
 * methods that return a {@link FetchableFluentQuery} return a new instance, the original instance won't be
 * modified.
 *
 * @author Michael J. Simons
 * @param <S> Source type
 * @param <R> Result type
 * @since 6.2
 */
@API(status = API.Status.INTERNAL, since = "6.2")
final class FetchableFluentQueryByExample<S, R> extends FluentQuerySupport<R> implements FetchableFluentQuery<R> {

	private final Neo4jMappingContext mappingContext;

	private final Example<S> example;

	private final FluentFindOperation findOperation;

	private final Function<Example<S>, Long> countOperation;

	private final Function<Example<S>, Boolean> existsOperation;

	FetchableFluentQueryByExample(
			Example<S> example,
			Class<R> resultType,
			Neo4jMappingContext mappingContext,
			FluentFindOperation findOperation,
			Function<Example<S>, Long> countOperation,
			Function<Example<S>, Boolean> existsOperation
	) {
		this(example, resultType, mappingContext, findOperation, countOperation, existsOperation, Sort.unsorted(),
				null, null);
	}

	FetchableFluentQueryByExample(
			Example<S> example,
			Class<R> resultType,
			Neo4jMappingContext mappingContext,
			FluentFindOperation findOperation,
			Function<Example<S>, Long> countOperation,
			Function<Example<S>, Boolean> existsOperation,
			Sort sort,
			@Nullable Integer limit,
			@Nullable Collection<String> properties
	) {
		super(resultType, sort, limit, properties);
		this.mappingContext = mappingContext;
		this.example = example;
		this.findOperation = findOperation;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
	}

	@Override
	@SuppressWarnings("HiddenField")
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		return new FetchableFluentQueryByExample<>(this.example, this.resultType, this.mappingContext, this.findOperation,
				this.countOperation, this.existsOperation, this.sort.and(sort), this.limit, this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public FetchableFluentQuery<R> limit(int limit) {
		return new FetchableFluentQueryByExample<>(this.example, this.resultType, this.mappingContext, this.findOperation,
				this.countOperation, this.existsOperation, this.sort, limit, this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		return new FetchableFluentQueryByExample<>(this.example, resultType, this.mappingContext, this.findOperation,
				this.countOperation, this.existsOperation);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByExample<>(this.example, this.resultType, this.mappingContext, this.findOperation,
				this.countOperation, this.existsOperation, this.sort, this.limit, mergeProperties(properties));
	}

	@Override
	public R oneValue() {

		return findOperation.find(example.getProbeType())
				.as(resultType)
				.matching(QueryFragmentsAndParameters.forExampleWithSort(mappingContext, example, sort, limit,
						createIncludedFieldsPredicate()))
				.oneValue();
	}

	@Override
	public R firstValue() {

		List<R> all = all();
		return all.isEmpty() ? null : all.get(0);
	}

	@Override
	public List<R> all() {

		return findOperation.find(example.getProbeType())
				.as(resultType)
				.matching(QueryFragmentsAndParameters.forExampleWithSort(mappingContext, example, sort, limit,
						createIncludedFieldsPredicate()))
				.all();
	}

	@Override
	public Page<R> page(Pageable pageable) {

		List<R> page = findOperation.find(example.getProbeType())
				.as(resultType)
				.matching(QueryFragmentsAndParameters.forExampleWithPageable(mappingContext, example, pageable,
						createIncludedFieldsPredicate()))
				.all();

		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(page, pageable, totalCountSupplier);
	}

	@Override
	public Window<R> scroll(ScrollPosition scrollPosition) {
		Class<S> domainType = this.example.getProbeType();
		Neo4jPersistentEntity<?> entity = mappingContext.getPersistentEntity(domainType);

		var skip = scrollPosition.isInitial()
				? 0
				: (scrollPosition instanceof OffsetScrollPosition offsetScrollPosition) ? offsetScrollPosition.getOffset()
				: 0;

		Condition condition = scrollPosition instanceof KeysetScrollPosition keysetScrollPosition
				? CypherAdapterUtils.combineKeysetIntoCondition(mappingContext.getPersistentEntity(example.getProbeType()), keysetScrollPosition, sort, mappingContext.getConversionService())
				: null;

		List<R> rawResult = findOperation.find(domainType)
				.as(resultType)
				.matching(QueryFragmentsAndParameters.forExampleWithScrollPosition(mappingContext, example, condition, sort, limit == null ? 1 : limit + 1, skip, scrollPosition, createIncludedFieldsPredicate()))
				.all();

		return scroll(scrollPosition, rawResult, entity);
	}

	@Override
	public Stream<R> stream() {
		return all().stream();
	}

	@Override
	public long count() {
		return countOperation.apply(example);
	}

	@Override
	public boolean exists() {
		return existsOperation.apply(example);
	}
}
