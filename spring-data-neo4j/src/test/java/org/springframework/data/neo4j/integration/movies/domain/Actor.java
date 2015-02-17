package org.springframework.data.neo4j.integration.movies.domain;


import org.neo4j.ogm.annotation.GraphId;

public class Actor{

    @GraphId
    Long nodeId;
    String id;
    String name;

    public Actor() {
    }

    public Actor(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
