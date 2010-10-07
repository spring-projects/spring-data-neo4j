package org.springframework.datastore.graph.neo4j.partial;

import org.springframework.datastore.graph.api.GraphRelationship;
import org.springframework.datastore.graph.api.GraphRelationshipEndNode;
import org.springframework.datastore.graph.api.GraphRelationshipStartNode;

/**
 * @author Michael Hunger
 * @since 27.09.2010
 */
@GraphRelationship
public class Recommendation {
    @GraphRelationshipStartNode
    private User user;
    @GraphRelationshipEndNode
    private Restaurant restaurant;

    private int stars;
    private String comment;


    public Recommendation() {
    }

    public void rate(int stars, String comment) {
        this.stars = stars;
        this.comment = comment;
    }
}
