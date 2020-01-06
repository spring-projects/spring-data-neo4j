/*
 * Copyright 2011-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(rating).isNotNull();
		assertThat(rating).as("The loaded rating shouldn't be null").isNotNull();
		assertThat(rating.getStars())
				.as("The relationship properties weren't saved correctly")
				.isEqualTo(filmRating.getStars());
		assertThat(rating.getMovie().getName())
				.as("The rated film wasn't saved correctly").isEqualTo(film.getName());
		assertThat(rating.getUser().getId()).as("The critic wasn't saved correctly")
				.isEqualTo(critic.getId());
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithSingleProperty() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStars(2);
		assertThat(ratings).isNotNull();
		Rating loadedRating = ratings.get(0);
		assertThat(loadedRating).as("The loaded rating shouldn't be null").isNotNull();
		assertThat(loadedRating.getStars())
				.as("The relationship properties weren't saved correctly")
				.isEqualTo(filmRating.getStars());
		assertThat(loadedRating.getMovie().getName())
				.as("The rated film wasn't saved correctly").isEqualTo(film.getName());
		assertThat(loadedRating.getUser().getId()).as("The critic wasn't saved correctly")
				.isEqualTo(critic.getId());
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithMultiplePropertiesAnded() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 1000);
		assertThat(ratings).isNotNull();
		Rating loadedRating = ratings.get(0);
		assertThat(loadedRating).as("The loaded rating shouldn't be null").isNotNull();
		assertThat(loadedRating.getStars())
				.as("The relationship properties weren't saved correctly")
				.isEqualTo(filmRating.getStars());
		assertThat(loadedRating.getMovie().getName())
				.as("The rated film wasn't saved correctly").isEqualTo(film.getName());
		assertThat(loadedRating.getUser().getId()).as("The critic wasn't saved correctly")
				.isEqualTo(critic.getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 2000);
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithMultiplePropertiesOred() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStarsOrRatingTimestamp(5, 1000);
		assertThat(ratings).isNotNull();
		Rating loadedRating = ratings.get(0);
		assertThat(loadedRating).as("The loaded rating shouldn't be null").isNotNull();
		assertThat(loadedRating.getStars())
				.as("The relationship properties weren't saved correctly")
				.isEqualTo(filmRating.getStars());
		assertThat(loadedRating.getMovie().getName())
				.as("The rated film wasn't saved correctly").isEqualTo(film.getName());
		assertThat(loadedRating.getUser().getId()).as("The critic wasn't saved correctly")
				.isEqualTo(critic.getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(5, 2000);
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithMultiplePropertiesDifferentComparisonOperatorsAnded() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStarsAndRatingTimestampLessThan(2, 2000);
		assertThat(ratings).isNotNull();
		Rating loadedRating = ratings.get(0);
		assertThat(loadedRating).as("The loaded rating shouldn't be null").isNotNull();
		assertThat(loadedRating.getStars())
				.as("The relationship properties weren't saved correctly")
				.isEqualTo(filmRating.getStars());
		assertThat(loadedRating.getMovie().getName())
				.as("The rated film wasn't saved correctly").isEqualTo(film.getName());
		assertThat(loadedRating.getUser().getId()).as("The critic wasn't saved correctly")
				.isEqualTo(critic.getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 3000);
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-629
	public void shouldFindREWithMultiplePropertiesDifferentComparisonOperatorsOred() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStarsOrRatingTimestampGreaterThan(5, 500);
		assertThat(ratings).isNotNull();
		Rating loadedRating = ratings.get(0);
		assertThat(loadedRating).as("The loaded rating shouldn't be null").isNotNull();
		assertThat(loadedRating.getStars())
				.as("The relationship properties weren't saved correctly")
				.isEqualTo(filmRating.getStars());
		assertThat(loadedRating.getMovie().getName())
				.as("The rated film wasn't saved correctly").isEqualTo(film.getName());
		assertThat(loadedRating.getUser().getId()).as("The critic wasn't saved correctly")
				.isEqualTo(critic.getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(5, 2000);
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithNestedStartNodeProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Speed'}) CREATE (m2:Movie {name:'The Matrix'}) CREATE (m:Movie {name:'Chocolat'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserName("Michal");
		assertThat(ratings.size()).isEqualTo(2);
		Collections.sort(ratings);
		assertThat(ratings.get(0).getMovie().getName()).isEqualTo("Speed");
		assertThat(ratings.get(1).getMovie().getName()).isEqualTo("The Matrix");
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithNestedEndNodeProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Finding Dory'}) CREATE (m2:Movie {name:'Captain America'}) CREATE (m:Movie {name:'X-Men'})"
						+ " CREATE (u:User {name:'Vince'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByMovieName("Captain America");
		assertThat(ratings.size()).isEqualTo(1);
		assertThat(ratings.get(0).getUser().getName()).isEqualTo("Vince");
		assertThat(ratings.get(0).getMovie().getName()).isEqualTo("Captain America");
		assertThat(ratings.get(0).getStars()).isEqualTo(4);

		ratings = ratingRepository.findByMovieName("X-Men");
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithBothStartEndNestedProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Independence Day: Resurgence'}) CREATE (m2:Movie {name:'The Conjuring 2'}) CREATE (m:Movie {name:'The BFG'})"
						+ " CREATE (u:User {name:'Daniela'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserNameAndMovieName("Daniela", "Independence Day: Resurgence");
		assertThat(ratings.size()).isEqualTo(1);
		assertThat(ratings.get(0).getUser().getName()).isEqualTo("Daniela");
		assertThat(ratings.get(0).getMovie().getName())
				.isEqualTo("Independence Day: Resurgence");
		assertThat(ratings.get(0).getStars()).isEqualTo(3);

		ratings = ratingRepository.findByUserNameAndMovieName("Daniela", "The BFG");
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithBaseAndNestedStartNodePropertyAnded() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'The Shallows'}) CREATE (m2:Movie {name:'Central Intelligence'}) CREATE (m:Movie {name:'Now you see me'})"
						+ " CREATE (u:User {name:'Luanne'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserNameAndStars("Luanne", 3);
		assertThat(ratings.size()).isEqualTo(1);
		assertThat(ratings.get(0).getUser().getName()).isEqualTo("Luanne");
		assertThat(ratings.get(0).getMovie().getName()).isEqualTo("The Shallows");
		assertThat(ratings.get(0).getStars()).isEqualTo(3);

		ratings = ratingRepository.findByUserNameAndStars("Luanne", 1);
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test(expected = UnsupportedOperationException.class) // DATAGRAPH-662
	public void shouldFindRelEntitiesWithBaseAndNestedStartNodePropertyOred() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Swiss Army Man'}) CREATE (m2:Movie {name:'Me Before You'}) CREATE (m:Movie {name:'X-Men Apocalypse'})"
						+ " CREATE (u:User {name:'Mark'}) CREATE (u2:User {name:'Adam'})  "
						+ " CREATE (u)-[:RATED {stars:2}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)"
						+ " CREATE (u2)-[:RATED {stars:3}]->(m)");

		List<Rating> ratings = ratingRepository.findByStarsOrUserName(3, "Mark");
		assertThat(ratings.size()).isEqualTo(3);
		Collections.sort(ratings);
		assertThat(ratings.get(0).getMovie().getName()).isEqualTo("Swiss Army Man");
		assertThat(ratings.get(1).getMovie().getName()).isEqualTo("X-Men Apocalypse");
		assertThat(ratings.get(2).getMovie().getName()).isEqualTo("Me Before You");

		ratings = ratingRepository.findByStarsOrUserName(0, "Vince");
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithBaseAndNestedEndNodeProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Our Kind of Traitor'}) CREATE (m2:Movie {name:'Teenage Mutant Ninja Turtles'}) CREATE (m:Movie {name:'Zootopia'})"
						+ " CREATE (u:User {name:'Chris'}) CREATE (u2:User {name:'Katerina'}) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)"
						+ " CREATE (u2)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByStarsAndMovieName(4, "Teenage Mutant Ninja Turtles");
		assertThat(ratings.size()).isEqualTo(2);
		Collections.sort(ratings);
		assertThat(ratings.get(0).getUser().getName()).isEqualTo("Chris");
		assertThat(ratings.get(1).getUser().getName()).isEqualTo("Katerina");

		ratings = ratingRepository.findByStarsAndMovieName(5, "Teenage Mutant Ninja Turtles");
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithBaseAndBothStartEndNestedProperty() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'The Jungle Book'}) CREATE (m2:Movie {name:'The Angry Birds Movie'}) CREATE (m:Movie {name:'Alice Through The Looking Glass'})"
						+ " CREATE (u:User {name:'Alessandro'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserNameAndMovieNameAndStars("Alessandro", "The Jungle Book", 3);
		assertThat(ratings.size()).isEqualTo(1);
		assertThat(ratings.get(0).getUser().getName()).isEqualTo("Alessandro");
		assertThat(ratings.get(0).getMovie().getName()).isEqualTo("The Jungle Book");
		assertThat(ratings.get(0).getStars()).isEqualTo(3);

		ratings = ratingRepository.findByUserNameAndMovieNameAndStars("Colin", "Speed", 0);
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-632
	public void shouldFindRelEntitiesWithTwoStartNodeNestedProperties() {
		graphDatabaseService.execute(
				"CREATE (m1:Movie {name:'Batman v Superman'}) CREATE (m2:Movie {name:'Genius'}) CREATE (m:Movie {name:'Home'})"
						+ " CREATE (u:User {name:'David', middleName:'M'}) CREATE (u2:User {name:'Martin', middleName:'M'}) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)"
						+ " CREATE (u2)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserNameAndUserMiddleName("David", "M");
		assertThat(ratings.size()).isEqualTo(2);
		Collections.sort(ratings);
		assertThat(ratings.get(0).getUser().getName()).isEqualTo("David");
		assertThat(ratings.get(0).getMovie().getName()).isEqualTo("Batman v Superman");
		assertThat(ratings.get(1).getUser().getName()).isEqualTo("David");
		assertThat(ratings.get(1).getMovie().getName()).isEqualTo("Genius");

		ratings = ratingRepository.findByUserNameAndUserMiddleName("David", "V");
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-813
	public void shouldDeleteAndReturnDeletedIds() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Long> ratingIds = ratingRepository.deleteByStarsOrRatingTimestampGreaterThan(2, 500);
		assertThat(ratingIds.get(0)).isEqualTo(filmRating.getId());
		assertThat(ratingIds.size()).isEqualTo(1);

		List<Rating> ratings = ratingRepository.findByStarsAndRatingTimestamp(2, 2000);
		assertThat(ratings.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-813
	public void shouldCountByStars() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		long starredRatings = ratingRepository.countByStars(2);
		assertThat(starredRatings).isEqualTo(1L);

	}

	@Test // DATAGRAPH-813
	public void shouldRemoveByUserNameAndReturnCountOfDeletedObjects() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		long countRemovedObjects = ratingRepository.removeByUserName("Gary");
		assertThat(countRemovedObjects).isEqualTo(1L);

	}

	@Test // DATAGRAPH-813
	public void shouldCountNothingWithNonMatchingFilter() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		long countRemovedObjects = ratingRepository.removeByUserName("Bill");
		assertThat(countRemovedObjects).isEqualTo(0L);
	}

	@Test // DATAGRAPH-813
	public void shouldDeleteNothingWithNonMatchingFilter() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");
		filmRating.setRatingTimestamp(1000);

		userRepository.save(critic);

		List<Long> deletedIds = ratingRepository.deleteByStarsOrRatingTimestampGreaterThan(3, 2000);
		assertThat(deletedIds.size()).isEqualTo(0L);
	}
}
