package org.springframework.data.graph.neo4j.finder;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.index.impl.lucene.ValueContext;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.util.Collections;

/**
 * Repository like finder for Node and Relationship-Entities. Provides finder methods for direct access, access via {@link org.springframework.data.graph.core.NodeTypeStrategy}
 * and indexing.
 *
 * @param <T> GraphBacked target of this finder, enables the finder methods to return this concrete type
 * @param <S> Type of backing state, either Node or Relationship
 */
public abstract class AbstractFinder<S extends PropertyContainer, T extends GraphBacked<S>> implements Finder<S, T> {
    /**
     * Target graphbacked type
     */
    protected final Class<T> clazz;
    protected final GraphDatabaseContext graphDatabaseContext;

    public AbstractFinder(final GraphDatabaseContext graphDatabaseContext, final Class<T> clazz) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.clazz = clazz;
    }

    /**
     * @return Number of instances of the target type in the graph.
     */
    @Override
    public long count() {
        return graphDatabaseContext.count(clazz);
    }

    /**
     * @return lazy Iterable over all instances of the target type.
     */
    @Override
    public Iterable<T> findAll() {
        return graphDatabaseContext.findAll(clazz);
    }

    /**
     * @param id id
     * @return Entity with the given id or null.
     */
    @Override
    public T findById(final long id) {
        try {
            return createEntity(getById(id));
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Index based single finder.
     *
     * @param indexName or null for default
     * @param property
     * @param value
     * @return Single Entity with this property and value
     */
    @Override
    public T findByPropertyValue(final String indexName, final String property, final Object value) {
        try {
            S result = getIndexHits(indexName, property, value).getSingle();
            if (result == null) return null;
            return createEntity(result);
        } catch (NotFoundException e) {
            return null;
        }

    }

    private IndexHits<S> getIndexHits(String indexName, String property, Object value) {
        if (value instanceof Number) value = ValueContext.numeric((Number) value);
        return getIndex(indexName).get(property, value);
    }

    protected abstract Index<S> getIndex(String indexName);

    protected T createEntity(S node) {
        return graphDatabaseContext.createEntityFromState(node, clazz);
    }

    /**
     * Index based finder.
     *
     * @param indexName or null for default index
     * @param property
     * @param value
     * @return Iterable over Entities with this property and value
     */
    @Override
    public Iterable<T> findAllByPropertyValue(final String indexName, final String property, final Object value) {
        try {
            final IndexHits<S> indexHits = getIndexHits(indexName, property, value);
            if (indexHits == null) return Collections.emptyList();
            return new IterableWrapper<T, S>(indexHits) {
                @Override
                protected T underlyingObjectToObject(final S result) {
                    return createEntity(result);
                }
            };
        } catch (NotFoundException e) {
            return null;
        }
    }

    protected abstract S getById(long id);
}
