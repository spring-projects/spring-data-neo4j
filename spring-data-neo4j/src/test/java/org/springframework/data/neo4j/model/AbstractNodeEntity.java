package org.springframework.data.neo4j.model;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 *
 */
@NodeEntity
public abstract class AbstractNodeEntity {

   @GraphId
    public Long id;

    public String name;

    public AbstractNodeEntity() {
        this(null);
    }

    public AbstractNodeEntity(String name) {
       this.name = name;
    }
}