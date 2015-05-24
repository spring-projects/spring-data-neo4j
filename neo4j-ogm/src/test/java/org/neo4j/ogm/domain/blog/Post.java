package org.neo4j.ogm.domain.blog;

import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

/**
 * @author: Vince Bickers
 */
public class Post {

    private Long id;
    private String title;

    public Post() {
    }

    public Post(String title) {
        this.title = title;
    }

    private Post next;

    @Transient
    private Post previous;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Post getNext() {
        return next;
    }

    @Relationship(type="NEXT", direction = Relationship.OUTGOING)
    public void setNext(Post next) {
        System.out.println("linking " + this + "->" + next);
        this.next = next;
        if (next != null) {
            next.previous = this;
        }
    }

    public Post getPrevious() {
        return previous;
    }

    public void setPrevious(Post previous) {
        this.previous = previous;
    }

    public String toString() {
        return title + "(" + id + ")";
    }
}
