/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb.util;

import java.util.Iterator;
import java.util.Map;

import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.IteratorWrapper;


public class QueryResultBuilder<T> implements QueryResult<T> {
    private Iterable<T> result;
    private final ResultConverter defaultConverter;
    private final boolean isClosableIterable;
    private boolean isClosed;

    public QueryResultBuilder(Iterable<T> result) {
        this(result, new DefaultConverter());
    }

    public QueryResultBuilder(Iterable<T> result, final ResultConverter<T,?> defaultConverter) {
        this.result = result;
        this.isClosableIterable = result instanceof IndexHits || result instanceof ClosableIterable;
        this.defaultConverter = defaultConverter;
    }

    public static String replaceParams(String statement, Map<String, Object> params) {
        if (params==null || params.isEmpty()) return statement;
        for (Map.Entry<String, Object> param : params.entrySet()) {
            statement = statement.replaceAll("%"+param.getKey()+"\\b",""+param.getValue());
        }
        return statement;
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
                try {
                    final T value = IteratorUtil.single(QueryResultBuilder.this.result);
                    return resultConverter.convert(value, type);
                } finally {
                    closeIfNeeded();
                }
            }

            @Override
            public R singleOrNull() {
                try {
                    final T value = IteratorUtil.singleOrNull(QueryResultBuilder.this.result);
                    return resultConverter.convert(value, type);
                } finally {
                    closeIfNeeded();
                }
            }

            @Override
            public void handle(Handler<R> handler) {
                try {
                    for (T value : result) {
                        handler.handle(resultConverter.convert(value, type));
                    }
                } finally {
                    closeIfNeeded();
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
}
