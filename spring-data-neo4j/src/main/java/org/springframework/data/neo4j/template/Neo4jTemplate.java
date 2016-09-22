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

package org.springframework.data.neo4j.template;


import static org.springframework.data.neo4j.util.IterableUtils.*;

import java.util.Collection;
import java.util.Map;

import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.transaction.SessionFactoryUtils;
import org.springframework.util.Assert;

/**
 * Spring Data template for Neo4j, which is an implementation of {@link Neo4jOperations}.  Indeed, framework users are encouraged
 * to favour coding against the {@link Neo4jOperations} interface rather than the {@link Neo4jTemplate} directly, as the
 * interface API will be more consistent over time and enhanced proxy objects of the interface may actually be created by Spring
 * for auto-wiring instead of this template.
 * Please note also that all methods on this class throw a {@link DataAccessException} if any underlying {@code Exception} is
 * thrown. Since {@link DataAccessException} is a runtime exception, this is not documented at the method level.
 *
 * @author Adam George
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @deprecated Use {@link org.neo4j.ogm.session.Session}
 */
@Deprecated
public class Neo4jTemplate implements Neo4jOperations {

	private final SessionFactory sessionFactory;

	/**
	 * Constructs a new {@link Neo4jTemplate} based on the given Neo4j OGM {@link SessionFactory}.
	 *
	 * @param sessionFactory The Neo4j OGM SessionFactory upon which to base the template
	 */
	@Autowired
	public Neo4jTemplate(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "Cannot create a Neo4jTemplate without a SessionFactory!");
		this.sessionFactory = sessionFactory;
	}

	@Override
	public <T> T load(Class<T> type, Long id) {
		return SessionFactoryUtils.getSession(sessionFactory).load(type, id);
	}

	@Override
	public <T> T load(Class<T> type, Long id, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).load(type, id, depth);
	}

	public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, ids);
	}

	public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, ids, depth);
	}

	@Override
	public <T> Collection<T> loadAll(Class<T> type) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type);
	}

	@Override
	public <T> Collection<T> loadAll(Class<T> type, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, depth);
	}

	@Override
	public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, sortOrder, depth);
	}

	@Override
	public <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, Pagination pagination, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, sortOrder, pagination, depth);
	}

	public <T> Collection<T> loadAll(Collection<T> objects) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(objects);
	}

	@Override
	public <T> Collection<T> loadAll(Collection<T> objects, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(objects, depth);
	}

	@Override
	public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, ids, sortOrder, depth);
	}

	@Override
	public <T> Collection<T> loadAll(Class<T> type, Filter filter) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, filter);
	}

	@Override
	public <T> Collection<T> loadAll(Class<T> type, Pagination pagination, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, pagination, depth);
	}

	@Override
	public <T> Collection<T> loadAll(Class<T> type, Filter filter, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, filter, depth);
	}

	@Override
	public <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue) {
		return loadByProperty(type, propertyName, propertyValue, 1);
	}

	@Override
	public <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue, int depth) {
		return getSingle(loadAllByProperty(type, propertyName, propertyValue, depth));
	}

	public <T> T loadByPropertyOrNull(Class<T> type, String propertyName, Object propertyValue) {
		return getSingleOrNull(loadAllByProperty(type, propertyName, propertyValue));
	}

	@Override
	public <T> Collection<T> loadAllByProperty(Class<T> type, String name, Object value) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, new Filter(name, value));
	}

	@Override
	public <T> T loadByProperties(Class<T> type, Filters parameters) {
		return loadByProperties(type, parameters, 1);
	}

	@Override
	public <T> T loadByProperties(Class<T> type, Filters parameters, int depth) {
		return getSingle(loadAllByProperties(type, parameters, depth));
	}

	@Override
	public <T> Collection<T> loadAllByProperties(Class<T> type, Filters parameters) {
		return loadAllByProperties(type, parameters, 1);
	}

	@Override
	public <T> Collection<T> loadAllByProperties(Class<T> type, Filters parameters, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, parameters, depth);
	}

	public <T> Collection<T> loadAllByProperty(Class<T> type, String name, Object value, int depth) {
		return SessionFactoryUtils.getSession(sessionFactory).loadAll(type, new Filter(name, value), depth);
	}

	@Override
	public void delete(Object entity) {
		SessionFactoryUtils.getSession(sessionFactory).delete(entity);
	}

	@Override
	public void clear() {
		SessionFactoryUtils.getSession(sessionFactory).clear();
	}

	public <T> void deleteAll(Class<T> type) {
		SessionFactoryUtils.getSession(sessionFactory).deleteAll(type);
	}

//    @Override
//    public QueryStatistics execute(String jsonStatements) {
//        return SessionFactoryUtils.getSession(sessionFactory).query(jsonStatements, Utils.map()).queryStatistics();
//    }
//
//    @Override
//    public QueryStatistics execute(String cypher, Map<String, Object> parameters) {
//        return SessionFactoryUtils.getSession(sessionFactory).query(cypher, parameters).queryStatistics();
//    }

	public void purgeSession() {
		SessionFactoryUtils.getSession(sessionFactory).clear();
	}

	@Override
	public <T> T save(T entity) {
		SessionFactoryUtils.getSession(sessionFactory).save(entity);
		return entity;
	}

	public <T> T save(T entity, int depth) {
		SessionFactoryUtils.getSession(sessionFactory).save(entity, depth);
		return entity;
	}

	@Override
	public Result query(String cypher, Map<String, ?> parameters) {
		return SessionFactoryUtils.getSession(sessionFactory).query(cypher, parameters);
	}

	@Override
	public <T> Iterable<T> queryForObjects(Class<T> objectType, String cypher, Map<String, ?> parameters) {
		return SessionFactoryUtils.getSession(sessionFactory).query(objectType, cypher, parameters);
	}

	@Override
	public Result query(String cypher, Map<String, ?> parameters, boolean readOnly) {
		return SessionFactoryUtils.getSession(sessionFactory).query(cypher, parameters, readOnly);
	}

	@Override
	public <T> T queryForObject(Class<T> objectType, String cypher, Map<String, ?> parameters) {
		return SessionFactoryUtils.getSession(sessionFactory).queryForObject(objectType, cypher, parameters);
	}

	@Override
	public long count(Class<?> entityClass) {
		return SessionFactoryUtils.getSession(sessionFactory).countEntitiesOfType(entityClass);
	}
}
