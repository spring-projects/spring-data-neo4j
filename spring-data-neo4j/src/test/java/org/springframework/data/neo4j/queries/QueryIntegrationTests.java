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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.harness.ServerControls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.Gender;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.RichUserQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResultObject;
import org.springframework.data.neo4j.examples.movies.repo.UnmanagedUserPojo;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@RunWith(SpringRunner.class)
public class QueryIntegrationTests {

	@Autowired private ServerControls neo4jTestServer;

	@Autowired private UserRepository userRepository;

	@Autowired private TransactionTemplate transactionTemplate;

	@Before
	public void clearDatabase() {
		neo4jTestServer.graph().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	private void executeUpdate(String cypher) {
		neo4jTestServer.graph().execute(cypher);
	}

	@Test
	public void shouldFindArbitraryGraph() {
		executeUpdate("CREATE " + "(dh:Movie {name:'Die Hard'}), " + "(fe:Movie {name: 'The Fifth Element'}), "
				+ "(bw:User {name: 'Bruce Willis'}), " + "(ar:User {name: 'Alan Rickman'}), "
				+ "(mj:User {name: 'Milla Jovovich'}), " + "(mj)-[:ACTED_IN]->(fe), " + "(ar)-[:ACTED_IN]->(dh), "
				+ "(bw)-[:ACTED_IN]->(dh), " + "(bw)-[:ACTED_IN]->(fe)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Map<String, Object>> graph = userRepository.getGraph();
				assertNotNull(graph);
				int i = 0;
				for (Map<String, Object> properties : graph) {
					i++;
					assertNotNull(properties);
				}
				assertEquals(2, i);
			}
		});
	}

	@Test
	public void shouldFindScalarValues() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Integer> ids = userRepository.getUserIds();
				assertEquals(2, ids.size());

				List<Long> nodeIds = userRepository.getUserNodeIds();
				assertEquals(2, nodeIds.size());
			}
		});
	}

	@Test
	public void shouldFindUserByName() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByName("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameUsingSpElWithObject() {
		executeUpdate("CREATE (m:User {name:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameUsingSpElWithObject(new User("Michal"));
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test // DATAGRAPH-1184
	public void customQueriesShouldBeAbleToReturnOptionals() {
		executeUpdate("CREATE (m:User {name:'Michael'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Optional<User> user = userRepository.findOptionalUserWithCustomQuery("Michael");
				assertTrue(user.isPresent());
				assertEquals("Michael", user.map(User::getName).get());
			}
		});

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Optional<User> user = userRepository.findOptionalUserWithCustomQuery("Joe User");
				assertFalse(user.isPresent());
			}
		});
	}

	@Test // DATAGRAPH-1184
	public void customQueriesShouldBeAbleToReturnOptionalQueryResults() {
		executeUpdate("CREATE (m:User {name:'Michael'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Optional<UserQueryResult> user = userRepository.findOptionalUserResultWithCustomQuery("Michael");
				assertTrue(user.isPresent());
				assertEquals("Michael", user.map(UserQueryResult::getUserName).get());
				assertEquals(42L, (long) user.map(UserQueryResult::getAge).get());
			}
		});

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Optional<UserQueryResult> user = userRepository.findOptionalUserResultWithCustomQuery("Joe User");
				assertFalse(user.isPresent());
			}
		});
	}

	@Test
	public void shouldFindUserByNameUsingSpElWithIndex() {
		executeUpdate("CREATE (m:User {name:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameUsingSpElWithIndex("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameUsingSpElWithIndexColon() {
		executeUpdate("CREATE (m:User {name:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameUsingSpElWithIndexColon("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByAgeUsingSpElWithGenericSpElExpression() {
		executeUpdate("CREATE (m:User {age:10, name:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameUsingSpElWithSpElExpression();
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameAndMiddleNameUsingSpElWithObject() {
		executeUpdate("CREATE (m:User {name:'Michal', middleName:'Hans'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User userForSearch = new User("Michal");
				userForSearch.setMiddleName("Hans");
				User user = userRepository.findUserByNameAndMiddleNameUsingSpElWithObject(userForSearch);
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameUsingSpElWithValue() {
		executeUpdate("CREATE (m:User {name:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameAndMiddleNameUsingSpElWithValue();
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameAndSurnameUsingSpElIndexAndPlaceholderWithOneParameter() {
		executeUpdate("CREATE (m:User {name:'Michal', surname:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository
						.findUserByNameAndSurnameUsingSpElIndexAndPlaceholderWithOneParameter("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameAndSurnameUsingSpElPropertyAndPlaceholderWithOneParameter() {
		executeUpdate("CREATE (m:User {name:'Michal', surname:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository
						.findUserByNameAndSurnameUsingSpElPropertyAndPlaceholderWithOneParameter("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameAndSurnameUsingSpElPropertyAndIndexWithOneParameter() {
		executeUpdate("CREATE (m:User {name:'Michal', surname:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameAndSurnameUsingSpElPropertyAndIndexWithOneParameter("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameAndSurnameUsingSpElPropertyTwice() {
		executeUpdate("CREATE (m:User {name:'Michal', surname:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameAndSurnameUsingSpElPropertyTwice("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameAndSurnameUsingSpElPropertyAndSpElIndex() {
		executeUpdate("CREATE (m:User {name:'Michal', surname:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameAndSurnameUsingSpElPropertyAndSpElIndex("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUserByNameUsingNativeIndexAndNameAndSpElNameAndSpElIndex() {
		executeUpdate("CREATE (m:User {name:'Michal'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameUsingNativeIndexAndNameAndSpElNameAndSpElIndex("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindTotalUsers() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				int users = userRepository.findTotalUsers();
				assertEquals(users, 2);
			}
		});
	}

	@Test
	public void shouldFindUsers() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Collection<User> users = userRepository.getAllUsers();
				assertEquals(users.size(), 2);
			}
		});
	}

	@Test
	public void shouldFindUserByNameWithNamedParam() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameWithNamedParam("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test // DATAGRAPH-1135
	public void namedParameterShouldWorkWithouthAtParamsInAtQueryMethods() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByNameWithNamedParamWithoutParamAnnotation("Michal");
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test
	public void shouldFindUsersAsProperties() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Iterable<Map<String, Object>> users = userRepository.getUsersAsProperties();
				assertNotNull(users);
				int i = 0;
				for (Map<String, Object> properties : users) {
					i++;
					assertNotNull(properties);
				}
				assertEquals(2, i);
			}
		});
	}

	@Test // DATAGRAPH-698, DATAGRAPH-861
	public void shouldFindUsersAndMapThemToConcreteQueryResultObjectCollection() {
		executeUpdate(
				"CREATE (g:User {name:'Gary', age:32}), (s:User {name:'Sheila', age:29}), (v:User {name:'Vince', age:66})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertEquals("There should be some users in the database", 3, userRepository.findTotalUsers());

				Iterable<UserQueryResult> expected = Arrays.asList(new UserQueryResult("Sheila", 29),
						new UserQueryResult("Gary", 32), new UserQueryResult("Vince", 66));

				Iterable<UserQueryResult> queryResult = userRepository.retrieveAllUsersAndTheirAges();
				assertNotNull("The query result shouldn't be null", queryResult);
				assertEquals(expected, queryResult);
				for (UserQueryResult userQueryResult : queryResult) {
					assertNotNull(userQueryResult.getUserId());
					assertNotNull(userQueryResult.getId());
				}
			}
		});
	}

	/**
	 * This limitation about not handling unmanaged types may be addressed after M2 if there's demand for it.
	 */
	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void shouldThrowMappingExceptionIfQueryResultTypeIsNotManagedInMappingMetadata() {
		executeUpdate("CREATE (:User {name:'Colin'}), (:User {name:'Jeff'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				// NB: UnmanagedUserPojo is not scanned with the other domain classes
				UnmanagedUserPojo queryResult = userRepository.findIndividualUserAsDifferentObject("Jeff");
				assertNotNull("The query result shouldn't be null", queryResult);
				assertEquals("Jeff", queryResult.getName());
			}
		});
	}

	@Test
	public void shouldFindUsersAndMapThemToProxiedQueryResultInterface() {
		executeUpdate(
				"CREATE (:User {name:'Morne', age:30}), (:User {name:'Abraham', age:31}), (:User {name:'Virat', age:27})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				UserQueryResultObject result = userRepository.findIndividualUserAsProxiedObject("Abraham");
				assertNotNull("The query result shouldn't be null", result);
				assertEquals("The wrong user was returned", "Abraham", result.getName());
				assertEquals("The wrong user was returned", 31, result.getAgeOfUser());
			}
		});
	}

	@Test
	public void shouldRetrieveUsersByGenderAndConvertToCorrectTypes() {
		executeUpdate(
				"CREATE (:User {name:'David Warner', gender:'MALE'}), (:User {name:'Shikhar Dhawan', gender:'MALE'}), "
						+ "(:User {name:'Sarah Taylor', gender:'FEMALE', account: '3456789', yearOfBirth: 1979, deposits:['12345.6','45678.9']})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Iterable<RichUserQueryResult> usersByGender = userRepository.findUsersByGender(Gender.FEMALE);
				assertNotNull("The resultant users list shouldn't be null", usersByGender);

				Iterator<RichUserQueryResult> userIterator = usersByGender.iterator();
				assertTrue(userIterator.hasNext());
				RichUserQueryResult userQueryResult = userIterator.next();
				assertEquals(Gender.FEMALE, userQueryResult.getUserGender());
				assertEquals("Sarah Taylor", userQueryResult.getUserName());
				assertEquals(Year.of(1979), userQueryResult.getYearOfBirth());
				assertEquals(BigInteger.valueOf(3456789), userQueryResult.getUserAccount());
				assertArrayEquals(new BigDecimal[] { BigDecimal.valueOf(12345.6), BigDecimal.valueOf(45678.9) },
						userQueryResult.getUserDeposits());
				assertFalse(userIterator.hasNext());
			}
		});
	}

	@Test // DATAGRAPH-694
	public void shouldSubstituteUserId() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = userRepository.findUserByName("Michal");
				assertNotNull(michal);
				User user = userRepository.loadUserById(michal);
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test // DATAGRAPH-694
	public void shouldSubstituteNamedParamUserId() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = userRepository.findUserByName("Michal");
				assertNotNull(michal);
				User user = userRepository.loadUserByNamedId(michal);
				assertEquals("Michal", user.getName());
			}
		});
	}

	@Test // DATAGRAPH-727
	public void shouldFindIterableUsers() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Iterable<User> users = userRepository.getAllUsersIterable();
				int count = 0;
				for (User user : users) {
					count++;
				}
				assertEquals(2, count);
			}
		});
	}

	@Test // DATAGRAPH-772
	public void shouldAllowNullParameters() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				userRepository.setNamesNull(null);
				Iterable<User> users = userRepository.findAll();
				for (User u : users) {
					assertNull(u.getName());
				}
			}
		});
	}

	@Test // DATAGRAPH-772
	public void shouldMapNullsToQueryResults() {
		executeUpdate("CREATE (g:User), (s:User)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertEquals("There should be some users in the database", 2, userRepository.findTotalUsers());

				Iterable<UserQueryResult> expected = Arrays
						.asList(new UserQueryResult(null, 0), new UserQueryResult(null, 0));

				Iterable<UserQueryResult> queryResult = userRepository.retrieveAllUsersAndTheirAges();
				assertNotNull("The query result shouldn't be null", queryResult);
				assertEquals(expected, queryResult);
				for (UserQueryResult userQueryResult : queryResult) {
					assertNotNull(userQueryResult.getUserId());
				}
			}
		});
	}

	@Test // DATAGRAPH-700
	public void shouldMapNodeEntitiesIntoQueryResultObjects() {
		executeUpdate("CREATE (:User {name:'Abraham'}), (:User {name:'Barry'}), (:User {name:'Colin'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				EntityWrappingQueryResult wrappedUser = userRepository.findWrappedUserByName("Barry");
				assertNotNull("The loaded wrapper object shouldn't be null", wrappedUser);
				assertNotNull("The enclosed user shouldn't be null", wrappedUser.getUser());
				assertEquals("Barry", wrappedUser.getUser().getName());
			}
		});
	}

	@Test // DATAGRAPH-700
	public void shouldMapNodeCollectionsIntoQueryResultObjects() {
		executeUpdate(
				"CREATE (d:User {name:'Daniela'}),  (e:User {name:'Ethan'}), (f:User {name:'Finn'}), (d)-[:FRIEND_OF]->(e), (d)-[:FRIEND_OF]->(f)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				EntityWrappingQueryResult result = userRepository.findWrappedUserAndFriendsDepth1("Daniela");
				assertNotNull("The result shouldn't be null", result);
				assertNotNull("The enclosed user shouldn't be null", result.getUser());
				assertEquals("Daniela", result.getUser().getName());
				assertEquals(2, result.getFriends().size());
				List<String> friends = new ArrayList<>();
				for (User u : result.getFriends()) {
					friends.add(u.getName());
				}
				assertTrue(friends.contains("Ethan"));
				assertTrue(friends.contains("Finn"));
				// we expect friends to be mapped since the relationships were returned
				assertEquals(2, result.getUser().getFriends().size());
			}
		});
	}

	@Test // DATAGRAPH-700
	public void shouldMapRECollectionsIntoQueryResultObjects() {
		executeUpdate(
				"CREATE (g:User {name:'Gary'}), (sw:Movie {name: 'Star Wars: The Force Awakens'}), (hob:Movie {name:'The Hobbit: An Unexpected Journey'}), (g)-[:RATED {stars : 5}]->(sw), (g)-[:RATED {stars: 4}]->(hob) ");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				EntityWrappingQueryResult result = userRepository.findWrappedUserAndRatingsByName("Gary");
				assertNotNull("The loaded wrapper object shouldn't be null", result);
				assertNotNull("The enclosed user shouldn't be null", result.getUser());
				assertEquals("Gary", result.getUser().getName());
				assertEquals(2, result.getRatings().size());
				for (Rating rating : result.getRatings()) {
					if (rating.getStars() == 4) {
						assertEquals("The Hobbit: An Unexpected Journey", rating.getMovie().getName());
					} else {
						assertEquals("Star Wars: The Force Awakens", rating.getMovie().getName());
					}
				}

				assertEquals(4.5f, result.getAvgRating(), 0);
				assertEquals(2, result.getMovies().length);
				List<String> titles = new ArrayList<>();
				for (TempMovie movie : result.getMovies()) {
					titles.add(movie.getName());
				}
				assertTrue(titles.contains("The Hobbit: An Unexpected Journey"));
				assertTrue(titles.contains("Star Wars: The Force Awakens"));
			}
		});
	}

	@Test // DATAGRAPH-700
	public void shouldMapRelationshipCollectionsWithDepth0IntoQueryResultObjects() {
		executeUpdate(
				"CREATE (i:User {name:'Ingrid'}),  (j:User {name:'Jake'}), (k:User {name:'Kate'}), (i)-[:FRIEND_OF]->(j), (i)-[:FRIEND_OF]->(k)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				EntityWrappingQueryResult result = userRepository.findWrappedUserAndFriendsDepth0("Ingrid");
				assertNotNull("The result shouldn't be null", result);
				assertNotNull("The enclosed user shouldn't be null", result.getUser());
				assertEquals("Ingrid", result.getUser().getName());
				assertEquals(2, result.getFriends().size());
				List<String> friends = new ArrayList<>();
				for (User u : result.getFriends()) {
					friends.add(u.getName());
				}
				assertTrue(friends.contains("Kate"));
				assertTrue(friends.contains("Jake"));
				// we do not expect friends to be mapped since the relationships were not returned
				assertEquals(0, result.getUser().getFriends().size());
			}
		});
	}

	@Test // DATAGRAPH-700
	public void shouldReturnMultipleQueryResultObjects() {
		executeUpdate(
				"CREATE (g:User {name:'Gary'}), (h:User {name:'Harry'}), (sw:Movie {name: 'Star Wars: The Force Awakens'}), (hob:Movie {name:'The Hobbit: An Unexpected Journey'}), (g)-[:RATED {stars : 5}]->(sw), (g)-[:RATED {stars: 4}]->(hob), (h)-[:RATED {stars: 3}]->(hob) ");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<EntityWrappingQueryResult> results = userRepository.findAllUserRatings();
				assertEquals(2, results.size());
				EntityWrappingQueryResult result = results.get(0);

				assertNotNull("The loaded wrapper object shouldn't be null", result);
				assertNotNull("The enclosed user shouldn't be null", result.getUser());
				assertEquals("Harry", result.getUser().getName());
				assertEquals(1, result.getRatings().size());
				Rating rating = result.getRatings().get(0);
				assertEquals("The Hobbit: An Unexpected Journey", rating.getMovie().getName());
				assertEquals(3, rating.getStars());
				assertEquals(3f, result.getAvgRating(), 0);
				assertEquals(1, result.getMovies().length);
				assertEquals("The Hobbit: An Unexpected Journey", result.getMovies()[0].getName());

				result = results.get(1);
				assertNotNull("The loaded wrapper object shouldn't be null", result);
				assertNotNull("The enclosed user shouldn't be null", result.getUser());
				assertEquals("Gary", result.getUser().getName());
				for (Rating r : result.getRatings()) {
					if (r.getStars() == 4) {
						assertEquals("The Hobbit: An Unexpected Journey", r.getMovie().getName());
					} else {
						assertEquals("Star Wars: The Force Awakens", r.getMovie().getName());
					}
				}

				assertEquals(4.5f, result.getAvgRating(), 0);
				assertEquals(2, result.getMovies().length);
				List<String> titles = new ArrayList<>();
				for (TempMovie movie : result.getMovies()) {
					titles.add(movie.getName());
				}
				assertTrue(titles.contains("The Hobbit: An Unexpected Journey"));
				assertTrue(titles.contains("Star Wars: The Force Awakens"));
			}
		});
	}

	@Test // DATAGRAPH-700
	public void shouldMapEntitiesToProxiedQueryResultInterface() {
		executeUpdate(
				"CREATE (:User {name:'Morne', age:30}), (:User {name:'Abraham', age:31}), (:User {name:'Virat', age:27})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				UserQueryResultObject result = userRepository.findWrappedUserAsProxiedObject("Abraham");
				assertNotNull("The query result shouldn't be null", result);
				assertNotNull("The mapped user shouldn't be null", result.getUser());
				assertEquals("The wrong user was returned", "Abraham", result.getUser().getName());
				assertEquals("The wrong user was returned", 31, result.getAgeOfUser());
			}
		});
	}

	@Test // DATAGRAPH-860
	public void shouldMapEmptyNullCollectionsToQueryResultInterface() {
		executeUpdate("CREATE (g:User {name:'Gary'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				EntityWrappingQueryResult result = userRepository.findAllRatingsNull();
				assertNotNull(result);
				assertEquals(0, result.getAllRatings().size());
			}
		});
	}

	@Test // DATAGRAPH-1249
	public void shouldFlushSessionAfterBulkUpdateReturningNodes() {

		executeUpdate("CREATE (:User {name:'Schneider'}), (:User {name:'Hundingsbane'})");
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertEquals(2, ((List<User>) userRepository.findAll()).size());

				List<String> names = userRepository.bulkUpdateReturningNode().stream()
						.map(User::getSurname)
						.distinct().collect(Collectors.toList());
				assertEquals(1, names.size());
				assertTrue(names.contains("Helge"));

				names = StreamSupport.stream(userRepository.findAll().spliterator(), false)
						.map(User::getSurname)
						.distinct().collect(Collectors.toList());
				assertEquals(1, names.size());
				assertTrue(names.contains("Helge"));
			}
		});
	}

	@Test // DATAGRAPH-1249
	public void shouldFlushSessionAfterBulkUpdateWithoutNodes() {

		executeUpdate("CREATE (:User {name:'Schneider'}), (:User {name:'Hundingsbane'})");
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertEquals(2, ((List<User>) userRepository.findAll()).size());

				userRepository.bulkUpdateNoReturn();

				List<String> names = StreamSupport.stream(userRepository.findAll().spliterator(), false)
						.map(User::getSurname)
						.distinct().collect(Collectors.toList());
				assertEquals(1, names.size());
				assertTrue(names.contains("Helge"));
			}
		});
	}
}
