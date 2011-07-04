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

package org.springframework.data.graph.neo4j.conversion;

import org.neo4j.helpers.collection.IteratorWrapper;

import java.util.Iterator;

/**
 * @author mh
 * @since 28.06.11
 */
public class QueryResultBuilder<T> implements QueryResult<T> {
    private Iterable<T> result;
    private final ResultConverter defaultConverter;

    public QueryResultBuilder(Iterable<T> result) {
        this(result, new DefaultConverter());
    }

    public QueryResultBuilder(Iterable<T> result, final ResultConverter<T,?> defaultConverter) {
        this.result = result;
        this.defaultConverter = defaultConverter;
    }

    @Override
    public <R> ConvertedResult<R> to(Class<R> type) {
        return this.to(type, defaultConverter);
    }

    @Override
    public <R> ConvertedResult<R> to(final Class<R> type, final ResultConverter<T, R> resultConverter) {
        return new ConvertedResult<R>() {
            @Override
            public R single() {
                final Iterator<T> it = result.iterator();
                if (!it.hasNext()) throw new IllegalStateException("Expected at least one result, got none.");
                final T value = it.next();
                if (it.hasNext()) throw new IllegalStateException("Expected at least one result, got more than one.");
                return resultConverter.convert(value, type);
            }

            @Override
            public void handle(Handler<R> handler) {
                for (T value : result) {
                    handler.handle(resultConverter.convert(value, type));
                }
            }

            @Override
            public Iterator<R> iterator() {
                return new IteratorWrapper<R, T>(result.iterator()) {
                    protected R underlyingObjectToObject(T value) {
                        return resultConverter.convert(value, type);
                    }
                };
            }
        };
    }

    @Override
    public Iterator<T> iterator() {
        return result.iterator();
    }
}
