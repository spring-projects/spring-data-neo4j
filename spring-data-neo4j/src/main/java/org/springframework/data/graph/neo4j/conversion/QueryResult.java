package org.springframework.data.graph.neo4j.conversion;

/**
* @author mh
* @since 28.06.11
*/
public interface QueryResult<T> extends Iterable<T> {
    <R> ConvertedResult<R> to(Class<R> type);
    <R,U extends T> ConvertedResult<R> to(Class<R> type, ResultConverter<U, R> resultConverter);
}
