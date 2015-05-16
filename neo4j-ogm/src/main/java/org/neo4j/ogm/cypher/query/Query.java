package org.neo4j.ogm.cypher.query;

import org.neo4j.ogm.cypher.statement.ParameterisedStatement;

import java.util.Map;

/**
 * @author: Vince Bickers
 */
public class Query extends ParameterisedStatement implements PagingAndSorting {

    protected Query(String cypher, Map<String, ?> parameters) {
        super(cypher, parameters);
    }

    protected Query(String cypher, Map<String, ?> parameters, String... resultDataContents) {
        super(cypher, parameters, resultDataContents);
    }

    protected Query(String cypher, Map<String, ?> parameters, boolean includeStats, String... resultDataContents) {
        super(cypher, parameters, includeStats, resultDataContents);
    }

    public Query setPage(Paging page) {
        super.addPaging(page);
        return this;
    }

    public Query orderAscending(String... properties) {
        super.addOrdering(Direction.ASC, properties);
        return this;
    }

    public Query orderDescending(String... properties) {
        super.addOrdering(Direction.DESC, properties);
        return this;
    }

}
