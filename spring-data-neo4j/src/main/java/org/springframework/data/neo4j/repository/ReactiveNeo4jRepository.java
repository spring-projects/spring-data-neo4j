package org.springframework.data.neo4j.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface ReactiveNeo4jRepository<T, ID extends Serializable> extends ReactiveSortingRepository<T, ID> {

	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
	 * entity instance completely.
	 *
	 * @param entity must not be {@literal null}.
	 * @param depth depth of query
	 * @return {@link Mono} emitting the saved entity.
	 * @throws IllegalArgumentException in case the given {@code entity} is {@literal null}.
	 */
	<S extends T> Mono<S> save(S entity, int depth);

	/**
	 * Retrieves an entity by its id.
	 *
	 * @param id must not be {@literal null}.
	 * @param depth depth of query
	 * @return {@link Mono} emitting the entity with the given id or {@link Mono#empty()} if none found.
	 * @throws IllegalArgumentException in case the given {@code id} is {@literal null}.
	 */
	Mono<T> findById(ID id, int depth);

	/**
	 * Returns all instances of the type.
	 * 
	 * @param depth depth of query
	 * @return {@link Flux} emitting all entities.
	 */
	Flux<T> findAll(int depth);

	/**
	 * Returns all entities sorted by the given options.
	 *
	 * @param sort must not be {@literal null}.
	 * @param depth depth of query
	 * @return all entities sorted by the given options.
	 * @throws IllegalArgumentException in case the given {@link Sort} is {@literal null}.
	 */
	Flux<T> findAll(Sort sort, int depth);

	/**
	 * Returns all instances with the given IDs.
	 *
	 * @param ids must not be {@literal null}.
	 * @param depth depth of query
	 * @return {@link Flux} emitting the found entities.
	 * @throws IllegalArgumentException in case the given {@link Iterable} {@code ids} is {@literal null}.
	 */
	Flux<T> findAllById(Iterable<ID> ids, int depth);

	/**
	 * Returns all instances with the given IDs sorted by the given options.
	 *
	 * @param ids must not be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @return {@link Flux} emitting the found entities.
	 * @throws IllegalArgumentException in case the given {@link Iterable} {@code ids} is {@literal null}.
	 */
	Flux<T> findAllById(Iterable<ID> ids, Sort sort);

	/**
	 * Returns all instances with the given IDs sorted by the given options.
	 *
	 * @param ids must not be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @param depth depth of query
	 * @return {@link Flux} emitting the found entities.
	 * @throws IllegalArgumentException in case the given {@link Iterable} {@code ids} is {@literal null}.
	 */
	Flux<T> findAllById(Iterable<ID> ids, Sort sort, int depth);

	// /**
	// * Returns a {@link Page} of entities meeting the paging restriction provided in the {@code Pageable} object.
	// * {@link Page#getTotalPages()} returns an estimation of the total number of pages and should not be relied upon for
	// accuracy.
	// *
	// * @param pageable page to be retrieved
	// * @return a page of entities
	// */
	// Mono<Page<T>> findAll(Pageable pageable);
	//
	// /**
	// * Returns a {@link Page} of entities meeting the paging restriction provided in the {@code Pageable} object.
	// * {@link Page#getTotalPages()} returns an estimation of the total number of pages and should not be relied upon for
	// accuracy.
	// *
	// * @param pageable page to be retrieved
	// * @param depth depth of query
	// * @return a page of entities
	// */
	// Mono<Page<T>> findAll(Pageable pageable, int depth);
}
