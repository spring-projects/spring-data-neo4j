package org.neo4j.cineasts.service;

import org.neo4j.cineasts.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mh
 * @since 04.03.11
 */
@Repository
@Transactional
public class CineastsRepository {

    @Autowired private MovieRepository movieRepository;


}
