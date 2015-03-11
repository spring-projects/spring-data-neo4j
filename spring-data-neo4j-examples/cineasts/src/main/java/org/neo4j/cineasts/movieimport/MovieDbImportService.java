package org.neo4j.cineasts.movieimport;

import org.neo4j.cineasts.domain.Actor;
import org.neo4j.cineasts.domain.Director;
import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.Person;
import org.neo4j.cineasts.domain.Roles;
import org.neo4j.cineasts.repository.ActorRepository;
import org.neo4j.cineasts.repository.DirectorRepository;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.PersonRepository;
import org.neo4j.helpers.collection.IteratorUtil;
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
    private MovieRepository movieRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private DirectorRepository directorRepository;
    @Autowired
    private PersonRepository personRepository;

    private MovieDbApiClient client = new MovieDbApiClient("70c7465a780b1d65c0f3d5bd394c5b80");

    private String baseImageUrl;

    private MovieDbLocalStorage localStorage = new MovieDbLocalStorage("data/json");

    public void importImageConfig() {
        Map data = client.getImageConfig();
        baseImageUrl = ((Map)data.get("images")).get("base_url") + "w185";

    }
     @Transactional
    public Map<Integer, String> importMovies(Map<Integer, Integer> ranges) {
        final Map<Integer, String> movies = new LinkedHashMap<Integer, String>();
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
            movie = new Movie(movieId, null);
        }

        Map data = loadMovieData(movieId);
        if (data.containsKey("not_found")) {
            throw new RuntimeException("Data for Movie " + movieId + " not found.");
        }
        movieDbJsonMapper.mapToMovie(data, movie, baseImageUrl);
        movieRepository.save(movie);
        relatePersonsToMovie(movie, (Map) data.get("credits"));
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
        //Relate Crew
        @SuppressWarnings("unchecked") Collection<Map> crew = (Collection<Map>) data.get("crew");
        for (Map entry : crew) {
            String id = "" + entry.get("id");
            String jobName = (String) entry.get("job");
            Roles job = movieDbJsonMapper.mapToRole(jobName);
            if (job == null) {
                if (logger.isInfoEnabled()) {
                    logger.info("Could not add person with job " + jobName + " " + entry);
                }
                continue;
            }
            if (Roles.DIRECTED.equals(job)) {
                try {
                    final Director director = doImportPerson(id, new Director(id));
                    director.directed(movie);
                    directorRepository.save(director);
                }
                catch (ClassCastException cce) {  //TODO hack for now
                    logger.debug("Person " + id + " and job " + jobName + " already exists as an actor");
                }
            }
        }

        //Relate Cast
        @SuppressWarnings("unchecked") Collection<Map> cast = (Collection<Map>) data.get("cast");
        for (Map entry : cast) {
            String id = "" + entry.get("id");
            try {
                final Actor actor = doImportPerson(id, new Actor(id));
                actor.playedIn(movie, (String) entry.get("character"));
                actorRepository.save(actor);
            }
            catch(ClassCastException cce) {
                logger.debug("Person " + id + " and job Actor already exists as a director");
            }
        }
    }

     @Transactional
    public <T extends Person> T importPerson(String personId, T person) {
        return doImportPerson(personId, person);
    }

    private <T extends Person> T doImportPerson(String personId, T newPerson) {
        logger.debug("Importing person " + personId);
        Person person = IteratorUtil.singleOrNull(personRepository.findByProperty("id", personId));
        if (person != null) {
            return (T) person;
        }
        Map data = loadPersonData(personId);
        if (data.containsKey("not_found")) {
            throw new RuntimeException("Data for Person " + personId + " not found.");
        }
        movieDbJsonMapper.mapToPerson(data, newPerson);
        return personRepository.save(newPerson);
    }

    /*private Actor doImportActor(String personId, Actor newPerson) {
        logger.debug("Importing actor " + personId);
        Actor actor = IteratorUtil.singleOrNull(actorRepository.findByProperty("id", personId));
        if (actor != null) {
            return actor;
        }
        Map data = loadPersonData(personId);
        if (data.containsKey("not_found")) {
            throw new RuntimeException("Data for Person " + personId + " not found.");
        }
        movieDbJsonMapper.mapToPerson(data, newPerson);
        return actorRepository.save(newPerson);
    }

    private Director doImportDirector(String personId, Director newPerson) {
        logger.debug("Importing director " + personId);
        Director director = IteratorUtil.singleOrNull(directorRepository.findByProperty("id", personId));
        if (director != null) {
            return director;
        }
        Map data = loadPersonData(personId);
        if (data.containsKey("not_found")) {
            throw new RuntimeException("Data for Person " + personId + " not found.");
        }
        movieDbJsonMapper.mapToPerson(data, newPerson);
        return directorRepository.save(newPerson);
    }
*/
    private Map loadPersonData(String personId) {
        if (localStorage.hasPerson(personId)) {
            return localStorage.loadPerson(personId);
        }
        Map data = client.getPerson(personId);
        localStorage.storePerson(personId, data);
        return localStorage.loadPerson(personId);
    }
}
