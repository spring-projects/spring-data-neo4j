package org.springframework.datastore.graph.neo4j.partial;

import org.springframework.datastore.graph.annotations.EndNode;
import org.springframework.datastore.graph.annotations.RelationshipEntity;
import org.springframework.datastore.graph.annotations.StartNode;

/**
 * @author Michael Hunger
 * @since 27.09.2010
 */
@RelationshipEntity
public class Recommendation {
    @StartNode
    private User user;
    @EndNode
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
