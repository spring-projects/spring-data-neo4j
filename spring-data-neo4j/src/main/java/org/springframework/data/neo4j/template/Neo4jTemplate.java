/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc.", "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.template;

import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.neo4j.event.*;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Map;

import static org.springframework.data.neo4j.util.IterableUtils.getSingle;
import static org.springframework.data.neo4j.util.IterableUtils.getSingleOrNull;

/**
 * Spring Data template for Neo4j.  Implementation of {@link Neo4jOperations}.
 */
public class Neo4jTemplate implements Neo4jOperations, ApplicationEventPublisherAware {

    private final Session session;
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * Constructs a new {@link Neo4jTemplate} based on the given Neo4j OGM {@link Session}.
     *
     * @param session The Neo4j OGM session upon which to base the template
     * @throws NullPointerException if the given {@link Session} is <code>null</code>
     */
    @Autowired
    public Neo4jTemplate(Session session) {
        Assert.notNull(session, "Cannot create a Neo4jTemplate without a Session!");
        this.session = session;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public <T> T load(Class<T> type, Long id) {
        return session.load(type, id);
    }

    @Override
    public <T> T load(Class<T> type, Long id, int depth) {
        return session.load(type, id, depth);
    }

    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids) {
        return session.loadAll(type, ids);
    }

    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth) {
        return session.loadAll(type, ids, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type) {
        return session.loadAll(type);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, int depth) {
        return session.loadAll(type, depth);
    }

    public <T> Collection<T> loadAll(Collection<T> objects) {
        return session.loadAll(objects);
    }

    @Override
    public <T> Collection<T> loadAll(Collection<T> objects, int depth) {
        return session.loadAll(objects, depth);
    }

    @Override
    public <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue) {
        return getSingle(loadAllByProperty(type, propertyName, propertyValue));
    }

    public <T> T loadByPropertyOrNull(Class<T> type, String propertyName, Object propertyValue) {
        return getSingleOrNull(loadAllByProperty(type, propertyName, propertyValue));
    }

    @Override
    public <T> Collection<T> loadAllByProperty(Class<T> type, String name, Object value) {
        return session.loadByProperty(type, Property.with(name, value));
    }

    public <T> Collection<T> loadAllByProperty(Class<T> type, String name, Object value, int depth) {
        return session.loadByProperty(type, Property.with(name, value), depth);
    }

    @Override
    public void delete(Object entity) {
        publishEvent(new BeforeDeleteEvent(this, entity));
        session.delete(entity);
        publishEvent(new AfterDeleteEvent(this, entity));
    }

    public <T> void deleteAll(Class<T> type) {
        session.deleteAll(type);
    }

    @Override
    public void execute(String jsonStatements) {
        session.execute(jsonStatements);
    }

    public void purgeSession() {
        session.clear();
    }

    @Override
    public <T> T save(T entity) {
        publishEvent(new BeforeSaveEvent(this, entity));
        session.save(entity);
        publishEvent(new AfterSaveEvent(this, entity));
        return entity;
    }

    public <T> T save(T entity, int depth) {
        publishEvent(new BeforeSaveEvent(this, entity));
        session.save(entity, depth);
        publishEvent(new AfterSaveEvent(this, entity));
        return entity;
    }

    @Override
    public Iterable<Map<String, Object>> query(String cypher, Map<String, ?> parameters) {
        return session.query(cypher, parameters);
    }

    @Override
    public <T> Iterable<T> queryForObjects(Class<T> objectType, String cypher, Map<String, ?> parameters) {
        return session.query(objectType, cypher, parameters);
    }

    @Override
    public <T> T queryForObject(Class<T> objectType, String cypher, Map<String, ?> parameters) {
        return session.queryForObject(objectType, cypher, parameters);
    }

    @Override
    public long count(Class<?> entityClass) {
        return session.countEntitiesOfType(entityClass);
    }

    private void publishEvent(Neo4jDataManipulationEvent event) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(event);
        }
    }

}
