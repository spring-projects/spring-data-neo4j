package org.neo4j.examples.imdb.web;

import org.neo4j.examples.imdb.domain.Actor;
import org.neo4j.examples.imdb.domain.ImdbService;
import org.neo4j.examples.imdb.domain.Movie;
import org.neo4j.examples.imdb.domain.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;

public class MovieFindControllerDelegate implements FindControllerDelegate {
    @Autowired
    private ImdbService imdbService;

    public String getFieldName() {
        return "title";
    }

    @Transactional
    public void getModel(final Object command, final Map<String, Object> model)
            throws ServletException {
        final String title = ((MovieForm) command).getTitle();
        final Movie movie = imdbService.getMovie(title);
        populateModel(model, movie);
    }

    private void populateModel(final Map<String, Object> model,
                               final Movie movie) {
        if (movie == null) {
            model.put("movieTitle", "No movie found");
            model.put("actorNames", Collections.emptyList());
        } else {
            model.put("movieTitle", movie.getTitle());
            final Collection<ActorInfo> actorInfo = new TreeSet<ActorInfo>();
            for (Actor actor : movie.getActors()) {
                actorInfo.add(new ActorInfo(actor, actor.getRole(movie)));
            }
            model.put("actorInfo", actorInfo);
        }
    }

    public static final class ActorInfo implements Comparable<ActorInfo> {
        private String name;
        private String role;

        public ActorInfo(final Actor actor, final Role role) {
            setName(actor.getName());
            if (role == null || role.getName() == null) {
                setRole("(unknown)");
            } else {
                setRole(role.getName());
            }
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setRole(final String role) {
            this.role = role;
        }

        public String getRole() {
            return role;
        }

        public int compareTo(ActorInfo otherActorInfo) {
            return getName().compareTo(otherActorInfo.getName());
        }
    }
}
