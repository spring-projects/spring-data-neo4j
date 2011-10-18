package org.neo4j.examples.imdb.domain;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.kernel.StandardExpander;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

class ImdbServiceImpl implements ImdbService {
    @Autowired
    private Neo4jTemplate template;
    @Autowired
    private ImdbSearchEngine searchEngine;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ActorRepository actorRepository;

    public Actor createActor(final String name) {
        final Actor actor = new Actor().persist();
        actor.setName(name);
        searchEngine.indexActor(actor);
        return actor;
    }

    public Movie createMovie(final String title, final int year) {
        final Movie movie = new Movie().persist();
        movie.setTitle(title);
        movie.setYear(year);
        searchEngine.indexMovie(movie);
        return movie;
    }

    public Actor getActor(final String name) {
        Actor actor = actorRepository.findByPropertyValue("name", name);
        if (actor != null) return actor;
        return searchEngine.searchActor(name);
    }

    public Movie getMovie(final String title) {
        Movie movie = getExactMovie(title);
        if (movie != null) return movie;

        return searchEngine.searchMovie(title);
    }

    public Movie getExactMovie(final String title) {
        return movieRepository.findByPropertyValue("title", title);
    }


    @Transactional
    public void setupReferenceRelationship() {
        Node referenceNode = template.getReferenceNode();
        Actor bacon = actorRepository.findByPropertyValue("name", "Bacon, Kevin");

        if (bacon == null) throw new NoSuchElementException("Unable to find Kevin Bacon actor");

        referenceNode.createRelationshipTo(bacon.getPersistentState(), RelTypes.IMDB);
    }

    public List<?> getBaconPath(final Actor actor) {
        if (actor == null) throw new IllegalArgumentException("Null actor");

        Actor bacon = actorRepository.findByPropertyValue("name", "Bacon, Kevin");

        Path path = GraphAlgoFactory.shortestPath(StandardExpander.DEFAULT.add(RelTypes.ACTS_IN), 10).findSinglePath(bacon.getPersistentState(), actor.getPersistentState());
        if (path==null) return Collections.emptyList();
        return convertNodesToActorsAndMovies(path);
    }

    private List<?> convertNodesToActorsAndMovies(final Path list) {
        final List<Object> actorAndMovieList = new LinkedList<Object>();
        int mod = 0;
        for (Node node : list.nodes()) {
            if (mod++ % 2 == 0) {
                actorAndMovieList.add(template.createEntityFromState(node, Actor.class));
            } else {
                actorAndMovieList.add(template.createEntityFromState(node, Movie.class));
            }
        }
        return actorAndMovieList;
    }
}