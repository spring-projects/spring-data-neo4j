package org.springframework.data.graph.neo4j.support.relationship;

import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;
import org.springframework.data.graph.core.NodeBacked;

/**
 * @author mh
 * @since 26.02.11
 */
@RelationshipEntity
public class DefaultRelationshipBacked {
    @StartNode
    NodeBacked startNode;

    @EndNode
    NodeBacked endNode;

}
