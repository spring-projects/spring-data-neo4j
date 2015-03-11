package org.neo4j.cineasts.domain;

import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * @author mh
 * @since 04.11.11
 */
@QueryResult
public class MovieRecommendation {

    Movie movie;
    int rating;
}
