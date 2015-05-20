package org.neo4j.ogm.cypher.query;

import org.neo4j.ogm.cypher.Filters;

/**
 * @author: Vince Bickers
 */
public interface FilteringPagingAndSorting {

    Query setPagination(Pagination page);
    Query setFilters(Filters filters);
    Query setSortOrder(SortOrder sortOrder);

}
