/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.RatingRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DerivedRelationshipEntityQueryTest {

	@ClassRule
	public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule(7879);

	private static Session session;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CinemaRepository cinemaRepository;

	@Autowired
	private RatingRepository ratingRepository;

	@Before
	public void init() throws IOException {
		session = new SessionFactory("org.springframework.data.neo4j.examples.movies.domain").openSession(neo4jRule.url());
	}

	@After
	public void clearDatabase() {
		neo4jRule.clearDatabase();
	}

	private void executeUpdate(String cypher) {
		new ExecutionEngine(neo4jRule.getGraphDatabaseService()).execute(cypher);
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
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
		assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
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
		assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(2,2000);
		assertEquals(0,ratings.size());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
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
		assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(5,2000);
		assertEquals(0,ratings.size());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
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
		assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(2,3000);
		assertEquals(0,ratings.size());
	}


	/**
	 * @see DATAGRAPH-629
	 */
	@Test
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
		assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());

		ratings = ratingRepository.findByStarsAndRatingTimestamp(5, 2000);
		assertEquals(0,ratings.size());
	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindRelEntitiesWithNestedStartNodeProperty() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");

		List<Rating> ratings = ratingRepository.findByUserName("Michal");
		assertEquals(2, ratings.size());
		Collections.sort(ratings);
		assertEquals("Speed", ratings.get(0).getMovie().getTitle());
		assertEquals("The Matrix", ratings.get(1).getMovie().getTitle());
	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindRelEntitiesWithNestedEndNodeProperty() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");


		List<Rating> ratings = ratingRepository.findByMovieTitle("The Matrix");
		assertEquals(1, ratings.size());
		assertEquals("Michal", ratings.get(0).getUser().getName());
		assertEquals("The Matrix", ratings.get(0).getMovie().getTitle());
		assertEquals(4, ratings.get(0).getStars());


		ratings = ratingRepository.findByMovieTitle("Chocolat");
		assertEquals(0, ratings.size());
	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindRelEntitiesWithBothStartEndNestedProperty() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");
		List<Rating> ratings = ratingRepository.findByUserNameAndMovieTitle("Michal", "Speed");
		assertEquals(1, ratings.size());
		assertEquals("Michal", ratings.get(0).getUser().getName());
		assertEquals("Speed", ratings.get(0).getMovie().getTitle());
		assertEquals(3, ratings.get(0).getStars());

		ratings = ratingRepository.findByUserNameAndMovieTitle("Michal", "Chocolat");
		assertEquals(0, ratings.size());
	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindRelEntitiesWithBaseAndNestedStartNodePropertyAnded() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");
		List<Rating> ratings = ratingRepository.findByUserNameAndStars("Michal", 3);
		assertEquals(1, ratings.size());
		assertEquals("Michal", ratings.get(0).getUser().getName());
		assertEquals("Speed", ratings.get(0).getMovie().getTitle());
		assertEquals(3, ratings.get(0).getStars());

		ratings = ratingRepository.findByUserNameAndStars("Michal", 1);
		assertEquals(0, ratings.size());

	}

	/**
	 * @see DATAGRAPH-662
	 * //TODO FIXME
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void shouldFindRelEntitiesWithBaseAndNestedStartNodePropertyOred() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u2:User {name:'Vince'})  " +
				" CREATE (u)-[:RATED {stars:2}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)" +
				" CREATE (u2)-[:RATED {stars:3}]->(m)");
		List<Rating> ratings = ratingRepository.findByStarsOrUserName(3, "Michal");
		assertEquals(3, ratings.size());
		Collections.sort(ratings);
		assertEquals("Speed", ratings.get(0).getMovie().getTitle());
		assertEquals("Chocolat", ratings.get(1).getMovie().getTitle());
		assertEquals("The Matrix", ratings.get(2).getMovie().getTitle());


		ratings = ratingRepository.findByStarsOrUserName(0, "Vince");
		assertEquals(0, ratings.size());

	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindRelEntitiesWithBaseAndNestedEndNodeProperty() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u2:User {name:'Vince'}) " +
				" CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)" +
				" CREATE (u2)-[:RATED {stars:4}]->(m2)");
		List<Rating> ratings = ratingRepository.findByStarsAndMovieTitle(4,"The Matrix");
		assertEquals(2, ratings.size());
		Collections.sort(ratings);
		assertEquals("Michal", ratings.get(0).getUser().getName());
		assertEquals("Vince", ratings.get(1).getUser().getName());

		ratings = ratingRepository.findByStarsAndMovieTitle(5, "The Matrix");
		assertEquals(0, ratings.size());
	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindRelEntitiesWithBaseAndBothStartEndNestedProperty() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");
		List<Rating> ratings = ratingRepository.findByUserNameAndMovieTitleAndStars("Michal", "Speed", 3);
		assertEquals(1, ratings.size());
		assertEquals("Michal", ratings.get(0).getUser().getName());
		assertEquals("Speed", ratings.get(0).getMovie().getTitle());
		assertEquals(3, ratings.get(0).getStars());

		ratings = ratingRepository.findByUserNameAndMovieTitleAndStars("Michal", "Speed", 0);
		assertEquals(0, ratings.size());

	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindRelEntitiesWithTwoStartNodeNestedProperties() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal', middleName:'M'}) CREATE (u2:User {name:'Vince', middleName:'M'}) " +
				" CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)" +
				" CREATE (u2)-[:RATED {stars:4}]->(m2)");
		List<Rating> ratings = ratingRepository.findByUserNameAndUserMiddleName("Michal", "M");
		assertEquals(2, ratings.size());
		Collections.sort(ratings);
		assertEquals("Michal", ratings.get(0).getUser().getName());
		assertEquals("Speed", ratings.get(0).getMovie().getTitle());
		assertEquals("Michal", ratings.get(1).getUser().getName());
		assertEquals("The Matrix", ratings.get(1).getMovie().getTitle());

		ratings = ratingRepository.findByUserNameAndUserMiddleName("Michal", "V");
		assertEquals(0, ratings.size());

	}


}
