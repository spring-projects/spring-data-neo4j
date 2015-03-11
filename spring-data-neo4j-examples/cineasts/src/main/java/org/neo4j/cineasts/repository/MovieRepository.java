package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Movie;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.List;

/**
 * @author mh
 * @since 02.04.11
 */
public interface MovieRepository extends GraphRepository<Movie> {
    Movie findById(String id);

    // Page<Movie> findByTitleLike(String title, Pageable page);

    @Query("MATCH (movie:Movie) WHERE movie.title =~ '(?i).*{0}.*' RETURN movie")
    Iterable<Movie> findByTitleLike(String title);

    @Query( "match (user:User {login: {0}})-[r:RATED]->(movie)<-[r2:RATED]-(other)-[r3:RATED]->(otherMovie) " +
                    " where r.stars >= 3 and r2.stars >= r.stars and r3.stars >= r.stars " +
                    " with otherMovie, avg(r3.stars) as rating, count(*) as cnt" +
                    " order by rating desc, cnt desc" +
                    " return otherMovie limit 10" )
    List<Movie> getRecommendations(String login);
}
