package org.neo4j.cineasts.movieimport;

import org.neo4j.cineasts.domain.*;
import org.neo4j.cineasts.repository.DirectorRepository;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.ActorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MovieDbImportService {

    private static final Logger logger = LoggerFactory.getLogger(MovieDbImportService.class);
    MovieDbJsonMapper movieDbJsonMapper = new MovieDbJsonMapper();

    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private DirectorRepository directorRepository;

    @Autowired
    private MovieDbApiClient client;

    @Autowired
    private MovieDbLocalStorage localStorage;
    @Autowired private Neo4jOperations template;

    @Transactional
    public Map<Integer, String> importMovies(Map<Integer, Integer> ranges) {
        final Map<Integer,String> movies=new LinkedHashMap<Integer, String>();
        for (Map.Entry<Integer, Integer> entry : ranges.entrySet()) {
            for (int id = entry.getKey(); id <= entry.getValue(); id++) {
                String result = importMovieFailsafe(id);
                movies.put(id, result);
            }
        }
        return movies;
    }

    private String importMovieFailsafe(Integer id) {
        try {
            Movie movie = doImportMovie(String.valueOf(id));
            return movie.getTitle();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Transactional
    public Movie importMovie(String movieId) {
        return doImportMovie(movieId);
    }

    private Movie doImportMovie(String movieId) {
        logger.debug("Importing movie " + movieId);

        Movie movie = movieRepository.findById(movieId);
        if (movie == null) { // Not found: Create fresh
            movie = new Movie(movieId,null);
        }

        Map data = loadMovieData(movieId);
        if (data.containsKey("not_found")) throw new RuntimeException("Data for Movie "+movieId+" not found.");
        movieDbJsonMapper.mapToMovie(data, movie);
        movieRepository.save(movie);
        relatePersonsToMovie(movie, data);
        return movie;
    }

    private Map loadMovieData(String movieId) {
        if (localStorage.hasMovie(movieId)) {
            return localStorage.loadMovie(movieId);
        }

        Map data = client.getMovie(movieId);
        localStorage.storeMovie(movieId, data);
        return data;
    }

    private void relatePersonsToMovie(Movie movie, Map data) {
        @SuppressWarnings("unchecked") Collection<Map> cast = (Collection<Map>) data.get("cast");
        for (Map entry : cast) {
            String id = "" + entry.get("id");
            String jobName = (String) entry.get("job");
            Roles job = movieDbJsonMapper.mapToRole(jobName);
            if (job==null) {
                if (logger.isInfoEnabled()) logger.info("Could not add person with job "+jobName+" "+entry);
                continue;
            }
            switch (job) {
                case DIRECTED:
                    final Director director = template.projectTo(doImportPerson(id, new Director(id)), Director.class);
                    director.directed(movie);
                    directorRepository.save(director);
                    break;
                case ACTS_IN:
                    final Actor actor = template.projectTo(doImportPerson(id, new Actor(id)), Actor.class);
                    actor.playedIn(movie, (String) entry.get("character"));
                    actorRepository.save(actor);
                    break;
            }
        }
    }

    @Transactional
    public <T extends Person> T importPerson(String personId, T person) {
        return doImportPerson(personId,person);
    }

    private <T extends Person> T doImportPerson(String personId, T newPerson) {
        logger.debug("Importing person " + personId);
        Person person = template.lookup(Person.class,"id",personId).to(Person.class).singleOrNull();
        if (person!=null) return (T)person;
        Map data = loadPersonData(personId);
        if (data.containsKey("not_found")) throw new RuntimeException("Data for Person "+personId+" not found.");
        movieDbJsonMapper.mapToPerson(data, newPerson);
        return template.save(newPerson);
    }

    private Map loadPersonData(String personId) {
        if (localStorage.hasPerson(personId)) {
            return localStorage.loadPerson(personId);
        }
        Map data = client.getPerson(personId);
        localStorage.storePerson(personId, data);
        return localStorage.loadPerson(personId);
    }
}
