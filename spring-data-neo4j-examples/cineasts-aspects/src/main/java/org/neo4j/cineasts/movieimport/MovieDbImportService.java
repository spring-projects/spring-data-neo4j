package org.neo4j.cineasts.movieimport;

import org.neo4j.cineasts.domain.*;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.PersonRepository;
import org.neo4j.cineasts.service.CineastsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    MovieRepository movieRepository;
    @Autowired
    PersonRepository personRepository;

    @Autowired
    MovieDbApiClient client;

    @Autowired
    MovieDbLocalStorage localStorage;

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
        movie.persist();
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
        Collection<Map> cast = (Collection<Map>) data.get("cast");
        for (Map entry : cast) {
            String id = "" + entry.get("id");
            String jobName = (String) entry.get("job");
            Roles job = movieDbJsonMapper.mapToRole(jobName);
            if (job==null) {
                if (logger.isInfoEnabled()) logger.info("Could not add person with job "+jobName+" "+entry);
                continue;
            }
            Person person = doImportPerson(id);
            switch (job) {
                case DIRECTED:
                    person.directed(movie);
                    break;
                case ACTS_IN:
                    person.playedIn(movie, (String) entry.get("character"));
                    break;
            }
        }
    }

    @Transactional
    public Person importPerson(String personId) {
        return doImportPerson(personId);
    }

    private Person doImportPerson(String personId) {
        logger.debug("Importing person " + personId);
        Person person = personRepository.findById(personId);
        if (person!=null) return person;
        Map data = loadPersonData(personId);
        if (data.containsKey("not_found")) throw new RuntimeException("Data for Person "+personId+" not found.");
        Person newPerson=new Person(personId,null);
        movieDbJsonMapper.mapToPerson(data, newPerson);
        return newPerson.persist();
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
