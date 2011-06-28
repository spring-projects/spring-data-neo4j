package org.springframework.data.graph.neo4j.conversion;

/**
* @author mh
* @since 28.06.11
*/
public interface ConvertedResult<R> extends Iterable<R> {
    R single();
    void handle(Handler<R> handler);
}
