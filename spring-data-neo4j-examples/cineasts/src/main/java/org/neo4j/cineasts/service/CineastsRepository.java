package org.neo4j.cineasts.service;

import org.neo4j.cineasts.domain.*;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * @author mh
 * @since 04.03.11
 */
@Repository
@Transactional
public class CineastsRepository {

    @Autowired private PersonRepository personRepository;
    @Autowired private MovieRepository movieRepository;


    public Movie getMovie(String id) {
        return movieRepository.findById(id);
    }

    public Page<Movie> findMovies(String query, int max) {
        if (query.isEmpty()) return new PageImpl<Movie>(Collections.<Movie>emptyList());
        if (max < 1 || max > 1000) max = 100;

        return movieRepository.findByTitleLike(query, new PageRequest(0, max));
    }

    public Person getPerson(String id) {
        return personRepository.findById(id);
    }

    public Rating rateMovie(Movie movie, User user, int stars, String comment) {
        if (user == null || movie==null) return null;
        return user.rate(movie, stars,comment);
    }

    public Iterable<MovieRecommendation> recommendMovies(User user) {
        return movieRepository.getRecommendations(user);
    }
}
