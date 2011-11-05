package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.MovieRecommendation;
import org.neo4j.cineasts.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;

import java.util.List;

/**
 * @author mh
 * @since 02.04.11
 */
public interface MovieRepository extends GraphRepository<Movie>, NamedIndexRepository<Movie> {
    Movie findById(String id);

    Page<Movie> findByTitleLike(String title, Pageable page);

    @Query("start user=node({_0}) " +
            " match user-[r:RATED]->movie<-[r2:RATED]-other-[r3:RATED]->otherMovie " +
            " where r.stars >= 3 and r2.stars >= 3 and r3.stars >= 3 " +
            " return otherMovie, avg(r3.stars) " +
            " order by avg(r3.stars) desc, count(*) desc" +
            " limit 10")
    List<MovieRecommendation> getRecommendations(User user);

}
