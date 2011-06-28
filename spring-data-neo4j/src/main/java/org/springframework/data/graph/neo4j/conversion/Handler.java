package org.springframework.data.graph.neo4j.conversion;

/**
 * @author mh
 * @since 28.06.11
 */
public interface Handler<R> {
    void handle(R value);
}

