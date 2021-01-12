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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.harness.ServerControls;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.Director;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.DirectorRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@RunWith(SpringRunner.class)
public class DerivedQueryTests {

	@Autowired private ServerControls neo4jTestServer;

	@Autowired private Session session;

	@Autowired private UserRepository userRepository;

	@Autowired private CinemaRepository cinemaRepository;

	@Autowired private DirectorRepository directorRepository;

	@Autowired private TransactionTemplate transactionTemplate;

	@Before
	public void clearDatabase() {
		neo4jTestServer.graph().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	private void executeUpdate(String cypher) {
		neo4jTestServer.graph().execute(cypher);
	}

	@Test
	public void shouldFindUsersByName() {
		executeUpdate("CREATE (m:User:Person {name:'Michal'})<-[:FRIEND_OF]-(a:User:Person {name:'Adam'})");

		Collection<User> users = userRepository.findByName("Michal");
		Iterator<User> iterator = users.iterator();
		assertThat(iterator.hasNext()).isTrue();
		User user = iterator.next();
		assertThat(user.getName()).isEqualTo("Michal");
		assertThat(user.getFriends().size()).isEqualTo(1);
		assertThat(user.getFriends().iterator().next().getName()).isEqualTo("Adam");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldFindUsersByMiddleName() {
		executeUpdate("CREATE (m:User:Person {middleName:'Joseph'})<-[:FRIEND_OF]-(a:User:Person {middleName:'Mary', name: 'Joseph'})");

		Collection<User> users = userRepository.findByMiddleName("Joseph");
		Iterator<User> iterator = users.iterator();
		assertThat(iterator.hasNext()).isTrue();
		User user = iterator.next();
		assertThat(user.getMiddleName()).isEqualTo("Joseph");
		assertThat(user.getFriends().size()).isEqualTo(1);
		User friend = user.getFriends().iterator().next();
		assertThat(friend.getMiddleName()).isEqualTo("Mary");
		assertThat(friend.getName()).isEqualTo("Joseph");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test // DATAGRAPH-628
	public void shouldFindNodeEntitiesWithLabels() {
		executeUpdate(
				"CREATE (u:User:Person {name:'Michal'}) CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'}) CREATE (u)-[:VISITED]->(p)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Collection<Cinema> cinemas = cinemaRepository.findByName("Picturehouse");
				Iterator<Cinema> iterator = cinemas.iterator();
				assertThat(iterator.hasNext()).isTrue();
				Cinema cinema = iterator.next();
				assertThat(cinema.getName()).isEqualTo("Picturehouse");
				assertThat(cinema.getVisited().size()).isEqualTo(1);
				assertThat(cinema.getVisited().iterator().next().getName())
						.isEqualTo("Michal");
				assertThat(iterator.hasNext()).isFalse();

				List<Cinema> theatres = cinemaRepository.findByLocation("London");
				assertThat(theatres.size()).isEqualTo(2);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();
				assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();
			}
		});
	}

	@Test
	public void shouldUseDepthOnCollectionFinder() {
		executeUpdate(
				"CREATE (u:User {name:'Michal'}) CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'}) CREATE (u)-[:VISITED]->(p)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {

				List<Cinema> theatres = cinemaRepository.findByLocation("London", 0);
				assertThat(theatres.size()).isEqualTo(2);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();
				assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();
				assertThat(theatres.get(0).getBlockbusterOfTheWeek()).isNull();
				assertThat(theatres.get(0).getVisited().isEmpty()).isTrue();
			}
		});
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntitiesMultipleAndedProperties() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByNameAndLocation("Ritzy", "London");
		assertThat(theatres.size()).isEqualTo(1);
		assertThat(theatres.get(0).getVisited().iterator().next().getName())
				.isEqualTo("Michal");
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntititiesMultipleOredProperties() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByNameOrLocation("Ritzy", "London");
		assertThat(theatres.size()).isEqualTo(2);
	}

	@Test // DATAGRAPH-629
	public void shouldReturnNoResultsCorrectly() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		Collection<Cinema> theatres = cinemaRepository.findByName("Does not exist");
		assertThat(theatres.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntititiesWithComparisonOperators() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByCapacityGreaterThan(3000);
				assertThat(theatres.size()).isEqualTo(2);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();
				assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();

				theatres = cinemaRepository.findByCapacityGreaterThan(6000);
				assertThat(theatres.size()).isEqualTo(1);
				assertThat(theatres.get(0).getName()).isEqualTo("Ritzy");

				theatres = cinemaRepository.findByCapacityLessThan(8000);
				assertThat(theatres.size()).isEqualTo(2);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();
				assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();

				theatres = cinemaRepository.findByCapacityLessThan(7000);
				assertThat(theatres.size()).isEqualTo(1);
				assertThat(theatres.get(0).getName()).isEqualTo("Picturehouse");
			}
		});
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntititiesWithMultipleComparisonOperatorsAnded() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 4500})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByLocationAndCapacityGreaterThan("London", 3000);
				assertThat(theatres.size()).isEqualTo(2);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();
				assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();

				theatres = cinemaRepository.findByCapacityLessThanAndLocation(6000, "Bombay");
				assertThat(theatres.size()).isEqualTo(1);
				assertThat(theatres.get(0).getName()).isEqualTo("Regal");
			}
		});
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntititiesWithMultipleComparisonOperatorsOred() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 9000})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByLocationOrCapacityLessThan("London", 100);
				assertThat(theatres.size()).isEqualTo(2);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();
				assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();

				theatres = cinemaRepository.findByCapacityGreaterThanOrLocation(8000, "Paris");
				assertThat(theatres.size()).isEqualTo(1);
				assertThat(theatres.get(0).getName()).isEqualTo("Regal");
			}
		});
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntititiesWithNestedProperty() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByVisitedName("Michal");
		assertThat(theatres.size()).isEqualTo(1);
		assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();
	}

	@Test
	public void shouldFindNodeEntititiesWithDeepNestedProperty() {
		executeUpdate("CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})"
				+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r) CREATE (m1:Movie {name:'Speed'})"
				+ " CREATE (g:Genre {name:'Thriller'}) CREATE (u)-[:INTERESTED]->(g)");

		List<Cinema> theatres = cinemaRepository.findByVisitedInterestedName("Thriller");
		assertThat(theatres.size()).isEqualTo(1);
		assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntititiesWithBaseAndNestedProperty() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 5000})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(m)");

		List<Cinema> theatres = cinemaRepository.findByLocationAndVisitedName("London", "Michal");
		assertThat(theatres.size()).isEqualTo(1);
		assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();
	}

	@Test(expected = UnsupportedOperationException.class) // DATAGRAPH-662
	public void shouldFindNodeEntititiesWithBaseOrNestedProperty() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'The Old Vic', city:'London', capacity: 5000})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(m)");

		List<Cinema> theatres = cinemaRepository.findByLocationOrVisitedName("P", "Michal");
		assertThat(theatres.size()).isEqualTo(2);
		assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();
		// assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertThat(theatres.contains(new Cinema("The Old Vic"))).isTrue();
	}

	@Test // DATAGRAPH-632
	public void shouldFindNodeEntitiesWithNestedRelationshipEntityProperty() {
		executeUpdate(
				"CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})"
						+ " CREATE (u:User:Person {name:'Michal'}) CREATE (u1:User:Person {name:'Vince'}) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

		List<User> users = userRepository.findByRatingsStars(3);
		assertThat(users.size()).isEqualTo(2);
		assertThat(users.contains(new User("Michal"))).isTrue();
		assertThat(users.contains(new User("Vince"))).isTrue();
	}

	@Test // DATAGRAPH-629, DATAGRAPH-705
	public void shouldFindNodeEntititiesWithTwoNestedPropertiesAndedAcrossDifferentRelatedNodeEntities() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " + " CREATE (u:User {name:'Michal'}) "
				+ " CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" + " CREATE (m1:Movie {name:'San Andreas'}) "
				+ " CREATE (m2:Movie {name:'Pitch Perfect 2'})" + " CREATE (p)-[:BLOCKBUSTER]->(m1)"
				+ " CREATE (r)-[:BLOCKBUSTER]->(m2)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByVisitedNameAndBlockbusterOfTheWeekName("Michal", "San Andreas");
				assertThat(theatres.size()).isEqualTo(1);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();

				theatres = cinemaRepository.findByVisitedNameAndBlockbusterOfTheWeekName("Michal", "Tomorrowland");
				assertThat(theatres.size()).isEqualTo(0);
			}
		});
	}

	@Test(expected = UnsupportedOperationException.class) // DATAGRAPH-662
	public void shouldFindNodeEntititiesWithTwoNestedPropertiesOred() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " + " CREATE (u:User {name:'Michal'}) "
				+ " CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" + " CREATE (m1:Movie {title:'San Andreas'}) "
				+ " CREATE (m2:Movie {title:'Pitch Perfect 2'})" + " CREATE (p)-[:BLOCKBUSTER]->(m1)"
				+ " CREATE (r)-[:BLOCKBUSTER]->(m2)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByVisitedNameOrBlockbusterOfTheWeekName("Michal", "San Andreas");
				assertThat(theatres.size()).isEqualTo(2);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();
				assertThat(theatres.contains(new Cinema("Ritzy"))).isTrue();

				theatres = cinemaRepository.findByVisitedNameOrBlockbusterOfTheWeekName("Vince", "Tomorrowland");
				assertThat(theatres.size()).isEqualTo(0);
			}
		});
	}

	@Test
	public void shouldFindNodeEntititiesWithMultipleNestedPropertiesOred() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) "
				+ " CREATE (u:User {name:'Michal', middleName:'M'}) CREATE (u1:User {name:'Vince', middleName:'M'}) "
				+ " CREATE (u)-[:VISITED]->(p)  CREATE (u1)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByVisitedNameOrVisitedMiddleName("Vince", "M");
				assertThat(theatres.size()).isEqualTo(2);
				assertThat(theatres).extracting(Cinema::getName).containsExactlyInAnyOrder("Picturehouse", "Ritzy");
			}
		});
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntititiesWithMultipleNestedPropertiesAnded() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) "
				+ " CREATE (u:User {name:'Michal', middleName:'M'}) CREATE (u1:User {name:'Vince', middleName:'M'}) "
				+ " CREATE (u)-[:VISITED]->(p)  CREATE (u1)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByVisitedNameAndVisitedMiddleName("Michal", "M");
				assertThat(theatres.size()).isEqualTo(1);
				assertThat(theatres.contains(new Cinema("Picturehouse"))).isTrue();

				theatres = cinemaRepository.findByVisitedNameAndVisitedMiddleName("Vince", "V");
				assertThat(theatres.size()).isEqualTo(0);
			}
		});
	}

	@Test // DATAGRAPH-629
	public void shouldFindNodeEntititiesWithRelationshipEntityAndNestedProperty() {
		executeUpdate(
				"CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})"
						+ " CREATE (u:User:Person {name:'Michal'}) CREATE (u1:User:Person {name:'Vince'}) CREATE (g:Genre {name:'Thriller'}) CREATE (u)-[:INTERESTED]->(g) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

		List<User> users = userRepository.findByRatingsStarsAndInterestedName(3, "Thriller");
		assertThat(users.size()).isEqualTo(1);
		assertThat(users.contains(new User("Michal"))).isTrue();
	}

	@Test // DATAGRAPH-601, DATAGRAPH-761
	public void shouldFindNodeEntitiesByRegularExpressionMatchingOnPropertiesInDerivedFinderMethods() {
		executeUpdate("CREATE (:Theatre {name:'Odeon', city:'Preston'}), " + "(:Theatre {name:'Vue', city:'Dumfries'}), "
				+ "(:Theatre {name:'PVR', city:'Mumbai'}) ");

		// ideally, I'd name this to be "findWhereNameMatches" or "findByNameMatching"
		List<Cinema> cinemas = cinemaRepository.findByNameMatches("^[Vv].+$");
		assertThat(cinemas.size()).as("The wrong number of cinemas was returned")
				.isEqualTo(1);
		assertThat(cinemas.get(0).getLocation()).as("An unexpected cinema was retrieved")
				.isEqualTo("Dumfries");
	}

	@Test // DATAGRAPH-761
	public void shouldMatchNodeEntitiesUsingCaseInsensitiveLikeWithWildcards() {
		executeUpdate("CREATE (:Theatre {name:'IMAX', city:'Chesterfield'}), "
				+ "(:Theatre {name:'Odeon', city:'Manchester'}), " + "(:Theatre {name:'IMAX', city:'Edinburgh'}) ");

		List<Cinema> cinemas = cinemaRepository.findByLocationLike("*chest*");
		assertThat(cinemas.size()).as("The wrong number of cinemas was returned")
				.isEqualTo(2);
	}

	@Test // DATAGRAPH-761
	public void shouldMatchNodeEntitiesUsingLikeWithWildcardsAndSpecialCharacters() {
		executeUpdate("CREATE (:Theatre {name:'IMAX', city:'Kolkata (Calcutta)'}), "
				+ "(:Theatre {name:'PVR', city:'Bengaluru (Bangalore)'}), "
				+ "(:Theatre {name:'Metro Big Cinema', city:'Mumbai (Bombay)'}) ");

		List<Cinema> indianCinemas = cinemaRepository.findByLocationLike("*(B*");
		assertThat(indianCinemas.size()).as("The wrong number of cinemas was returned")
				.isEqualTo(2);
	}

	@Test // DATAGRAPH-761
	public void shouldMatchNodeEntitiesUsingNotLikeWithAsteriskWildcards() {
		executeUpdate("CREATE (:User:Person {name:'Jeff'}), " + "(:User:Person {name:'Jeremy'}), " + "(:User:Person {name:'Alan'})");

		List<User> nonMatchingUsers = userRepository.findByNameIsNotLike("Je*");
		assertThat(nonMatchingUsers.size()).as("The wrong number of users was returned")
				.isEqualTo(1);
		assertThat(nonMatchingUsers.get(0).getName()).as("The wrong user was returned")
				.isEqualTo("Alan");
	}

	@Test // DATAGRAPH-787
	public void shouldFindDirectorsByName() {
		executeUpdate(
				"CREATE (m:User:Person {name:'Michal'})<-[:FRIEND_OF]-(a:User:Person {name:'Adam'}) CREATE (d:Director:Person {name:'Vince'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Collection<Director> directors = directorRepository.findByName("Vince");
				Iterator<Director> iterator = directors.iterator();
				assertThat(iterator.hasNext()).isTrue();
				Director director = iterator.next();
				assertThat(director.getName()).isEqualTo("Vince");
				assertThat(iterator.hasNext()).isFalse();

				directors = directorRepository.findByName("Michal");
				iterator = directors.iterator();
				assertThat(iterator.hasNext()).isFalse();
			}
		});
	}

	@Test // DATAGRAPH-744
	public void shouldFindUserWithCustomDepth() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " + " CREATE (u:User {name:'Michal'}) "
				+ " CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" + " CREATE (m1:Movie {name:'San Andreas'}) "
				+ " CREATE (m2:Movie {name:'Pitch Perfect 2'})" + " CREATE (p)-[:BLOCKBUSTER]->(m1)"
				+ " CREATE (r)-[:BLOCKBUSTER]->(m2)" + " CREATE (u)-[:RATED {stars :3}]->(m1)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Cinema cinema = cinemaRepository.findByName("Picturehouse", 0);
				assertThat(cinema).isNotNull();
				assertThat(cinema.getName()).isEqualTo("Picturehouse");
				assertThat(cinema.getVisited().size()).isEqualTo(0);
				assertThat(cinema.getBlockbusterOfTheWeek()).isEqualTo(null);

				cinema = cinemaRepository.findByName("Picturehouse", 1);
				assertThat(cinema).isNotNull();
				assertThat(cinema.getName()).isEqualTo("Picturehouse");
				assertThat(cinema.getVisited().size()).isEqualTo(1);
				assertThat(cinema.getVisited().iterator().next().getRatings().size())
						.isEqualTo(0);
				assertThat(cinema.getBlockbusterOfTheWeek().getName())
						.isEqualTo("San Andreas");

				cinema = cinemaRepository.findByName("Picturehouse", 2);
				assertThat(cinema).isNotNull();
				assertThat(cinema.getName()).isEqualTo("Picturehouse");
				assertThat(cinema.getVisited().size()).isEqualTo(1);
				assertThat(cinema.getVisited().iterator().next().getRatings().size())
						.isEqualTo(1);
				assertThat(cinema.getBlockbusterOfTheWeek().getName())
						.isEqualTo("San Andreas");
			}
		});
	}

	@Test // DATAGRAPH-744
	public void shouldFindUsersByNameWithStaticDepth() {
		executeUpdate(
				"CREATE (m:User:Person {name:'Michal', surname:'Bachman'})<-[:FRIEND_OF]-(a:User:Person {name:'Adam', surname:'George'})");

		User user = userRepository.findBySurname("Bachman");
		assertThat(user).isNotNull();
		assertThat(user.getName()).isEqualTo("Michal");
		assertThat(user.getSurname()).isEqualTo("Bachman");
		assertThat(user.getFriends().size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-864
	public void shouldSupportLiteralMapsInQueryResults() {
		executeUpdate(
				"CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})"
						+ " CREATE (u:User:Person {name:'Michal'}) CREATE (u1:User:Person {name:'Vince'}) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

		List<EntityWrappingQueryResult> result = userRepository.findRatingsWithLiteralMap();
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);
		EntityWrappingQueryResult row = result.get(0);
		if (row.getUser().getName().equals("Vince")) {
			assertThat(row.getLiteralMap().size()).isEqualTo(1);
			assertThat(row.getLiteralMap().iterator().next().get("movietitle"))
					.isEqualTo("Chocolat");
			assertThat(row.getLiteralMap().iterator().next().get("stars")).isEqualTo(3L);
		} else {
			assertThat(row.getLiteralMap().size()).isEqualTo(2);
		}
	}

	@Test // DATAGRAPH-876
	public void shouldAllowMultiThreadedDerivedFinderExecution() throws InterruptedException {
		int numThreads = 3;
		executeUpdate(
				"CREATE (m:User {name:'Michal', surname:'Bachman'}), (a:User {name:'Adam', surname:'George'}), (l:User {name:'Luanne', surname:'Misquitta'})");

		String[] firstNames = new String[] { "Michal", "Adam", "Luanne" };
		String[] lastNames = new String[] { "Bachman", "George", "Misquitta" };

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CountDownLatch latch = new CountDownLatch(numThreads);

		for (int i = 0; i < numThreads; i++) {
			executor.submit(new DerivedQueryRunner(latch, firstNames[i], lastNames[i]));
		}
		latch.await(); // pause until the count reaches 0

		// force termination of all threads
		executor.shutdownNow();
	}

	@Test // DATAGRAPH-680
	@Transactional
	public void shouldPageDerivedFinderQueries() {
		for (int i = 0; i < 10; i++) {
			userRepository.save(new User("A", "B"));
		}
		userRepository.save(new User("A", "C"));

		Pageable pageable = PageRequest.of(0, 4);
		Page<User> page = userRepository.findByNameAndSurname("A", "B", pageable);
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.getTotalElements()).isEqualTo(10);
		assertThat(page.getNumber()).isEqualTo(0);
		assertThat(page.isFirst()).isTrue();
		assertThat(page.isLast()).isFalse();
		assertThat(page.hasNext()).isTrue();

		page = userRepository.findByNameAndSurname("A", "B", page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.getTotalElements()).isEqualTo(10);
		assertThat(page.getNumber()).isEqualTo(1);
		assertThat(page.isFirst()).isFalse();
		assertThat(page.isLast()).isFalse();
		assertThat(page.hasNext()).isTrue();

		page = userRepository.findByNameAndSurname("A", "B", page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(2);
		assertThat(page.getTotalElements()).isEqualTo(10);
		assertThat(page.getNumber()).isEqualTo(2);
		assertThat(page.isFirst()).isFalse();
		assertThat(page.isLast()).isTrue();
		assertThat(page.hasNext()).isFalse();

		page = userRepository.findByNameAndSurname("A", "B", PageRequest.of(0, 10));
		assertThat(page.getNumberOfElements()).isEqualTo(10);
		assertThat(page.getTotalElements()).isEqualTo(10);
		assertThat(page.getNumber()).isEqualTo(0);
		assertThat(page.isFirst()).isTrue();
		assertThat(page.isLast()).isTrue();
		assertThat(page.hasNext()).isFalse();
	}

	@Test // DATAGRAPH-680
	@Transactional
	public void shouldSliceDerivedFinderQueries() {
		for (int i = 0; i < 10; i++) {
			User user = new User("A");
			user.rate(new TempMovie("Temp"), 5, "just okay");
			userRepository.save(user);
		}
		userRepository.save(new User("A", "C"));

		Pageable pageable = PageRequest.of(0, 4);
		Slice<User> page = userRepository.findByNameAndRatingsStars("A", 5, pageable);
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.hasNext()).isTrue();

		page = userRepository.findByNameAndRatingsStars("A", 5, page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.hasNext()).isTrue();

		page = userRepository.findByNameAndRatingsStars("A", 5, page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(2);
		assertThat(page.hasNext()).isFalse();

		page = userRepository.findByNameAndRatingsStars("A", 5, PageRequest.of(0, 10));
		assertThat(page.getNumberOfElements()).isEqualTo(10);
		assertThat(page.hasNext()).isFalse();
	}

	@Test // DATAGRAPH-1286
	public void findAllByIdShouldSupportPageableParameter() {

		List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			User u = new User("U" + i);
			userRepository.save(u);
			ids.add(u.getId());
		}

		// Just make sure stuff exists and OGM correctly uses id(n) instead of n.id :(
		Optional<User> randomUser = userRepository.findById(ids.get(ids.size() - 2));
		assertThat(randomUser).isPresent().map(User::getName).hasValue("U8");

		// Assert findAllById works _at all_
		Iterable<User> allUsers = userRepository.findAllById(ids);
		assertThat(allUsers).hasSize(ids.size());

		Pageable pageable = PageRequest.of(0, 2);
		Page<User> page = userRepository.findAllByIdIn(ids.subList(0, 4), pageable);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).hasSize(2);
		assertThat(page.hasNext()).isTrue();
	}

	@Test // DATAGRAPH-1286
	public void findByIdInInDerivedQueryMethodShouldWork() {

		List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			User u = new User("U" + i);
			userRepository.save(u);
			ids.add(u.getId());
		}

		List<User> users = userRepository.findAllByIdInAndNameLike(ids.subList(0, 4), "U*");
		assertThat(users).hasSize(4);
	}

	@Test // DATAGRAPH-1286
	public void findByIdEqualsInDerivedQueryMethodShouldWork() {

		List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			User u = new User("U" + i);
			userRepository.save(u);
			ids.add(u.getId());
		}

		List<User> users = userRepository.findAllByIdAndName(ids.get(2), "U2");
		assertThat(users).hasSize(1);
	}

	@Test // GH-1792
	public void findByIdInNestedPropertyTraversalShouldWork() {
		executeUpdate("CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})"
					  + " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r) CREATE (m1:Movie {name:'Speed'})"
					  + " CREATE (g:Genre {name:'Thriller'}) CREATE (u)-[:INTERESTED]->(g)");

		long genreId = session.queryForObject(Long.class, "MATCH (g:Genre {name:'Thriller'}) RETURN id(g)",
				Collections.emptyMap());

		List<User> users = userRepository.findAllByInterestedId(genreId);
		assertThat(users).extracting(User::getName).containsExactly("Michal");
	}

	@Test // DATAGRAPH-1093
	public void shouldFindNodeEntitiesByAttributeIgnoringCase() {
		executeUpdate("CREATE (:Director:Person {name:'Patty Jenkins'})\n" + //
				"      ,(:Director:Person {name:'Marry Harron'})\n" + //
				"      ,(m1:Movie {title:'Speed'})\n" + //
				"      ,(m2:Movie {title:'The Matrix'})\n" + //
				"      ,(m3:Movie {title:'Chocolat'})\n" + //
				"      ,(g:Genre {name:'Thriller'})\n" + //
				"      ,(u1:User:Person {name:'Michal'})\n" + //
				"            ,(u1)-[:INTERESTED]->(g)\n" + "            ,(u1)-[:RATED {stars:3}]->(m1)\n" + //
				"            ,(u1)-[:RATED {stars:4}]->(m2)\n" + //
				"      ,(u2:User {name:'Vince'})\n" + //
				"            ,(u1)-[:RATED {stars:3}]->(m3)");

		Collection<Director> directors = directorRepository.findByName("paTTY jenKins");
		assertThat(directors).isEmpty();

		// Ignore case for attribute Director#name set to ALWAYS
		directors = directorRepository.findByNameIgnoreCase("paTTY jenKins");
		assertThat(directors).hasSize(1).extracting(Director::getName).containsExactly("Patty Jenkins");

		List<User> users = userRepository.findByRatingsStarsAndInterestedName(3, "THRILLER");
		assertThat(users).isEmpty();

		// Ignore case for attribute Director#name set to ALWAYS
		users = userRepository.findByRatingsStarsAndInterestedNameIgnoreCase(3, "THRILLER");
		assertThat(users).hasSize(1).extracting(User::getName).containsExactly("Michal");

		// Ignore case for both Rating#stars and Genre#name to WHEN_POSSIBLE
		users = userRepository.findByRatingsStarsAndInterestedNameAllIgnoreCase(3, "THRILLER");
		assertThat(users).hasSize(1).extracting(User::getName).containsExactly("Michal");

		// Ignore case for Rating#stars set to ALWAYS
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> userRepository.findByRatingsStarsIgnoreCase(3))
				.withMessageStartingWith("Unable to ignore case of int types, the property 'ratings' must reference a String");
	}

	@Test // DATAGRAPH-1190
	public void shouldFindNodeByNameContainingAndIgnoreCase() {

		String userName = "ABCD";
		User user = new User(userName);
		userRepository.save(user);

		User foundUser = userRepository.findByNameContainingIgnoreCase("bc").iterator().next();
		assertThat(foundUser).isNotNull();
		assertThat(foundUser.getName()).isEqualTo(userName);
	}

	class DerivedQueryRunner implements Runnable {

		private final CountDownLatch latch;
		private final String firstName;
		private final String lastName;

		public DerivedQueryRunner(CountDownLatch latch, String firstName, String lastName) {
			this.latch = latch;
			this.firstName = firstName;
			this.lastName = lastName;
		}

		@Override
		public void run() {
			try {
				User user = userRepository.findBySurname(lastName);
				assertThat(user).isNotNull();
				assertThat(user.getName()).isEqualTo(firstName);
				assertThat(user.getSurname()).isEqualTo(lastName);
			} finally {
				latch.countDown();
			}
		}
	}
}
