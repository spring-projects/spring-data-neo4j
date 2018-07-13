/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.session.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.util.PagingAndSortingUtils;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link org.springframework.data.repository.CrudRepository} interface. This will offer
 * you a more sophisticated interface than the plain {@link Session} .
 *
 * @param <T> the type of the entity to handle
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Gerrit Meier
 */
@Repository
@Transactional(readOnly = true)
public class SimpleNeo4jRepository<T, ID extends Serializable> implements Neo4jRepository<T, ID> {

	private static final int DEFAULT_QUERY_DEPTH = 1;
	private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";

	private final Class<T> clazz;
	private final Session session;

	/**
	 * Creates a new {@link SimpleNeo4jRepository} to manage objects of the given domain type.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param session must not be {@literal null}.
	 */
	public SimpleNeo4jRepository(Class<T> domainClass, Session session) {
		Assert.notNull(domainClass, "Domain class must not be null!");
		Assert.notNull(session, "Session must not be null!");

		this.clazz = domainClass;
		this.session = session;
	}

	@Transactional
	@Override
	public <S extends T> S save(S entity) {
		session.save(entity);
		return entity;
	}

	@Transactional
	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		session.save(entities);
		return entities;
	}

	@Override
	public Optional<T> findById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return Optional.ofNullable(session.load(clazz, id));
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	public long count() {
		return session.countEntitiesOfType(clazz);
	}

	@Transactional
	@Override
	public void deleteById(ID id) {
		findById(id).ifPresent(session::delete);
	}

	@Transactional
	@Override
	public void delete(T t) {
		session.delete(t);
	}

	@Transactional
	@Override
	public void deleteAll(Iterable<? extends T> ts) {
		for (T t : ts) {
			session.delete(t);
		}
	}

	@Transactional
	@Override
	public void deleteAll() {
		session.deleteAll(clazz);
	}

	@Transactional
	@Override
	public <S extends T> S save(S s, int depth) {
		session.save(s, depth);
		return s;
	}

	@Transactional
	@Override
	public <S extends T> Iterable<S> save(Iterable<S> ses, int depth) {
		session.save(ses, depth);
		return ses;
	}

	@Override
	public Optional<T> findById(ID id, int depth) {
		return Optional.ofNullable(session.load(clazz, id, depth));
	}

	// findAll and variants
	@Override
	public Iterable<T> findAll() {
		return findAll(DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Iterable<T> findAll(int depth) {
		return session.loadAll(clazz, depth);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> longs) {
		return findAllById(longs, DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids, int depth) {
		return session.loadAll(clazz, (Collection<ID>) ids, depth);
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		return findAll(sort, DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Iterable<T> findAll(Sort sort, int depth) {
		return session.loadAll(clazz, PagingAndSortingUtils.convert(sort), depth);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids, Sort sort) {
		return findAllById(ids, sort, DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids, Sort sort, int depth) {
		return session.loadAll(clazz, (Collection<ID>) ids, PagingAndSortingUtils.convert(sort), depth);
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return findAll(pageable, DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Page<T> findAll(Pageable pageable, int depth) {
		Pagination pagination = new Pagination(pageable.getPageNumber(), pageable.getPageSize());
		Collection<T> data = session.loadAll(clazz, PagingAndSortingUtils.convert(pageable.getSort()), pagination, depth);

		return PageableExecutionUtils.getPage(new ArrayList<>(data), pageable, () -> session.countEntitiesOfType(clazz));
	}
}
