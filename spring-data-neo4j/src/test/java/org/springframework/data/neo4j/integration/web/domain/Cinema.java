package org.springframework.data.neo4j.integration.web.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

public class Cinema {

    private Long id;
    private String name;

    @Relationship(direction = Relationship.INCOMING)
    private Set<User> visited = new HashSet<>();

    public Cinema() {
    }

    public Cinema(String name) {
        this.name = name;
    }

    public void addVisitor(User user) {
        visited.add(user);
    }

    public String getName() {
        return name;
    }
}
