package org.neo4j.rest.graphdb.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 25.09.14
 */
public interface CypherResult {
    Collection<String> getColumns();

    Iterable<List<Object>> getData();

    Map asMap();
}
