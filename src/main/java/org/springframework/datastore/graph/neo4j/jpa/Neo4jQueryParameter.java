package org.springframework.datastore.graph.neo4j.jpa;

import javax.persistence.Parameter;
/**
 * @author Michael Hunger
 * @since 11.09.2010
 */
public class Neo4jQueryParameter<T> implements Parameter<T> {
    private final Class<T> type;
    private final String name;
    private final Integer position;

    public Neo4jQueryParameter(Class<T> type, String name, Integer position) {
        this.type = type;
        this.name = name;
        this.position = position;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    @Override
    public Class<T> getParameterType() {
        return type;
    }

    public static Parameter<?> param(String name) { return new Neo4jQueryParameter<Object>(null,name,null);}
    public static Parameter<?> param(Integer position) { return new Neo4jQueryParameter<Object>(null,null,position);}
    public static <T> Parameter<T> param(Class<T> type, String name, Integer position) { return new Neo4jQueryParameter<T>(type,name,position);}
}
