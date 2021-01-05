/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.RatingRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Vince Bickers
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@RunWith(SpringRunner.class)
@Transactional
public class DerivedRelationshipEntityQueryTests {

	@Autowired private GraphDatabaseService graphDatabaseService;

	@Autowired private UserRepository userRepository;

	@Autowired private RatingRepository ratingRepository;

	@Before
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldFindREFromCustomQuery() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");

		userRepository.save(critic);

		Rating rating = ratingRepository.findRatingByUserAndTempMovie(critic.getId(), film.getId());
		assertNotNull(rating);
		assertNotNull("The loaded rating shouldn't be null", rating);
		assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), rating.getStars());
		assertEquals("The rated film wasn't saved correctly", film.getName(), rating.getMovie().getName());
		assertEquals("The critic wasn't saved correctly", critic.getId(), rating.getUser().getId());
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithSingleProperty() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStars(2);
		assertNotNull(ratings);
		Rating loadedRating = ratings.get(0);
		assertNotNull("The loaded rating shouldn't be null", loadedRating);
		assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
		assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithMultiplePropertiesAnded() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 1000);
		assertNotNull(ratings);
		Rating loadedRating = ratings.get(0);
		assertNotNull("The loaded rating shouldn't be null", loadedRating);
		assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
		assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 2000);
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithMultiplePropertiesOred() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStarsOrRatingTimestamp(5, 1000);
		assertNotNull(ratings);
		Rating loadedRating = ratings.get(0);
		assertNotNull("The loaded rating shouldn't be null", loadedRating);
		assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
		assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(5, 2000);
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithMultiplePropertiesDifferentComparisonOperatorsAnded() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStarsAndRatingTimestampLessThan(2, 2000);
		assertNotNull(ratings);
		Rating loadedRating = ratings.get(0);
		assertNotNull("The loaded rating shouldn't be null", loadedRating);
		assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
		assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 3000);
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithMultiplePropertiesDifferentComparisonOperatorsOred() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStarsOrRatingTimestampGreaterThan(5, 500);
		assertNotNull(ratings);
		Rating loadedRating = ratings.get(0);
		assertNotNull("The loaded rating shouldn't be null", loadedRating);
		assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
		assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(5, 2000);
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithNestedStartNodeProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Speed'}) CREATE (m2:Movie {name:'The Matrix'}) CREATE (m:Movie {name:'Chocolat'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserName("Michal");
		assertEquals(2, ratings.size());
		Collections.sort(ratings);
		assertEquals("Speed", ratings.get(0).getMovie().getName());
		assertEquals("The Matrix", ratings.get(1).getMovie().getName());
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithNestedEndNodeProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Finding Dory'}) CREATE (m2:Movie {name:'Captain America'}) CREATE (m:Movie {name:'X-Men'})"
						+ " CREATE (u:User {name:'Vince'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByMovieName("Captain America");
		assertEquals(1, ratings.size());
		assertEquals("Vince", ratings.get(0).getUser().getName());
		assertEquals("Captain America", ratings.get(0).getMovie().getName());
		assertEquals(4, ratings.get(0).getStars());

		ratings = ratingRepository.findByMovieName("X-Men");
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithBothStartEndNestedProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Independence Day: Resurgence'}) CREATE (m2:Movie {name:'The Conjuring 2'}) CREATE (m:Movie {name:'The BFG'})"
						+ " CREATE (u:User {name:'Daniela'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserNameAndMovieName("Daniela", "Independence Day: Resurgence");
		assertEquals(1, ratings.size());
		assertEquals("Daniela", ratings.get(0).getUser().getName());
		assertEquals("Independence Day: Resurgence", ratings.get(0).getMovie().getName());
		assertEquals(3, ratings.get(0).getStars());

		ratings = ratingRepository.findByUserNameAndMovieName("Daniela", "The BFG");
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithBaseAndNestedStartNodePropertyAnded() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'The Shallows'}) CREATE (m2:Movie {name:'Central Intelligence'}) CREATE (m:Movie {name:'Now you see me'})"
						+ " CREATE (u:User {name:'Luanne'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserNameAndStars("Luanne", 3);
		assertEquals(1, ratings.size());
		assertEquals("Luanne", ratings.get(0).getUser().getName());
		assertEquals("The Shallows", ratings.get(0).getMovie().getName());
		assertEquals(3, ratings.get(0).getStars());

		ratings = ratingRepository.findByUserNameAndStars("Luanne", 1);
		assertEquals(0, ratings.size());
	}

	@Test(expected = UnsupportedOperationException.class) // DATAGRAPH-662
	public void shouldFindRelEntitiesWithBaseAndNestedStartNodePropertyOred() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Swiss Army Man'}) CREATE (m2:Movie {name:'Me Before You'}) CREATE (m:Movie {name:'X-Men Apocalypse'})"
						+ " CREATE (u:User {name:'Mark'}) CREATE (u2:User {name:'Adam'})  "
						+ " CREATE (u)-[:RATED {stars:2}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)"
						+ " CREATE (u2)-[:RATED {stars:3}]->(m)");

		List<Rating> ratings = ratingRepository.findByStarsOrUserName(3, "Mark");
		assertEquals(3, ratings.size());
		Collections.sort(ratings);
		assertEquals("Swiss Army Man", ratings.get(0).getMovie().getName());
		assertEquals("X-Men Apocalypse", ratings.get(1).getMovie().getName());
		assertEquals("Me Before You", ratings.get(2).getMovie().getName());

		ratings = ratingRepository.findByStarsOrUserName(0, "Vince");
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithBaseAndNestedEndNodeProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Our Kind of Traitor'}) CREATE (m2:Movie {name:'Teenage Mutant Ninja Turtles'}) CREATE (m:Movie {name:'Zootopia'})"
						+ " CREATE (u:User {name:'Chris'}) CREATE (u2:User {name:'Katerina'}) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)"
						+ " CREATE (u2)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByStarsAndMovieName(4, "Teenage Mutant Ninja Turtles");
		assertEquals(2, ratings.size());
		Collections.sort(ratings);
		assertEquals("Chris", ratings.get(0).getUser().getName());
		assertEquals("Katerina", ratings.get(1).getUser().getName());

		ratings = ratingRepository.findByStarsAndMovieName(5, "Teenage Mutant Ninja Turtles");
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithBaseAndBothStartEndNestedProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'The Jungle Book'}) CREATE (m2:Movie {name:'The Angry Birds Movie'}) CREATE (m:Movie {name:'Alice Through The Looking Glass'})"
						+ " CREATE (u:User {name:'Alessandro'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserNameAndMovieNameAndStars("Alessandro", "The Jungle Book", 3);
		assertEquals(1, ratings.size());
		assertEquals("Alessandro", ratings.get(0).getUser().getName());
		assertEquals("The Jungle Book", ratings.get(0).getMovie().getName());
		assertEquals(3, ratings.get(0).getStars());

		ratings = ratingRepository.findByUserNameAndMovieNameAndStars("Colin", "Speed", 0);
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithTwoStartNodeNestedProperties() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Batman v Superman'}) CREATE (m2:Movie {name:'Genius'}) CREATE (m:Movie {name:'Home'})"
						+ " CREATE (u:User {name:'David', middleName:'M'}) CREATE (u2:User {name:'Martin', middleName:'M'}) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)"
						+ " CREATE (u2)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserNameAndUserMiddleName("David", "M");
		assertEquals(2, ratings.size());
		Collections.sort(ratings);
		assertEquals("David", ratings.get(0).getUser().getName());
		assertEquals("Batman v Superman", ratings.get(0).getMovie().getName());
		assertEquals("David", ratings.get(1).getUser().getName());
		assertEquals("Genius", ratings.get(1).getMovie().getName());

		ratings = ratingRepository.findByUserNameAndUserMiddleName("David", "V");
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-813
	public void shouldDeleteAndReturnDeletedIds() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Long> ratingIds = ratingRepository.deleteByStarsOrRatingTimestampGreaterThan(2, 500);
		assertEquals(filmRating.getId(), ratingIds.get(0));
		assertEquals(1, ratingIds.size());

		List<Rating> ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 2000);
		assertEquals(0, ratings.size());
	}

	@Test // DATAGRAPH-813
	public void shouldCountByStars() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		long starredRatings = ratingRepository.countByStars(2);
		assertEquals(1L, starredRatings);

	}

	@Test // DATAGRAPH-813
	public void shouldRemoveByUserNameAndReturnCountOfDeletedObjects() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		long countRemovedObjects = ratingRepository.removeByUserName("Gary");
		assertEquals(1L, countRemovedObjects);

	}

	@Test // DATAGRAPH-813
	public void shouldCountNothingWithNonMatchingFilter() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		long countRemovedObjects = ratingRepository.removeByUserName("Bill");
		assertEquals(0L, countRemovedObjects);
	}

	@Test // DATAGRAPH-813
	public void shouldDeleteNothingWithNonMatchingFilter() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Long> deletedIds = ratingRepository.deleteByStarsOrRatingTimestampGreaterThan(3, 2000);
		assertEquals(0L, deletedIds.size());
	}
}
