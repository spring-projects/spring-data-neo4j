package org.springframework.data.graph.neo4j;

import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;

@NodeEntity
public class InvalidReadOnlyOneToNEntity {
    @RelatedTo
    private Iterable<InvalidReadOnlyOneToNEntity> others;
}
