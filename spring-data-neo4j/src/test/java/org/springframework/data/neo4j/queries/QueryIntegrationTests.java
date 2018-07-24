/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.Gender;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.RichUserQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResultObject;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.UnmanagedUserPojo;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
@ContextConfiguration(classes = { QueryIntegrationTests.MoviesContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class QueryIntegrationTests extends MultiDriverTestClass {

	@Autowired PlatformTransactionManager platformTransactionManager;

	@Autowired private UserRepository userRepository;

	@Autowired private CinemaRepository cinemaRepository;

	private TransactionTemplate transactionTemplate;

	@Before
	public void clearDatabase() {
		transactionTemplate = new TransactionTemplate(platformTransactionManager);
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	private void executeUpdate(String cypher) {
		getGraphDatabaseService().execute(cypher);
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
				User user = userRepository.findUserByNameAndSurnameUsingSpElIndexAndPlaceholderWithOneParameter("Michal");
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
				User user = userRepository.findUserByNameAndSurnameUsingSpElPropertyAndPlaceholderWithOneParameter("Michal");
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

	/**
	 * @see DATAGRAPH-698, DATAGRAPH-861
	 */
	@Test
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
						+ "(:User {name:'Sarah Taylor', gender:'FEMALE', account: '3456789', deposits:['12345.6','45678.9']})");

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
				assertEquals(BigInteger.valueOf(3456789), userQueryResult.getUserAccount());
				assertArrayEquals(new BigDecimal[] { BigDecimal.valueOf(12345.6), BigDecimal.valueOf(45678.9) },
						userQueryResult.getUserDeposits());
				assertFalse(userIterator.hasNext());
			}
		});
	}

	/**
	 * @see DATAGRAPH-694
	 */
	@Test
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

	/**
	 * @see DATAGRAPH-694
	 */
	@Test
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

	/**
	 * @see DATAGRAPH-727
	 */
	@Test
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

	/**
	 * @see DATAGRAPH-772
	 */
	@Test
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

	/**
	 * @see DATAGRAPH-772
	 */
	@Test
	public void shouldMapNullsToQueryResults() {
		executeUpdate("CREATE (g:User), (s:User)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertEquals("There should be some users in the database", 2, userRepository.findTotalUsers());

				Iterable<UserQueryResult> expected = Arrays.asList(new UserQueryResult(null, 0), new UserQueryResult(null, 0));

				Iterable<UserQueryResult> queryResult = userRepository.retrieveAllUsersAndTheirAges();
				assertNotNull("The query result shouldn't be null", queryResult);
				assertEquals(expected, queryResult);
				for (UserQueryResult userQueryResult : queryResult) {
					assertNotNull(userQueryResult.getUserId());
				}
			}
		});
	}

	/**
	 * @see DATAGRAPH-700
	 */
	@Test
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

	/**
	 * @see DATAGRAPH-700
	 */
	@Test
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
				assertEquals(2, result.getUser().getFriends().size()); // we expect friends to be mapped since the relationships
																																// were returned
			}
		});
	}

	/**
	 * @see DATAGRAPH-700
	 */
	@Test
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

	/**
	 * @see DATAGRAPH-700
	 */
	@Test
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
				assertEquals(0, result.getUser().getFriends().size()); // we do not expect friends to be mapped since the
																																// relationships were not returned
			}
		});
	}

	/**
	 * @see DATAGRAPH-700
	 */
	@Test
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

	/**
	 * @see DATAGRAPH-700
	 */
	@Test
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

	/**
	 * @see DATAGRAPH-860
	 */
	@Test
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

	@Configuration
	@ComponentScan({ "org.springframework.data.neo4j.examples.movies.service" })
	@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.movies.repo")
	@EnableTransactionManagement
	static class MoviesContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.examples.movies.domain");
		}
	}
}
