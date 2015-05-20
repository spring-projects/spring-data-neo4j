package org.neo4j.ogm.cypher.query;

import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.statement.ParameterisedStatement;

import java.util.Map;

/**
 * @author: Vince Bickers
 */
public class Query extends ParameterisedStatement implements FilteringPagingAndSorting {

    protected Query(String cypher, Map<String, ?> parameters) {
        super(cypher, parameters);
    }

    protected Query(String cypher, Map<String, ?> parameters, String... resultDataContents) {
        super(cypher, parameters, resultDataContents);
    }

    protected Query(String cypher, Map<String, ?> parameters, boolean includeStats, String... resultDataContents) {
        super(cypher, parameters, includeStats, resultDataContents);
    }

    public Query setPagination(Pagination page) {
        super.addPaging(page);
        return this;
    }

    public Query setFilters(Filters filters) {
        super.addFilters(filters);
        return this;
    }

    public Query setSortOrder(SortOrder sortOrder) {
        super.addSortOrder(sortOrder);
        return this;
    }
}
