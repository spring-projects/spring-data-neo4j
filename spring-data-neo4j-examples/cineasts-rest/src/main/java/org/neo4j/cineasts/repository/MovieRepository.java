package org.neo4j.cineasts.repository;

import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;

import java.util.List;

public interface MovieRepository extends GraphRepository<Movie>, NamedIndexRepository<Movie> {

    Movie findById( String id );

    Page<Movie> findByTitleLike( String title, Pageable page );

    @Query( "start user=node({0}) " +
                    " match user-[r:RATED]->movie<-[r2:RATED]-other-[r3:RATED]->otherMovie " +
                    " where r.stars >= 3 and r2.stars >= r.stars and r3.stars >= r.stars " +
                    " return otherMovie, avg(r3.stars) as rating, count(*) as cnt" +
                    " order by rating desc, cnt desc" +
                    " limit 10" )
    List<MovieRecommendation> getRecommendations( User user );

    @MapResult
    interface MovieRecommendation {
        @ResultColumn( "otherMovie" )
        Movie getMovie();

        @ResultColumn( "rating" )
        int getRating();
    }

    @Query( "START movie=node:Movie(id={0}) MATCH movie-[rating?:rating]->() RETURN movie, AVG(rating.stars)" )
    MovieData getMovieData( String movieId );

    @MapResult
    interface MovieData {
        @ResultColumn( "movie" )
        Movie getMovie();

        @ResultColumn( "avg(rating.stars)" )
        Integer getAverageRating();
    }
}