package org.springframework.data.graph.neo4j.finder;

import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.index.impl.lucene.ValueContext;
import org.springframework.data.graph.core.GraphBacked;
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

    protected Index<S> getIndex(String indexName) {
        return graphDatabaseContext.getIndex(clazz,indexName);
    }

    protected T createEntity(S node) {
        return graphDatabaseContext.createEntityFromState(node, clazz);
    }

    /**
     * Index based exact finder.
     *
     * @param indexName or null for default index
     * @param property
     * @param value
     * @return Iterable over Entities with this property and value
     */
    @Override
    public Iterable<T> findAllByPropertyValue(final String indexName, final String property, final Object value) {
        return query(indexName, new Query<S>() {
            public IndexHits<S> query(Index<S> index) {
                return getIndexHits(indexName, property, value);
            }
        });
    }
    /**
     * Index based fulltext / query object finder.
     *
     *
     * @param indexName or null for default index
     * @param key key of the field to query
     *@param query lucene query object or query-string  @return Iterable over Entities with this property and value
     */
    @Override
    public Iterable<T> findAllByQuery(final String indexName, final String key, final Object query) {
        return query(indexName, new Query<S>() {
            public IndexHits<S> query(Index<S> index) {
                return getIndex(indexName).query(key, query);
            }
        });
    }

    interface Query<S extends PropertyContainer> {
        IndexHits<S> query(Index<S> index);
    }
    private Iterable<T> query(String indexName, Query<S> query) {
        try {
            final IndexHits<S> indexHits = query.query(getIndex(indexName));
            if (indexHits == null) return Collections.emptyList();
            return new IterableWrapper<T, S>(indexHits) {
                protected T underlyingObjectToObject(final S result) {
                    return createEntity(result);
                }
            };
        } catch (NotFoundException e) {
            return null;
        }
    }
    @Override
    public Iterable<T> findAllByRange(final String indexName, final String property, final Number from, final Number to) {
        return query(indexName, new Query<S>() {
            public IndexHits<S> query(Index<S> index) {
                return index.query(property, createRangeQuery(property, from, to));
            }
        });
    }

    protected <T extends Number> NumericRangeQuery<T> createRangeQuery(String property, Number from, Number to) {
        if (from instanceof Long) return (NumericRangeQuery<T>) NumericRangeQuery.newLongRange(property, from.longValue(),to.longValue(),true,true);
        if (from instanceof Integer) return (NumericRangeQuery<T>) NumericRangeQuery.newIntRange(property, from.intValue(), to.intValue(), true, true);
        if (from instanceof Double) return (NumericRangeQuery<T>) NumericRangeQuery.newDoubleRange(property, from.doubleValue(), to.doubleValue(), true, true);
        if (from instanceof Float) return (NumericRangeQuery<T>) NumericRangeQuery.newFloatRange(property, from.floatValue(), to.floatValue(), true, true);
        return (NumericRangeQuery<T>) NumericRangeQuery.newIntRange(property, from.intValue(), to.intValue(), true, true);
    }

    protected abstract S getById(long id);
}
