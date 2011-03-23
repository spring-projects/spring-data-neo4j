package org.springframework.data.graph.neo4j;

import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;

import java.util.Collection;

@NodeEntity
public class InvalidOneToNEntity {
    @RelatedTo
    private Collection<InvalidOneToNEntity> others;
}
