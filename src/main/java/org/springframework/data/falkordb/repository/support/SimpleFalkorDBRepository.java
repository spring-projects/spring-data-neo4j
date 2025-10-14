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
package org.springframework.data.falkordb.repository.support;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apiguardian.api.API;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.falkordb.core.FalkorDBOperations;
import org.springframework.data.falkordb.repository.FalkorDBRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository base implementation for FalkorDB.
 *
 * @param <T> the type of the domain class managed by this repository
 * @param <ID> the type of the unique identifier of the domain class
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Repository
@Transactional(readOnly = true)
@API(status = API.Status.STABLE, since = "1.0")
public class SimpleFalkorDBRepository<T, ID> implements FalkorDBRepository<T, ID> {

	/**
	 * The FalkorDB operations template.
	 */
	private final FalkorDBOperations falkorDBOperations;

	/**
	 * Entity information for type T.
	 */
	private final FalkorDBEntityInformation<T, ID> entityInformation;

	/**
	 * Creates a new {@link SimpleFalkorDBRepository} for the given
	 * {@link FalkorDBEntityInformation} and {@link FalkorDBOperations}.
	 * @param falkorDBOperations must not be {@literal null}
	 * @param entityInformation must not be {@literal null}
	 */
	protected SimpleFalkorDBRepository(FalkorDBOperations falkorDBOperations,
			FalkorDBEntityInformation<T, ID> entityInformation) {
		this.falkorDBOperations = falkorDBOperations;
		this.entityInformation = entityInformation;
	}

	@Override
	public Optional<T> findById(ID id) {
		return this.falkorDBOperations.findById(id, this.entityInformation.getJavaType());
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		return this.falkorDBOperations.findAllById(ids, this.entityInformation.getJavaType());
	}

	@Override
	public List<T> findAll() {
		return this.falkorDBOperations.findAll(this.entityInformation.getJavaType());
	}

	@Override
	public List<T> findAll(Sort sort) {
		return this.falkorDBOperations.findAll(this.entityInformation.getJavaType(), sort);
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		// For now, we'll implement basic pagination - this could be enhanced
		// later
		List<T> allResults = findAll(pageable.getSort());
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), allResults.size());
		List<T> pageContent = allResults.subList(start, end);

		return new PageImpl<>(pageContent, pageable, allResults.size());
	}

	@Override
	public long count() {
		return this.falkorDBOperations.count(this.entityInformation.getJavaType());
	}

	@Override
	public boolean existsById(ID id) {
		return this.falkorDBOperations.existsById(id, this.entityInformation.getJavaType());
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {
		return this.falkorDBOperations.save(entity);
	}

	@Override
	@Transactional
	public <S extends T> List<S> saveAll(Iterable<S> entities) {
		return this.falkorDBOperations.saveAll(entities);
	}

	@Override
	@Transactional
	public void deleteById(ID id) {
		this.falkorDBOperations.deleteById(id, this.entityInformation.getJavaType());
	}

	@Override
	@Transactional
	public void delete(T entity) {
		ID id = Objects.requireNonNull(this.entityInformation.getId(entity),
				"Cannot delete individual entities without an id");
		this.deleteById(id);
	}

	@Override
	@Transactional
	public void deleteAllById(Iterable<? extends ID> ids) {
		this.falkorDBOperations.deleteAllById(ids, this.entityInformation.getJavaType());
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {
		List<ID> ids = StreamSupport.stream(entities.spliterator(), false)
			.map(this.entityInformation::getId)
			.collect(Collectors.toList());

		this.falkorDBOperations.deleteAllById(ids, this.entityInformation.getJavaType());
	}

	@Override
	@Transactional
	public void deleteAll() {
		this.falkorDBOperations.deleteAll(this.entityInformation.getJavaType());
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	@Override
	public <S extends T, R> R findBy(Example<S> example,
			java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
		// Placeholder implementation - would need to be enhanced with proper
		// fluent query support
		throw new UnsupportedOperationException("Fluent query API not yet implemented");
	}

	/**
	 * Simple PageImpl for basic pagination support.
	 */
	private static class PageImpl<T> implements Page<T> {

		private final List<T> content;

		private final Pageable pageable;

		private final long total;

		PageImpl(List<T> content, Pageable pageable, long total) {
			this.content = content;
			this.pageable = pageable;
			this.total = total;
		}

		@Override
		public List<T> getContent() {
			return this.content;
		}

		@Override
		public Pageable getPageable() {
			return this.pageable;
		}

		@Override
		public long getTotalElements() {
			return this.total;
		}

		@Override
		public int getTotalPages() {
			return (int) Math.ceil((double) this.total / this.pageable.getPageSize());
		}

		@Override
		public boolean hasContent() {
			return !this.content.isEmpty();
		}

		@Override
		public Sort getSort() {
			return this.pageable.getSort();
		}

		@Override
		public boolean isFirst() {
			return this.pageable.getPageNumber() == 0;
		}

		@Override
		public boolean isLast() {
			return this.pageable.getPageNumber() >= getTotalPages() - 1;
		}

		@Override
		public boolean hasNext() {
			return !isLast();
		}

		@Override
		public boolean hasPrevious() {
			return !isFirst();
		}

		@Override
		public Pageable nextPageable() {
			return hasNext() ? this.pageable.next() : Pageable.unpaged();
		}

		@Override
		public Pageable previousPageable() {
			return hasPrevious() ? this.pageable.previousOrFirst() : Pageable.unpaged();
		}

		@Override
		public int getSize() {
			return this.pageable.getPageSize();
		}

		@Override
		public int getNumber() {
			return this.pageable.getPageNumber();
		}

		@Override
		public int getNumberOfElements() {
			return this.content.size();
		}

		// Additional required methods from Slice interface
		@Override
		public java.util.Iterator<T> iterator() {
			return this.content.iterator();
		}

		@Override
		public <U> Page<U> map(java.util.function.Function<? super T, ? extends U> converter) {
			List<U> convertedContent = this.content.stream()
				.map(converter)
				.collect(java.util.stream.Collectors.toList());
			return new PageImpl<>(convertedContent, this.pageable, this.total);
		}

	}

}
