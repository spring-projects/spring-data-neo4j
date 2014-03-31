package org.springframework.data.neo4j.repository;

import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.NoSuchIndexException;
import org.springframework.data.neo4j.support.index.NullReadableIndex;

/**
* @author mh
* @since 01.02.14
*/
@Deprecated
public class LegacyIndexSearcher<S extends PropertyContainer,T> {
    private final Neo4jTemplate template;
    private final Class<T> clazz;

    LegacyIndexSearcher(Neo4jTemplate template, Class<T> clazz) {
        this.template = template;
        this.clazz = clazz;
    }
    public <T> Result<T> geoQuery(String indexName, String geoQuery, Object params) {
        final IndexHits<S> indexHits = getIndex(indexName,null).query(geoQuery, params);
        Iterable<T> wrapper = (Iterable<T>) new IndexHitsWrapper(indexHits);
        return template.convert(wrapper);
    }

    private ReadableIndex<S> getIndex(String indexName, String property) {
        try {
            if (indexName!=null) {
                return template.getIndex(indexName,clazz);
            }
            return template.getIndex(clazz,property);
        } catch(NoSuchIndexException nsie) {
            return new NullReadableIndex<S>(nsie.getIndex(),template.getGraphDatabaseService());
        }
    }

    private T createEntity(S node) {
        return template.createEntityFromState(node, clazz, template.getMappingPolicy(clazz));
    }

    private class IndexHitsWrapper extends IterableWrapper<T, S> implements ClosableIterable<T> {
        private final IndexHits<S> indexHits;

        public IndexHitsWrapper(IndexHits<S> indexHits) {
            super(indexHits);
            this.indexHits = indexHits;
        }

        @SuppressWarnings({"unchecked"})
        protected T underlyingObjectToObject(final S result) {
            return createEntity(result);
        }

        @Override
        public void close() {
            this.indexHits.close();
        }
    }

    private IndexHits<S> getIndexHits(String indexName, String propertyName, Object value) {
        final Neo4jPersistentProperty property = template.getPersistentProperty(clazz, propertyName);
        if (value instanceof Number && (property==null || property.getIndexInfo().isNumeric())) {
            Number number = (Number) value;
            return getIndex(indexName, propertyName).query(propertyName, createInclusiveRangeQuery(propertyName, number,number));
        }
        return getIndex(indexName, propertyName).get(propertyName, value);
    }

    private ClosableIterable<T> query(String indexName, AbstractGraphRepository.Query<S> query) {
        try {
            final IndexHits<S> indexHits = query.query(getIndex(indexName, null));
            if (indexHits == null) return emptyClosableIterable();
            return new IndexHitsWrapper(indexHits);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked"})
    private ClosableIterable<T> emptyClosableIterable() {
        return AbstractGraphRepository.EMPTY_CLOSABLE_ITERABLE;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Number> NumericRangeQuery<T> createInclusiveRangeQuery(String property, Number from, Number to) {
        if (from instanceof Long) return (NumericRangeQuery<T>) NumericRangeQuery.newLongRange(property, from.longValue(),to.longValue(),true,true);
        if (from instanceof Integer) return (NumericRangeQuery<T>) NumericRangeQuery.newIntRange(property, from.intValue(), to.intValue(), true, true);
        if (from instanceof Double) return (NumericRangeQuery<T>) NumericRangeQuery.newDoubleRange(property, from.doubleValue(), to.doubleValue(), true, true);
        if (from instanceof Float) return (NumericRangeQuery<T>) NumericRangeQuery.newFloatRange(property, from.floatValue(), to.floatValue(), true, true);
        return (NumericRangeQuery<T>) NumericRangeQuery.newIntRange(property, from.intValue(), to.intValue(), true, true);
    }

    public Result<T> findAllByRange(String indexName, final String property, final Number from, final Number to) {
        return queryResult(indexName, new AbstractGraphRepository.Query<S>() {
            public IndexHits<S> query(ReadableIndex<S> index) {
                return index.query(property, createInclusiveRangeQuery(property, from, to));
            }
        });
    }

    public Result<T> findAllByQuery(final String indexName, final String property, final Object query) {
        return queryResult(indexName, new AbstractGraphRepository.Query<S>() {
            public IndexHits<S> query(ReadableIndex<S> index) {
                return getIndex(indexName, property).query(property, query);
            }
        });
    }

    public T findByPropertyValue(String indexName, String property, Object value) {
        try {
            S result = getIndexHits(indexName, property, value).getSingle();
            if (result == null) return null;
            return createEntity(result);
        } catch (NotFoundException e) {
            return null;
        }
    }


    public Result<T> findAllByPropertyValue(final String indexName, final String property, final Object value) {
        return queryResult(indexName, new AbstractGraphRepository.Query<S>() {
            public IndexHits<S> query(ReadableIndex<S> index) {
                return getIndexHits(indexName, property, value);
            }
        });
    }

    private Result<T> queryResult(String indexName, AbstractGraphRepository.Query<S> query) {
        try {
            final IndexHits<S> indexHits = query.query(getIndex(indexName, null));
            return template.convert(indexHits).to(clazz);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
