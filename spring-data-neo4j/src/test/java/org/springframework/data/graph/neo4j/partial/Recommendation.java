package org.springframework.data.graph.neo4j.partial;

import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;

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
