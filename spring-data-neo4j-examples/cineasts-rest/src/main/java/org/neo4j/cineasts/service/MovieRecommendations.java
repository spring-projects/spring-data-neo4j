package org.neo4j.cineasts.service;

import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.User;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import java.util.HashMap;
import java.util.Map;

import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

/**
* @author mh
* @since 04.04.11
*/
class MovieRecommendations implements Evaluator {
    private final Map<Long,int[]> ratings=new HashMap<Long, int[]>();
    private final int ratingDistance;
    private MovieRepository movieRepository;

    public MovieRecommendations(MovieRepository movieRepository, int ratingDistance) {
        this.movieRepository = movieRepository;
        this.ratingDistance = ratingDistance;
    }

    public Map<Movie, ?> getRecommendationsFor(User user) {
        TraversalDescription traversal= Traversal.description().breadthFirst()
                .uniqueness(Uniqueness.NODE_PATH)
                .relationships(withName(User.FRIEND))
                .relationships(withName(User.RATED), OUTGOING)
                .evaluator(this);
        return averageRecommendations(user, traversal);
    }

    private Map<Movie, Integer> averageRecommendations(User user, TraversalDescription traversal) { // todo sort, limit
        Map<Movie,Integer> result=new HashMap<Movie, Integer>();
        for (Movie movie : movieRepository.findAllByTraversal(user, traversal)) {
            final int[] rating = ratings.get(movie.getNodeId());
            result.put(movie, rating[1]==0 ? 0 : rating[0]/rating[1]);
        }
        return result;
    }

    @Override
    public Evaluation evaluate(Path path) {
        final int distance = path.length() - 1;
        if (distance > ratingDistance) return Evaluation.EXCLUDE_AND_PRUNE;
        Relationship rated = path.lastRelationship();
        if (rated != null && rated.getType().name().equals(User.RATED)) {
            if (distance == 0) return Evaluation.EXCLUDE_AND_PRUNE; // my rated movies
            updateRating(rated, distance);
            return Evaluation.INCLUDE_AND_PRUNE;
        }
        System.out.println();
        return Evaluation.EXCLUDE_AND_CONTINUE;
    }

    private void updateRating(Relationship rated, int distance) {
        final long movieId = rated.getEndNode().getId();
        int[] rating = obtainRating(movieId);

        int weight = ratingDistance - distance;
        final Integer stars = (Integer) rated.getProperty("stars", 0);
        rating[0] += weight * stars;
        rating[1] += weight;
    }

    private int[] obtainRating(long movieId) {
        if (!ratings.containsKey(movieId)) {
            ratings.put(movieId,new int[2]);
        }
        return ratings.get(movieId);
    }
}
