package org.springframework.data.neo4j.template;

import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.springframework.data.neo4j.util.IterableUtils.*;

/**
 * Spring Data template for Neo4j.  Implementation of {@link Neo4jOperations}.
 */
public class Neo4jTemplate implements Neo4jOperations {

    private final Session session;

    @Autowired
    public Neo4jTemplate(Session session) {
        this.session = session;
    }

    @Override
    public <T> T load(Class<T> type, Long id) {
        return session.load(type, id);
    }

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
        session.delete(entity);
    }

    public <T> void deleteAll(Class<T> type) {
        session.deleteAll(type);
    }

    public void execute(String jsonStatements) {
        session.execute(jsonStatements);
    }

    public void purgeSession() {
        session.purgeDatabase();
    }

    @Override
    public <T> T save(T object) {
        session.save(object);
        return object;
    }

    public <T> T save(T object, int depth) {
        session.save(object, depth);
        return object;
    }

    @Override
    public Iterable<Map<String, Object>> query(String cypher, Map<String, Object> parameters) {
        return session.query(cypher, parameters);
    }

    @Override
    public <T> Iterable<T> queryForObjects(Class<T> objectType, String cypher, Map<String, Object> parameters) {
        return session.query(objectType, cypher, parameters);
    }

    @Override
    public <T> T queryForObject(Class<T> objectType, String cypher, Map<String, Object> parameters) {
        return session.queryForObject(objectType, cypher, parameters);
    }

    @Override
    public long count(Class<?> entityClass) {
        return session.countEntitiesOfType(entityClass);
    }

}
