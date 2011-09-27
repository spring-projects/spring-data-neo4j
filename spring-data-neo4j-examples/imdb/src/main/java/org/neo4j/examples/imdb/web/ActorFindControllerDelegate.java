package org.neo4j.examples.imdb.web;

import org.neo4j.examples.imdb.domain.Actor;
import org.neo4j.examples.imdb.domain.ImdbService;
import org.neo4j.examples.imdb.domain.Movie;
import org.neo4j.examples.imdb.domain.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import java.util.*;

public class ActorFindControllerDelegate implements FindControllerDelegate {
    @Autowired
    private ImdbService imdbService;

    public String getFieldName() {
        return "name";
    }

    @Transactional
    public void getModel(final Object command, final Map<String, Object> model)
            throws ServletException {
        final String name = ((ActorForm) command).getName();
        final Actor actor = imdbService.getActor(name);
        populateModel(model, actor);
    }

    private void populateModel(final Map<String, Object> model,
                               final Actor actor) {
        if (actor == null) {
            model.put("actorName", "No actor found");
            model.put("kevinBaconNumber", "");
            model.put("movieTitles", Collections.emptyList());
        } else {
            model.put("actorName", actor.getName());
            final List<?> baconPathList = imdbService.getBaconPath(actor);
            model.put("kevinBaconNumber", baconPathList.size() / 2);
            final Collection<MovieInfo> movieInfo = new TreeSet<MovieInfo>();
            for (Movie movie : actor.getMovies()) {
                movieInfo.add(new MovieInfo(movie, actor.getRole(movie)));
            }
            model.put("movieInfo", movieInfo);
            final List<String> baconPath = new LinkedList<String>();
            for (Object actorOrMovie : baconPathList) {
                if (actorOrMovie instanceof Actor) {
                    baconPath.add(((Actor) actorOrMovie).getName());
                } else if (actorOrMovie instanceof Movie) {
                    baconPath.add(((Movie) actorOrMovie).getTitle());
                }
            }
            model.put("baconPath", baconPath);
        }
    }

    public static final class MovieInfo implements Comparable<MovieInfo> {
        private String title;
        private String role;

        MovieInfo(final Movie movie, final Role role) {
            setTitle(movie.getTitle());
            if (role == null || role.getName() == null) {
                setRole("(unknown)");
            } else {
                setRole(role.getName());
            }
        }

        public final void setTitle(final String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public final void setRole(final String role) {
            this.role = role;
        }

        public String getRole() {
            return role;
        }

        public int compareTo(final MovieInfo otherMovieInfo) {
            return getTitle().compareTo(otherMovieInfo.getTitle());
        }
    }
}
