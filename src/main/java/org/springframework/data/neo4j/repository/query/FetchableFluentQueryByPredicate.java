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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Cypher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.FluentFindOperation;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;

import com.querydsl.core.types.Predicate;

/**
 * Immutable implementation of a {@link FetchableFluentQuery}. All
 * methods that return a {@link FetchableFluentQuery} return a new instance, the original instance won't be
 * modified.
 *
 * @author Michael J. Simons
 * @param <S> Source type
 * @param <R> Result type
 * @since 6.2
 * @soundtrack Die Ärzte - Geräusch
 */
@API(status = API.Status.INTERNAL, since = "6.2")
final class FetchableFluentQueryByPredicate<S, R> extends FluentQuerySupport<R> implements FetchableFluentQuery<R> {

	private final Predicate predicate;

	private final Neo4jPersistentEntity<S> metaData;

	private final FluentFindOperation findOperation;

	private final Function<Predicate, Long> countOperation;

	private final Function<Predicate, Boolean> existsOperation;

	FetchableFluentQueryByPredicate(
			Predicate predicate,
			Neo4jPersistentEntity<S> metaData,
			Class<R> resultType,
			FluentFindOperation findOperation,
			Function<Predicate, Long> countOperation,
			Function<Predicate, Boolean> existsOperation
	) {
		this(predicate, metaData, resultType, findOperation, countOperation, existsOperation, Sort.unsorted(), null);
	}

	FetchableFluentQueryByPredicate(
			Predicate predicate,
			Neo4jPersistentEntity<S> metaData,
			Class<R> resultType,
			FluentFindOperation findOperation,
			Function<Predicate, Long> countOperation,
			Function<Predicate, Boolean> existsOperation,
			Sort sort,
			@Nullable Collection<String> properties
	) {
		super(resultType, sort, properties);
		this.predicate = predicate;
		this.metaData = metaData;
		this.findOperation = findOperation;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
	}

	@Override
	@SuppressWarnings("HiddenField")
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		return new FetchableFluentQueryByPredicate<>(this.predicate, this.metaData, this.resultType, this.findOperation,
				this.countOperation, this.existsOperation, this.sort.and(sort), this.properties);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		return new FetchableFluentQueryByPredicate<>(this.predicate, this.metaData, resultType, this.findOperation,
				this.countOperation, this.existsOperation);
	}

	@Override
	@SuppressWarnings("HiddenField")
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByPredicate<>(this.predicate, this.metaData, this.resultType, this.findOperation,
				this.countOperation, this.existsOperation, sort, mergeProperties(properties));
	}

	@Override
	public R oneValue() {

		return findOperation.find(metaData.getType())
				.as(resultType)
				.matching(
						QueryFragmentsAndParameters.forCondition(metaData,
								Cypher.adapt(predicate).asCondition(),
								null,
								CypherAdapterUtils.toSortItems(this.metaData, sort),
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

		return findOperation.find(metaData.getType())
				.as(resultType)
				.matching(
						QueryFragmentsAndParameters.forCondition(metaData,
								Cypher.adapt(predicate).asCondition(),
								null,
								CypherAdapterUtils.toSortItems(this.metaData, sort),
								createIncludedFieldsPredicate()))
				.all();
	}

	@Override
	public Page<R> page(Pageable pageable) {

		List<R> page = findOperation.find(metaData.getType())
				.as(resultType)
				.matching(
						QueryFragmentsAndParameters.forCondition(metaData,
								Cypher.adapt(predicate).asCondition(),
								pageable, null,
								createIncludedFieldsPredicate()))
				.all();

		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(page, pageable, totalCountSupplier);
	}

	@Override
	public Stream<R> stream() {
		return all().stream();
	}

	@Override
	public long count() {
		return countOperation.apply(predicate);
	}

	@Override
	public boolean exists() {
		return existsOperation.apply(predicate);
	}
}
