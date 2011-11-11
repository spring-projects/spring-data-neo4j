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

package org.springframework.data.neo4j.conversion;

import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.springframework.data.neo4j.mapping.MappingPolicy;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author mh
 * @since 28.06.11
 */
public class QueryResultBuilder<T> implements Result<T> {
    private Iterable<T> result;
    private final ResultConverter defaultConverter;
    private final boolean isClosableIterable;
    private boolean isClosed;
    private MappingPolicy mappingPolicy;

    @SuppressWarnings("unchecked")
    public QueryResultBuilder(Iterable<T> result) {
        this(result, new DefaultConverter());
    }

    public QueryResultBuilder(Iterable<T> result, final ResultConverter<T,?> defaultConverter) {
        this.result = result;
        this.isClosableIterable = result instanceof IndexHits || result instanceof ClosableIterable;
        this.defaultConverter = defaultConverter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> EndResult<R> to(Class<R> type) {
        return this.to(type, defaultConverter);
    }

    @Override
    public T single() {
        try {
            return IteratorUtil.single(result);
        } finally {
            closeIfNeeded();
        }
    }
    @Override
    public T singleOrNull() {
        try {
            return IteratorUtil.singleOrNull(result);
        } finally {
            closeIfNeeded();
        }
    }

    @Override
    public <R> EndResult<R> to(final Class<R> type, final ResultConverter<T, R> resultConverter) {
        return new EndResult<R>() {
            @Override
            public R single() {
                try {
                    final T value = IteratorUtil.single(result);
                    return convert(value);
                } finally {
                    closeIfNeeded();
                }
            }
            @Override
            public R singleOrNull() {
                try {
                    final T value = IteratorUtil.singleOrNull(result);
                    return convert(value);
                } finally {
                    closeIfNeeded();
                }
            }

            private R convert(T value) {
                return resultConverter.convert(value, type, mappingPolicy);
            }

            @Override
            public void handle(Handler<R> handler) {
                try {
                    for (T value : result) {
                        handler.handle(convert(value));
                    }
                } finally {
                    closeIfNeeded();
                }
            }

            @Override
            public Iterator<R> iterator() {
                return new IteratorWrapper<R, T>(result.iterator()) {
                    protected R underlyingObjectToObject(T value) {
                        return convert(value);
                    }
                };
            }

            @Override
            public <C extends Iterable<R>> C as(Class<C> container) {
                return ContainerConverter.toContainer(container, this);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Iterable<T>> C as(Class<C> container) {
        return ContainerConverter.toContainer(container, this);
    }

    @Override
    public void handle(Handler<T> handler) {
        try {
            for (T value : result) {
                handler.handle(value);
            }
        } finally {
            closeIfNeeded();
        }
    }


    private void closeIfNeeded() {
        if (isClosableIterable && !isClosed) {
            if (result instanceof IndexHits) {
               ((IndexHits) result).close();
            } else if (result instanceof ClosableIterable) {
               ((ClosableIterable) result).close();
            }
            isClosed=true;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return result.iterator();
    }

    public Result<T> with(MappingPolicy mappingPolicy) {
        this.mappingPolicy = mappingPolicy;
        return this;
    }

    public static <T> QueryResultBuilder<T> from(Iterable<T> values) {
        return new QueryResultBuilder<T>(values);
    }
    public static <T> QueryResultBuilder<T> from(T...values) {
        return from(Arrays.<T>asList(values));
    }
}
