package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.Session;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.util.PagingAndSortingUtils;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Collection;

public class SimpleReactiveNeo4jRepository<T, ID extends Serializable> implements ReactiveNeo4jRepository<T, ID> {
	private static final int DEFAULT_QUERY_DEPTH = 1;
	private final Class<T> clazz;
	private final Session session;

	/**
	 * Creates a new {@link SimpleNeo4jRepository} to manage objects of the given domain type.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param session     must not be {@literal null}.
	 */
	public SimpleReactiveNeo4jRepository(Class<T> domainClass, Session session) {
		Assert.notNull(domainClass, "Domain class must not be null!");
		Assert.notNull(session, "Session must not be null!");

		this.clazz = domainClass;
		this.session = session;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#save(java.lang.Object)
	 */
	@Override
	public <S extends T> Mono<S> save(S entity) {
		Assert.notNull(entity, "Entity must not be null!");
		return Mono.fromRunnable(() -> session.save(entity))
				.then(Mono.just(entity));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#saveAll(java.lang.Iterable)
	 */
	@Override
	public <S extends T> Flux<S> saveAll(Iterable<S> entities) {
		Assert.notNull(entities, "The given Iterable of entities must not be null!");
		return Flux.fromIterable(entities).flatMap(this::save);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#saveAll(org.reactivestreams.Publisher)
	 */
	@Override
	public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {
		Assert.notNull(entityStream, "The given Publisher of entities must not be null!");
		return Flux.from(entityStream)
				.flatMap(this::save);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#findById(java.lang.Object)
	 */
	@Override
	public Mono<T> findById(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		return Mono.just(session.load(clazz, id));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#findById(org.reactivestreams.Publisher)
	 */
	@Override
	public Mono<T> findById(Publisher<ID> publisher) {
		Assert.notNull(publisher, "The given id must not be null!");
		return Mono.from(publisher).flatMap(
				this::findById);

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#existsById(java.lang.Object)
	 */
	@Override
	public Mono<Boolean> existsById(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		return findById(id).hasElement();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#existsById(org.reactivestreams.Publisher)
	 */
	@Override
	public Mono<Boolean> existsById(Publisher<ID> publisher) {
		return findById(publisher).hasElement();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#count()
	 */
	@Override
	public Mono<Long> count() {
		return Mono.just(session.countEntitiesOfType(clazz));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteById(java.lang.Object)
	 */
	@Override
	public Mono<Void> deleteById(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		return findById(id).flatMap(t -> Mono.fromRunnable(() -> session.delete(t))).then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteById(org.reactivestreams.Publisher)
	 */
	@Override
	public Mono<Void> deleteById(Publisher<ID> publisher) {
		Assert.notNull(publisher, "Id must not be null!");

		return Mono.from(publisher).flatMap(this::deleteById).then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#delete(java.lang.Object)
	 */
	@Override
	public Mono<Void> delete(T t) {
		return Mono.fromRunnable(() -> session.delete(t)).then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteAll(java.lang.Iterable)
	 */
	@Override
	public Mono<Void> deleteAll(Iterable<? extends T> ts) {
		Assert.notNull(ts, "The given Iterable of entities must not be null!");

		return Flux.fromIterable(ts).flatMap(this::delete).then();

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteAll(org.reactivestreams.Publisher)
	 */
	@Override
	public Mono<Void> deleteAll(Publisher<? extends T> entityStream) {
		Assert.notNull(entityStream, "The given Publisher of entities must not be null!");

		return Flux.from(entityStream)
				.flatMap(this::delete)
				.then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteAll()
	 */
	@Override
	public Mono<Void> deleteAll() {
		return Mono.fromRunnable(() -> session.deleteAll(clazz)).then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveNeo4jRepository#save(java.lang.Object, int)
	 */
	@Override
	public <S extends T> Mono<S> save(S entity, int depth) {
		Assert.notNull(entity, "Entity must not be null!");
		return Mono.fromRunnable(() -> session.save(entity, depth))
				.then(Mono.just(entity));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveNeo4jRepository#findById(java.lang.Object, int)
	 */
	@Override
	public Mono<T> findById(ID id, int depth) {
		Assert.notNull(id, "The given id must not be null!");
		return Mono.just(session.load(clazz, id, depth));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#findAll()
	 */
	@Override
	public Flux<T> findAll() {

		return findAll(DEFAULT_QUERY_DEPTH);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveNeo4jRepository#findAll(int)
	 */
	@Override
	public Flux<T> findAll(int depth) {
		return Flux.fromIterable(session.loadAll(clazz, depth));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#findAllById(java.lang.Iterable)
	 */
	@Override
	public Flux<T> findAllById(Iterable<ID> longs) {
		return findAllById(longs, DEFAULT_QUERY_DEPTH);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#findAllById(org.reactivestreams.Publisher)
	 */
	@Override
	public Flux<T> findAllById(Publisher<ID> ids) {
		Assert.notNull(ids, "The given Publisher of Id's must not be null!");

		return Flux.from(ids).buffer().flatMap(this::findAllById);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveNeo4jRepository#findAllById(java.lang.Iterable, int)
	 */
	@Override
	public Flux<T> findAllById(Iterable<ID> ids, int depth) {
		return Flux.fromIterable(
				session.loadAll(clazz, (Collection<ID>) ids, depth)
		);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveSortingRepository#findAll(org.springframework.data.domain.Sort)
	 */
	@Override
	public Flux<T> findAll(Sort sort) {
		return findAll(sort, DEFAULT_QUERY_DEPTH);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveNeo4jRepository#findAll
	 * (org.springframework.data.domain.Sort, int)
	 */
	@Override
	public Flux<T> findAll(Sort sort, int depth) {
		return Flux.fromIterable(session.loadAll(clazz, PagingAndSortingUtils.convert(sort), depth));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveNeo4jRepository#findAll
	 * (java.lang.Iterable, org.springframework.data.domain.Sort)
	 */
	@Override
	public Flux<T> findAllById(Iterable<ID> ids, Sort sort) {
		return findAllById(ids, sort, DEFAULT_QUERY_DEPTH);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveNeo4jRepository#findAll
	 * (java.lang.Iterable, org.springframework.data.domain.Sort, int)
	 */
	@Override
	public Flux<T> findAllById(Iterable<ID> ids, Sort sort, int depth) {
		return Flux.fromIterable(session.loadAll(clazz, (Collection<ID>) ids, PagingAndSortingUtils.convert(sort), depth));
	}
}
