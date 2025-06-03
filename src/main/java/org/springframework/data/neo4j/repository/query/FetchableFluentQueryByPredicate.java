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
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import com.querydsl.core.types.Predicate;
import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Cypher;

import org.springframework.data.domain.KeysetScrollPosition;
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

/**
 * Immutable implementation of a {@link FetchableFluentQuery}. All methods that return a
 * {@link FetchableFluentQuery} return a new instance, the original instance won't be
 * modified.
 *
 * @param <S> the source type
 * @param <R> the result type if projected
 * @author Michael J. Simons
 * @since 6.2
 */
@API(status = API.Status.INTERNAL, since = "6.2")
final class FetchableFluentQueryByPredicate<S, R> extends FluentQuerySupport<R> implements FetchableFluentQuery<R> {

	private final Predicate predicate;

	private final Neo4jPersistentEntity<S> metaData;

	private final FluentFindOperation findOperation;

	private final Function<Predicate, Long> countOperation;

	private final Function<Predicate, Boolean> existsOperation;

	private final Neo4jMappingContext mappingContext;

	FetchableFluentQueryByPredicate(Predicate predicate, Neo4jMappingContext mappingContext,
			Neo4jPersistentEntity<S> metaData, Class<R> resultType, FluentFindOperation findOperation,
			Function<Predicate, Long> countOperation, Function<Predicate, Boolean> existsOperation) {
		this(predicate, mappingContext, metaData, resultType, findOperation, countOperation, existsOperation,
				Sort.unsorted(), null, null);
	}

	FetchableFluentQueryByPredicate(Predicate predicate, Neo4jMappingContext mappingContext,
			Neo4jPersistentEntity<S> metaData, Class<R> resultType, FluentFindOperation findOperation,
			Function<Predicate, Long> countOperation, Function<Predicate, Boolean> existsOperation, Sort sort,
			@Nullable Integer limit, @Nullable Collection<String> properties) {
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
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		return new FetchableFluentQueryByPredicate<>(this.predicate, this.mappingContext, this.metaData,
				this.resultType, this.findOperation, this.countOperation, this.existsOperation, this.sort.and(sort),
				this.limit, this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public FetchableFluentQuery<R> limit(int limit) {
		return new FetchableFluentQueryByPredicate<>(this.predicate, this.mappingContext, this.metaData,
				this.resultType, this.findOperation, this.countOperation, this.existsOperation, this.sort, limit,
				this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		return new FetchableFluentQueryByPredicate<>(this.predicate, this.mappingContext, this.metaData, resultType,
				this.findOperation, this.countOperation, this.existsOperation);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByPredicate<>(this.predicate, this.mappingContext, this.metaData,
				this.resultType, this.findOperation, this.countOperation, this.existsOperation, this.sort, this.limit,
				mergeProperties(extractAllPaths(properties)));
	}

	@Override
	@Nullable public R oneValue() {

		return this.findOperation.find(this.metaData.getType())
			.as(this.resultType)
			.matching(QueryFragmentsAndParameters.forConditionAndSort(this.metaData,
					Cypher.adapt(this.predicate).asCondition(), this.sort, this.limit, createIncludedFieldsPredicate()))
			.oneValue();
	}

	@Override
	@Nullable public R firstValue() {

		List<R> all = all();
		return all.isEmpty() ? null : all.get(0);
	}

	@Override
	public List<R> all() {

		return this.findOperation.find(this.metaData.getType())
			.as(this.resultType)
			.matching(QueryFragmentsAndParameters.forConditionAndSort(this.metaData,
					Cypher.adapt(this.predicate).asCondition(), this.sort, this.limit, createIncludedFieldsPredicate()))
			.all();
	}

	@Override
	public Page<R> page(Pageable pageable) {

		List<R> page = this.findOperation.find(this.metaData.getType())
			.as(this.resultType)
			.matching(QueryFragmentsAndParameters.forConditionAndPageable(this.metaData,
					Cypher.adapt(this.predicate).asCondition(), pageable, createIncludedFieldsPredicate()))
			.all();

		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(page, pageable, totalCountSupplier);
	}

	@Override
	public Window<R> scroll(ScrollPosition scrollPosition) {

		QueryFragmentsAndParameters queryFragmentsAndParameters = QueryFragmentsAndParameters
			.forConditionWithScrollPosition(this.metaData, Cypher.adapt(this.predicate).asCondition(),
					((scrollPosition instanceof KeysetScrollPosition keysetScrollPosition)
							? CypherAdapterUtils.combineKeysetIntoCondition(this.metaData, keysetScrollPosition,
									this.sort, this.mappingContext.getConversionService())
							: null),
					scrollPosition, this.sort, (this.limit != null) ? this.limit + 1 : 1,
					createIncludedFieldsPredicate());

		List<R> rawResult = this.findOperation.find(this.metaData.getType())
			.as(this.resultType)
			.matching(queryFragmentsAndParameters)
			.all();

		return scroll(scrollPosition, rawResult, this.metaData);
	}

	@Override
	public Stream<R> stream() {
		return all().stream();
	}

	@Override
	public long count() {
		return this.countOperation.apply(this.predicate);
	}

	@Override
	public boolean exists() {
		return this.existsOperation.apply(this.predicate);
	}

}
