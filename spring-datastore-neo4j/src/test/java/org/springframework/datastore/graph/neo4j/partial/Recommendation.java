package org.springframework.datastore.graph.neo4j.partial;

import org.springframework.datastore.graph.annotations.RelationshipEndNode;
import org.springframework.datastore.graph.annotations.RelationshipEntity;
import org.springframework.datastore.graph.annotations.RelationshipStartNode;

/**
 * @author Michael Hunger
 * @since 27.09.2010
 */
@RelationshipEntity
public class Recommendation {
    @RelationshipStartNode
    private User user;
    @RelationshipEndNode
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
