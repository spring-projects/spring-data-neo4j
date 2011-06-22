/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.repository;

import org.apache.lucene.search.NumericRangeQuery;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Repository like finder for Node and Relationship-Entities. Provides finder methods for direct access, access via {@link org.springframework.data.graph.core.TypeRepresentationStrategy}
 * and indexing.
 *
 * @param <T> GraphBacked target of this finder, enables the finder methods to return this concrete type
 * @param <S> Type of backing state, either Node or Relationship
 */
@org.springframework.stereotype.Repository
public abstract class AbstractGraphRepository<S extends PropertyContainer, T extends GraphBacked<S>> implements GraphRepository<T>, NamedIndexRepository<T> {
    public static final ClosableIterable EMPTY_CLOSABLE_ITERABLE = new ClosableIterable() {
        @Override
        public void close() {
        }

        @Override
        public Iterator<?> iterator() {
            return Collections.emptyList().iterator();
        }
    };
    /**
     * Target graphbacked type
     */
    protected final Class<T> clazz;
    protected final GraphDatabaseContext graphDatabaseContext;

    public AbstractGraphRepository(final GraphDatabaseContext graphDatabaseContext, final Class<T> clazz) {
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
    public ClosableIterable<T> findAll() {
        return graphDatabaseContext.findAll(clazz);
    }

    /**
     *
     * @param id id
     * @return Entity with the given id or null.
     */
    @Override
    public T findOne(final Long id) {
        try {
            return createEntity(getById(id));
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Index based single finder, uses the default index name for this type (short class name).
     *
     * @param property
     * @param value
     * @return Single Entity with this property and value
     */
    @Override
    public T findByPropertyValue(final String property, final Object value) {
        return findByPropertyValue(null,property,value);
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
        if (value instanceof Number) {
            Number number = (Number) value;
            return getIndex(indexName).query(property, createInclusiveRangeQuery(property, number,number));
        }
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
    public ClosableIterable<T> findAllByPropertyValue(final String indexName, final String property, final Object value) {
        return query(indexName, new Query<S>() {
            public IndexHits<S> query(Index<S> index) {
                return getIndexHits(indexName, property, value);
            }
        });
    }
    /**
     * Index based exact finder, uses the default index name for this type (short class name).
     * @param property
     * @param value
     * @return Iterable over Entities with this property and value
     */
    @Override
    public ClosableIterable<T> findAllByPropertyValue(final String property, final Object value) {
        return findAllByPropertyValue(null, property, value);
    }

    /**
     * Index based fulltext / query object finder, uses the default index name for this type (short class name).
     *
     * @param key key of the field to query
     *@param query lucene query object or query-string  @return Iterable over Entities with this property and value
     */
    @Override
    public ClosableIterable<T> findAllByQuery(final String key, final Object query) {
        return findAllByQuery(null, key,query);
    }
    /**
     * Index based fulltext / query object finder.
     *
     * @param indexName or null for default index
     * @param key key of the field to query
     *@param query lucene query object or query-string  @return Iterable over Entities with this property and value
     */
    @Override
    public ClosableIterable<T> findAllByQuery(final String indexName, final String key, final Object query) {
        return query(indexName, new Query<S>() {
            public IndexHits<S> query(Index<S> index) {
                return getIndex(indexName).query(key, query);
            }
        });
    }

    interface Query<S extends PropertyContainer> {
        IndexHits<S> query(Index<S> index);
    }
    private ClosableIterable<T> query(String indexName, Query<S> query) {
        try {
            final IndexHits<S> indexHits = query.query(getIndex(indexName));
            if (indexHits == null) return emptyClosableIterable();
            return new IndexHitsWrapper(indexHits);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked"})
    private ClosableIterable<T> emptyClosableIterable() {
        return EMPTY_CLOSABLE_ITERABLE;
    }

    @Override
    public ClosableIterable<T> findAllByRange(final String property, final Number from, final Number to) {
        return findAllByRange(null,property,from,to);
    }
    @Override
    public ClosableIterable<T> findAllByRange(final String indexName, final String property, final Number from, final Number to) {
        return query(indexName, new Query<S>() {
            public IndexHits<S> query(Index<S> index) {
                return index.query(property, createInclusiveRangeQuery(property, from, to));
            }
        });
    }

    protected <T extends Number> NumericRangeQuery<T> createInclusiveRangeQuery(String property, Number from, Number to) {
        if (from instanceof Long) return (NumericRangeQuery<T>) NumericRangeQuery.newLongRange(property, from.longValue(),to.longValue(),true,true);
        if (from instanceof Integer) return (NumericRangeQuery<T>) NumericRangeQuery.newIntRange(property, from.intValue(), to.intValue(), true, true);
        if (from instanceof Double) return (NumericRangeQuery<T>) NumericRangeQuery.newDoubleRange(property, from.doubleValue(), to.doubleValue(), true, true);
        if (from instanceof Float) return (NumericRangeQuery<T>) NumericRangeQuery.newFloatRange(property, from.floatValue(), to.floatValue(), true, true);
        return (NumericRangeQuery<T>) NumericRangeQuery.newIntRange(property, from.intValue(), to.intValue(), true, true);
    }

    protected abstract S getById(long id);

    @Override
    public boolean exists(Long id) {
        try {
            return getById(id)!=null;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public void delete(T entity) {
       entity.remove();
    }

    @Override
    public void delete(Long id) {
        delete(findOne(id));
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        for (T entity : entities) {
            entity.remove();
        }
    }

    @Override
    public void deleteAll() {
        delete(findAll());
    }

    @Override
    public ClosableIterable<T> findAll(Sort sort) {
        return findAll(); // todo
    }

    @Override
    public Page<T> findAll(final Pageable pageable) {
        final int count = pageable.getOffset()+pageable.getPageSize();
        int counter=count;
        ClosableIterable<T> all = findAll(pageable.getSort());
        List<T> result=new ArrayList<T>(count);
        for (T t : all) {
            if (counter == 0) break;
            result.add(t);
            counter--;
        }
        all.close();
        return new PageImpl<T>(result, pageable,count - counter);
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
}
