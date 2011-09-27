package org.neo4j.cineasts.domain;

import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;

/**
 * @author mh
 * @since 04.03.11
 */
@RelationshipEntity
public class Rating {
    private static final int MAX_STARS = 5;
    private static final int MIN_STARS = 0;

    @StartNode
    User user;
    @EndNode Movie movie;

    int stars;
    String comment;

    public User getUser() {
        return user;
    }

    public Movie getMovie() {
        return movie;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Rating rate(int stars, String comment) {
        if (stars>= MIN_STARS && stars <= MAX_STARS) this.stars=stars;
        if (comment!=null && !comment.isEmpty()) this.comment = comment;
        return this;
    }
}
