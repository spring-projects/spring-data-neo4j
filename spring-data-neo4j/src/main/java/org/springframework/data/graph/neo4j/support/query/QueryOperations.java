package org.springframework.data.graph.neo4j.support.query;

import java.util.Map;

/**
 * @author mh
 * @since 28.06.11
 */
public interface QueryOperations {
    Iterable<Map<String, Object>> queryForList(String statement);

    <T> Iterable<T> query(String statement, Class<T> type);

    <T> T queryForObject(String statement, Class<T> type);
}
