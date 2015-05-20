package org.neo4j.ogm.cypher.query;

/**
 * @author: Vince Bickers
 */
public class Pagination {

    private final Integer index;
    private final Integer size;

    public Pagination(Integer index, Integer size) {
        this.index = index;
        this.size = size;
    }

    public String toString() {
        return " SKIP " + (index * size) + " LIMIT " + size;
    }
}
