package org.springframework.data.graph.neo4j.conversion;

import org.neo4j.helpers.collection.IteratorWrapper;

import java.util.Iterator;

/**
 * @author mh
 * @since 28.06.11
 */
public class QueryResultBuilder<T> implements QueryResult<T> {
    private Iterable<T> result;

    public QueryResultBuilder(Iterable<T> result) {
        this.result = result;
    }

    @Override
    public <R> ConvertedResult<R> to(Class<R> type) {
        return this.to(type, new DefaultConverter());
    }

    @Override
    public <R, U extends T> ConvertedResult<R> to(final Class<R> type, final ResultConverter<U, R> resultConverter) {
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
                return new IteratorWrapper<R, U>(result.iterator()) {
                    protected R underlyingObjectToObject(U value) {
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
