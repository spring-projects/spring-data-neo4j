package org.springframework.data.neo4j.template;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Data operations interface, implemented by {@link Neo4jTemplate}, that provides the API for using
 * the persistence framework in a more direct way as an alternative to the repositories.
 */
public interface Neo4jOperations {

    // TODO add Spring event handling

    <T> T load(Class<T> type, Long id);

    <T> T load(Class<T> type, Long id, int depth);

    <T> Collection<T> loadAll(Class<T> type);

    <T> Collection<T> loadAll(Class<T> type, int depth);

    /**
     * Reloads all of the entities in the given {@link Collection} to the specified depth.  Of course, this will
     * only work for persistent objects (i.e., those with a non-null <code>@GraphId</code> field).
     *
     * @param objects The objects to re-hydrate
     * @param depth The depth to which the objects should be hydrated
     * @return A new {@link Collection} of entities matching those in the given collection hydrated to the given depth
     */
    <T> Collection<T> loadAll(Collection<T> objects, int depth);

    <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue);

    <T> Collection<T> loadAllByProperty(Class<T> type, String propertyName, Object propertyValue);

    <T> T save(T entity);

    /**
     * Removes the given node or relationship entity from the graph.  The entity is first removed
     * from all indexes and then deleted.
     *
     * @param entity The entity to delete
     */
    void delete(Object entity);

    Iterable<Map<String, Object>> query(String cypherQuery, Map<String, ?> params);

    <T> Iterable<T> queryForObjects(Class<T> objectType, String cypherQuery, Map<String, ?> parameters);

    <T> T queryForObject(Class<T> objectType, String cypherQuery, Map<String, ?> parameters);

    /**
     * Issue a single Cypher update operation (such as a <tt>CREATE</tt>, <tt>MERGE</tt> or <tt>DELETE</tt> statement).
     *
     * @param cypherQuery The Cypher query to execute
     */
    void execute(String cypherQuery);

    /**
     * Provides the instance count for the given <em>node</em> entity type.  This method is also provided by the
     * corresponding repository.
     *
     * @param entityClass The {@link Class} representing the type of node entity to count
     * @return The number of entities in the database of the given type
     */
    long count(Class<?> entityClass);

}
