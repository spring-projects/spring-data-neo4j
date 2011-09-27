package org.neo4j.examples.imdb.domain;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

// START SNIPPET: ActorClass
@NodeEntity
public class Actor {
    @Indexed
    private String name;

    @RelatedTo(type = "ACTS_IN", elementClass = Movie.class)
    private Set<Movie> movies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Movie> getMovies() {
        return movies;
    }

    public Role getRole(final Movie inMovie) {
        return getRelationshipTo(inMovie, Role.class, RelTypes.ACTS_IN.name());
    }

    @Override
    public String toString() {
        return "Actor '" + this.getName() + "'";
    }

    public void actsIn(Movie movie, final String role) {
        relateTo(movie, Role.class, RelTypes.ACTS_IN.name()).play(role);
    }

    public int getMovieCount() {
        return getMovies().size();
    }
}
// END SNIPPET: ActorClass
