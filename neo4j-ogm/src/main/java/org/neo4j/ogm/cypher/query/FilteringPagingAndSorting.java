package org.neo4j.ogm.cypher.query;

import org.neo4j.ogm.cypher.Filters;

/**
 * @author: Vince Bickers
 */
public interface FilteringPagingAndSorting {

    public static enum Direction {

        ASC, DESC
    }

    Query setPage(Pagination page);
    Query setFilters(Filters filters);
    Query setSortOrder(SortOrder sortOrder);

    Query orderAscending(String... properties);
    Query orderDescending(String... properties);
}
