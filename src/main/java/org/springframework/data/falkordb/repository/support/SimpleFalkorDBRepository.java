/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
	private final FalkorDBOperations operations;

	/**
	 * Entity information for type T.
	 */
	private final FalkorDBEntityInformation<T, ID> entityInfo;

	/**
	 * Creates a new {@link SimpleFalkorDBRepository} for the given
	 * {@link FalkorDBEntityInformation} and {@link FalkorDBOperations}.
	 * @param falkorDBOperations must not be {@literal null}
	 * @param entityInformation must not be {@literal null}
	 */
	protected SimpleFalkorDBRepository(final FalkorDBOperations falkorDBOperations,
			final FalkorDBEntityInformation<T, ID> entityInformation) {
		this.operations = falkorDBOperations;
		this.entityInfo = entityInformation;
	}

	@Override
	public final Optional<T> findById(final ID id) {
		return this.operations.findById(id, this.entityInfo.getJavaType());
	}

	@Override
	public final List<T> findAllById(final Iterable<ID> ids) {
		return this.operations.findAllById(ids, this.entityInfo.getJavaType());
	}

	@Override
	public final List<T> findAll() {
		return this.operations.findAll(this.entityInfo.getJavaType());
	}

	@Override
	public final List<T> findAll(final Sort sort) {
		return this.operations.findAll(this.entityInfo.getJavaType(), sort);
	}

	@Override
	public final Page<T> findAll(final Pageable pageable) {
		// For now, we'll implement basic pagination - this could be enhanced
		// later
		List<T> allResults = findAll(pageable.getSort());
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), allResults.size());
		List<T> pageContent = allResults.subList(start, end);

		return new PageImpl<>(pageContent, pageable, allResults.size());
	}

	@Override
	public final long count() {
		return this.operations.count(this.entityInfo.getJavaType());
	}

	@Override
	public final boolean existsById(final ID id) {
		return this.operations.existsById(id, this.entityInfo.getJavaType());
	}

	@Override
	@Transactional
	public final <S extends T> S save(final S entity) {
		return this.operations.save(entity);
	}

	@Override
	@Transactional
	public final <S extends T> List<S> saveAll(final Iterable<S> entities) {
		return this.operations.saveAll(entities);
	}

	@Override
	@Transactional
	public final void deleteById(final ID id) {
		this.operations.deleteById(id, this.entityInfo.getJavaType());
	}

	@Override
	@Transactional
	public final void delete(final T entity) {
		ID id = Objects.requireNonNull(this.entityInfo.getId(entity),
				"Cannot delete individual entities without an id");
		this.deleteById(id);
	}

	/**
	 * Deletes all entities by their IDs.
	 * @param ids the IDs of entities to delete
	 */
	@Override
	@Transactional
	public final void deleteAllById(final Iterable<? extends ID> ids) {
		this.operations.deleteAllById(ids, this.entityInfo.getJavaType());
	}

	/**
	 * Deletes all given entities.
	 * @param entities the entities to delete
	 */
	@Override
	@Transactional
	public final void deleteAll(final Iterable<? extends T> entities) {
		List<ID> ids = StreamSupport.stream(entities.spliterator(), false)
			.map(this.entityInfo::getId)
			.collect(Collectors.toList());

		this.operations.deleteAllById(ids, this.entityInfo.getJavaType());
	}

	/**
	 * Deletes all entities.
	 */
	@Override
	@Transactional
	public final void deleteAll() {
		this.operations.deleteAll(this.entityInfo.getJavaType());
	}

	/**
	 * Finds a single entity by example.
	 * @param example the example to match against
	 * @return the matching entity or empty
	 */
	@Override
	public final <S extends T> Optional<S> findOne(final Example<S> example) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	/**
	 * Finds all entities by example.
	 * @param example the example to match against
	 * @return the matching entities
	 */
	@Override
	public final <S extends T> List<S> findAll(final Example<S> example) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	/**
	 * Finds all entities by example with sorting.
	 * @param example the example to match against
	 * @param sort the sort specification
	 * @return the matching entities
	 */
	@Override
	public final <S extends T> List<S> findAll(final Example<S> example, final Sort sort) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	/**
	 * Finds all entities by example with pagination.
	 * @param example the example to match against
	 * @param pageable the pagination specification
	 * @return the matching entities page
	 */
	@Override
	public final <S extends T> Page<S> findAll(final Example<S> example, final Pageable pageable) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	/**
	 * Counts entities by example.
	 * @param example the example to match against
	 * @return the count of matching entities
	 */
	@Override
	public final <S extends T> long count(final Example<S> example) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	/**
	 * Checks if entities exist by example.
	 * @param example the example to match against
	 * @return true if matching entities exist
	 */
	@Override
	public final <S extends T> boolean exists(final Example<S> example) {
		// Placeholder implementation - would need to be enhanced with proper
		// query-by-example support
		throw new UnsupportedOperationException("Query by example not yet implemented");
	}

	/**
	 * Fluent query by example.
	 * @param example the example to match against
	 * @param queryFunction the query function
	 * @return the query result
	 */
	@Override
	public final <S extends T, R> R findBy(final Example<S> example,
			final java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
		// Placeholder implementation - would need to be enhanced with proper
		// fluent query support
		throw new UnsupportedOperationException("Fluent query API not yet implemented");
	}

	/**
	 * Simple PageImpl for basic pagination support.
	 *
	 * @param <T> the content type
	 */
	private static class PageImpl<T> implements Page<T> {

		/**
		 * The page content.
		 */
		private final List<T> content;

		/**
		 * The pageable information.
		 */
		private final Pageable pageable;

		/**
		 * The total number of elements.
		 */
		private final long total;

		/**
		 * Creates a new PageImpl.
		 * @param pageContent the content of this page
		 * @param pageRequest the paging information
		 * @param totalElements the total number of elements
		 */
		PageImpl(final List<T> pageContent, final Pageable pageRequest, final long totalElements) {
			this.content = pageContent;
			this.pageable = pageRequest;
			this.total = totalElements;
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
		public <U> Page<U> map(final java.util.function.Function<? super T, ? extends U> converter) {
			List<U> convertedContent = this.content.stream()
				.map(converter)
				.collect(java.util.stream.Collectors.toList());
			return new PageImpl<>(convertedContent, this.pageable, this.total);
		}

	}

}
