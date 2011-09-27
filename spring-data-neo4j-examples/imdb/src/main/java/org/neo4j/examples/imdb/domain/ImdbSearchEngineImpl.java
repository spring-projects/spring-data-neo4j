package org.neo4j.examples.imdb.domain;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * example-implementation of an in-graph index using splitted names and titles and Lookup - Entities
 * representing them to navigate to the "indexed" entities via their relationships.
 * Lookup[word] - name.part -*> Actor | Lookup[word] - title.part -*> Movie
 */
public class ImdbSearchEngineImpl implements ImdbSearchEngine {

    @Autowired
    private LookupRepository lookupRepository;

    public void indexActor(Actor actor) {
        for (Lookup lookup : obtainLookups(actor.getName())) {
            lookup.addActor(actor);
        }
    }

    public void indexMovie(Movie movie) {
        String name = movie.getTitle();
        for (Lookup lookup : obtainLookups(name)) {
            lookup.addMovie(movie);
        }
    }

    public Actor searchActor(String name) {
        Set<Actor> result = null;
        for (Lookup lookup : findLookups(name)) {
            result = merge(result, lookup.getActors());
        }
        return firstOrMax(result, new Comparator<Actor>() {
            public int compare(Actor actor1, Actor actor2) {
                return actor1.getMovieCount() - actor2.getMovieCount();
            }
        });
    }


    public Movie searchMovie(String title) {
        Set<Movie> result = null;
        for (Lookup lookup : findLookups(title)) {
            result = merge(result, lookup.getMovies());
        }
        return firstOrMax(result, new Comparator<Movie>(){
            public int compare(Movie movie1, Movie movie2) {
                return movie1.getActorsCount() - movie2.getActorsCount();
            }
        });
    }

    private List<Lookup> obtainLookups(String name) {
        List<Lookup> result = new ArrayList<Lookup>();
        for (String part : splitSearchString(name)) {
            Lookup lookup = findLookup(part);
            if (lookup == null) {
                result.add((Lookup) new Lookup(part).persist());
            } else {
                result.add(lookup);
            }
        }
        return result;
    }

    private <T> T firstOrMax(Set<T> result, Comparator<T> comparator) {
        if (result == null || result.isEmpty()) return null;
        return Collections.max(result, comparator);
    }

    private <T> Set<T> merge(Set<T> result, Collection<T> values) {
        if (result == null) {
            return new HashSet<T>(values);
        }

        result.retainAll(values);
        return result;
    }

    private List<Lookup> findLookups(String name) {
        List<Lookup> result = new ArrayList<Lookup>();
        for (String part : splitSearchString(name)) {
            Lookup lookup = findLookup(part);
            if (lookup != null) result.add(lookup);
        }
        return result;
    }

    private Lookup findLookup(String part) {
        return lookupRepository.findByPropertyValue("word", part);
    }

    private String[] splitSearchString(final String value) {
        return value.toLowerCase(Locale.ENGLISH).split("[^\\w]+");
    }
}
