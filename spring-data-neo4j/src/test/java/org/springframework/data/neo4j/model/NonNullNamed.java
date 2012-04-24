package org.springframework.data.neo4j.model;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

import javax.validation.constraints.NotNull;

@NodeEntity
public class NonNullNamed {
    @GraphId
	private Long graphId;

    @NotNull
	private String name;
}