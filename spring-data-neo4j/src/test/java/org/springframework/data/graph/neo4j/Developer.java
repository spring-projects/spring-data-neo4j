package org.springframework.data.graph.neo4j;

import org.springframework.data.graph.annotation.NodeEntity;

/**
 * @author mh
 * @since 18.02.11
 */
@NodeEntity(autoAttach = false)
public class Developer {
    String name;

    Person spouse;

    public Developer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Person getSpouse() {
        return spouse;
    }

    public void setSpouse(Person spouse) {
        this.spouse = spouse;
    }
}
