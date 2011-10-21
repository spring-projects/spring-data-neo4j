package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Movie;
import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;

/**
 * @author mh
 * @since 02.04.11
 */
public interface MovieRepository extends GraphRepository<Movie>, NamedIndexRepository<Movie> {

    @Query("START movie=node:Movie(id={_0}) MATCH movie-[rating?:rating]->() RETURN movie, AVG(rating)")
    MovieData getMovieData(String movieId);

    @MapResult
    interface MovieData {
        @ResultColumn("movie")
        Movie getMovie();

        @ResultColumn("avg(rating.stars)")
        Integer getAverageRating();
    }

}


