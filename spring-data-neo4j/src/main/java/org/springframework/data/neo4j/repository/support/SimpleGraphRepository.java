/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.session.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link org.springframework.data.repository.CrudRepository} interface. This will offer
 * you a more sophisticated interface than the plain {@link Session} .
 *
 * @param <T> the type of the entity to handle
 *
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
@Repository
@Transactional(readOnly = true)
public class SimpleGraphRepository<T> implements GraphRepository<T> {

	private static final int DEFAULT_QUERY_DEPTH = 1;
	private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";

	private Class<T> clazz;
	private Session session;

	/**
	 * Creates a new {@link SimpleGraphRepository} to manage objects of the given domain type.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param session must not be {@literal null}.
	 */
	public SimpleGraphRepository(Class<T> domainClass, Session session) {
		this.clazz = domainClass;
		this.session = session;
	}

	protected Class<T> getDomainClass() {
		return clazz;
	}

	@Transactional
	@Override
	public <S extends T> S save(S entity) {
		session.save(entity);
		return entity;
	}

	@Transactional
	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		for (S entity : entities) {
			session.save(entity);
		}
		return entities;
	}

	@Override
	public T findOne(Long id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return session.load(clazz, id);
	}

	@Override
	public boolean exists(Long id) {
		return findOne(id) != null;
	}

	@Override
	public long count() {
		return session.countEntitiesOfType(clazz);
	}

	@Transactional
	@Override
	public void delete(Long id) {
		Object o = findOne(id);
		if (o != null) {
			session.delete(o);
		}
	}

	@Transactional
	@Override
	public void delete(T t) {
		session.delete(t);
	}

	@Transactional
	@Override
	public void delete(Iterable<? extends T> ts) {
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
	public T findOne(Long id, int depth) {
		return session.load(clazz, id, depth);
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
	public Iterable<T> findAll(Iterable<Long> longs) {
		return findAll(longs, DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Iterable<T> findAll(Iterable<Long> ids, int depth) {
		return session.loadAll(clazz, (Collection<Long>) ids, depth);
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		return findAll(sort, DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Iterable<T> findAll(Sort sort, int depth) {
		return session.loadAll(clazz, convert(sort), depth);
	}

	@Override
	public Iterable<T> findAll(Iterable<Long> ids, Sort sort) {
		return findAll(ids, sort, DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Iterable<T> findAll(Iterable<Long> ids, Sort sort, int depth) {
		return session.loadAll(clazz, (Collection<Long>) ids, convert(sort), depth);
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return findAll(pageable, DEFAULT_QUERY_DEPTH);
	}

	@Override
	public Page<T> findAll(Pageable pageable, int depth) {
		Collection<T> data = session.loadAll(clazz, convert(pageable.getSort()), new Pagination(pageable.getPageNumber(), pageable.getPageSize()), depth);
		return updatePage(pageable, new ArrayList<T>(data));
	}

	/*
	 * Converts a Spring Data Sort object to an OGM SortOrder
	 */
	private SortOrder convert(Sort sort) {

		SortOrder sortOrder = new SortOrder();

		if (sort != null) {
			for (Sort.Order order : sort) {
				if (order.isAscending()) {
					sortOrder.add(order.getProperty());
				} else {
					sortOrder.add(SortOrder.Direction.DESC, order.getProperty());
				}
			}
		}
		return sortOrder;
	}

	/*
	 * This is a cheap trick to estimate the total number of objects without actually knowing the real value.
	 * Essentially, if the result size is the same as the page size, we assume more data can be fetched, so
	 * we set the expected total to the current total retrieved so far + the current page size. As soon as the
	 * result size is less than the page size, we know there are no more, so we set the total to the number
	 * retrieved so far. This will ensure that page.next() returns false.
	 */
	private Page<T> updatePage(Pageable pageable, List<T> results) {
		int pageSize = pageable.getPageSize();
		int pageOffset = pageable.getOffset();
		int total = pageOffset + results.size() + (results.size() == pageSize ? pageSize : 0);

		return new PageImpl<T>(results, pageable, total);
	}
}
