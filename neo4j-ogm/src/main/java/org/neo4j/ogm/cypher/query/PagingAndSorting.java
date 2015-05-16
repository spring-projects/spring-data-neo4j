package org.neo4j.ogm.cypher.query;

/**
 * @author: Vince Bickers
 */
public interface PagingAndSorting {

    public static enum Direction {

        ASC, DESC
    }

    Query setPage(Paging page);
    Query orderAscending(String... properties);
    Query orderDescending(String... properties);
}
