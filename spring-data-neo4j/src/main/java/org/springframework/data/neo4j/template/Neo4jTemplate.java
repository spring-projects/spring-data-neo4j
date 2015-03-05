package org.springframework.data.neo4j.template;

import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Map;

import static org.springframework.data.neo4j.util.IterableUtils.*;

/**
 *  todo discuss whether we should have this at all
 */
public class Neo4jTemplate implements Neo4jOperations {

    private static final int DEFAULT_DEPTH = 1;

    private final Session session;

    @Autowired
    public Neo4jTemplate(Session session) {
        this.session = session;
    }

    public <T> T load(Class<T> type, Long id) {
        return session.load(type, id);
    }

    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids) {
        return session.loadAll(type, ids);
    }

    public <T> T load(Class<T> type, Long id, int depth) {
        return session.load(type, id, depth);
    }

    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth) {
        return session.loadAll(type, ids, depth);
    }

    public <T> Collection<T> loadAll(Class<T> type) {
        return session.loadAll(type);
    }

    public <T> Collection<T> loadAll(Class<T> type, int depth) {
        return session.loadAll(type, depth);
    }

    public <T> void delete(T object) {
        session.delete(object);
    }

    public void execute(String jsonStatements) {
        session.execute(jsonStatements);
    }

    public <T> Collection<T> loadAll(Collection<T> objects, int depth) {
        return session.loadAll(objects, depth);
    }

    public void purgeSession() {
        session.purgeDatabase();
    }

    public <T> T save(T object) {
        session.save(object);
        return object;
    }

    public <T> Collection<T> loadAll(Collection<T> objects) {
        return session.loadAll(objects);
    }

    public <T> void deleteAll(Class<T> type) {
        session.deleteAll(type);
    }

    public <T> T loadSingleByProperty(Class<T> type, String name, Object value) {
        return getSingle(loadByProperty(type, name, value));
    }

    public <T> T loadSingleOrNullByProperty(Class<T> type, String name, Object value) {
        return getSingleOrNull(loadByProperty(type, name, value));
    }

    public <T> Collection<T> loadByProperty(Class<T> type, String name, Object value) {
        return session.loadByProperty(type, Property.with(name, value), DEFAULT_DEPTH);
    }

    public <T> Collection<T> loadByProperty(Class<T> type, String name, Object value, int depth) {
        return session.loadByProperty(type, Property.with(name, value), depth);
    }

    public <T> T save(T object, int depth) {
        session.save(object, depth);
        return object;
    }

    public Iterable<Map<String, Object>> query(String cypher, Map<String, Object> parameters) {
        return session.query(cypher, parameters);
    }

    public <T> Iterable<T> queryForObjects(Class<T> objectType, String cypher, Map<String, Object> parameters) {
        return session.query(objectType, cypher, parameters);
    }

    public <T> T queryForObject(Class<T> objectType, String cypher, Map<String, Object> parameters) {
        return session.queryForObject(objectType, cypher, parameters);
    }

}
