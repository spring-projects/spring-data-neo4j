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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.ServerControls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.examples.movies.domain.Gender;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.RichUserQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResultObject;
import org.springframework.data.neo4j.examples.movies.repo.UnmanagedUserPojo;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
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
				assertThat(graph).isNotNull();
				int i = 0;
				for (Map<String, Object> properties : graph) {
					i++;
					assertThat(properties).isNotNull();
				}
				assertThat(i).isEqualTo(2);
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
				assertThat(ids.size()).isEqualTo(2);

				List<Long> nodeIds = userRepository.getUserNodeIds();
				assertThat(nodeIds.size()).isEqualTo(2);
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(2).isEqualTo(users);
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
				assertThat(2).isEqualTo(users.size());
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(users).isNotNull();
				int i = 0;
				for (Map<String, Object> properties : users) {
					i++;
					assertThat(properties).isNotNull();
				}
				assertThat(i).isEqualTo(2);
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
				assertThat(userRepository.findTotalUsers())
						.as("There should be some users in the database").isEqualTo(3);

				Iterable<UserQueryResult> expected = Arrays.asList(new UserQueryResult("Sheila", 29),
						new UserQueryResult("Gary", 32), new UserQueryResult("Vince", 66));

				Iterable<UserQueryResult> queryResult = userRepository.retrieveAllUsersAndTheirAges();
				assertThat(queryResult).as("The query result shouldn't be null")
						.isNotNull();
				assertThat(queryResult).isEqualTo(expected);
				for (UserQueryResult userQueryResult : queryResult) {
					assertThat(userQueryResult.getUserId()).isNotNull();
					assertThat(userQueryResult.getId()).isNotNull();
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
				assertThat(queryResult).as("The query result shouldn't be null")
						.isNotNull();
				assertThat(queryResult.getName()).isEqualTo("Jeff");
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
				assertThat(result).as("The query result shouldn't be null").isNotNull();
				assertThat(result.getName()).as("The wrong user was returned")
						.isEqualTo("Abraham");
				assertThat(result.getAgeOfUser()).as("The wrong user was returned")
						.isEqualTo(31);
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
				assertThat(usersByGender).as("The resultant users list shouldn't be null")
						.isNotNull();

				Iterator<RichUserQueryResult> userIterator = usersByGender.iterator();
				assertThat(userIterator.hasNext()).isTrue();
				RichUserQueryResult userQueryResult = userIterator.next();
				assertThat(userQueryResult.getUserGender()).isEqualTo(Gender.FEMALE);
				assertThat(userQueryResult.getUserName()).isEqualTo("Sarah Taylor");
				assertThat(userQueryResult.getYearOfBirth()).isEqualTo(Year.of(1979));
				assertThat(userQueryResult.getUserAccount())
						.isEqualTo(BigInteger.valueOf(3456789));
				assertThat(userQueryResult.getUserDeposits())
						.isEqualTo(new BigDecimal[] { BigDecimal.valueOf(12345.6), BigDecimal.valueOf(45678.9) });
				assertThat(userIterator.hasNext()).isFalse();
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
				assertThat(michal).isNotNull();
				User user = userRepository.loadUserById(michal);
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(michal).isNotNull();
				User user = userRepository.loadUserByNamedId(michal);
				assertThat(user.getName()).isEqualTo("Michal");
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
				assertThat(count).isEqualTo(2);
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
					assertThat(u.getName()).isNull();
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
				assertThat(userRepository.findTotalUsers())
						.as("There should be some users in the database").isEqualTo(2);

				Iterable<UserQueryResult> expected = Arrays
						.asList(new UserQueryResult(null, 0), new UserQueryResult(null, 0));

				Iterable<UserQueryResult> queryResult = userRepository.retrieveAllUsersAndTheirAges();
				assertThat(queryResult).as("The query result shouldn't be null")
						.isNotNull();
				assertThat(queryResult).isEqualTo(expected);
				for (UserQueryResult userQueryResult : queryResult) {
					assertThat(userQueryResult.getUserId()).isNotNull();
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
				assertThat(wrappedUser).as("The loaded wrapper object shouldn't be null")
						.isNotNull();
				assertThat(wrappedUser.getUser())
						.as("The enclosed user shouldn't be null").isNotNull();
				assertThat(wrappedUser.getUser().getName()).isEqualTo("Barry");
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
				assertThat(result).as("The result shouldn't be null").isNotNull();
				assertThat(result.getUser()).as("The enclosed user shouldn't be null")
						.isNotNull();
				assertThat(result.getUser().getName()).isEqualTo("Daniela");
				assertThat(result.getFriends().size()).isEqualTo(2);
				List<String> friends = new ArrayList<>();
				for (User u : result.getFriends()) {
					friends.add(u.getName());
				}
				assertThat(friends.contains("Ethan")).isTrue();
				assertThat(friends.contains("Finn")).isTrue();
				// we expect friends to be mapped since the relationships were returned
				assertThat(result.getUser().getFriends().size()).isEqualTo(2);
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
				assertThat(result).as("The loaded wrapper object shouldn't be null")
						.isNotNull();
				assertThat(result.getUser()).as("The enclosed user shouldn't be null")
						.isNotNull();
				assertThat(result.getUser().getName()).isEqualTo("Gary");
				assertThat(result.getRatings().size()).isEqualTo(2);
				for (Rating rating : result.getRatings()) {
					if (rating.getStars() == 4) {
						assertThat(rating.getMovie().getName())
								.isEqualTo("The Hobbit: An Unexpected Journey");
					} else {
						assertThat(rating.getMovie().getName())
								.isEqualTo("Star Wars: The Force Awakens");
					}
				}

				assertThat(result.getAvgRating()).isCloseTo(4.5f, offset(0f));
				assertThat(result.getMovies().length).isEqualTo(2);
				List<String> titles = new ArrayList<>();
				for (TempMovie movie : result.getMovies()) {
					titles.add(movie.getName());
				}
				assertThat(titles.contains("The Hobbit: An Unexpected Journey")).isTrue();
				assertThat(titles.contains("Star Wars: The Force Awakens")).isTrue();
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
				assertThat(result).as("The result shouldn't be null").isNotNull();
				assertThat(result.getUser()).as("The enclosed user shouldn't be null")
						.isNotNull();
				assertThat(result.getUser().getName()).isEqualTo("Ingrid");
				assertThat(result.getFriends().size()).isEqualTo(2);
				List<String> friends = new ArrayList<>();
				for (User u : result.getFriends()) {
					friends.add(u.getName());
				}
				assertThat(friends.contains("Kate")).isTrue();
				assertThat(friends.contains("Jake")).isTrue();
				// we do not expect friends to be mapped since the relationships were not returned
				assertThat(result.getUser().getFriends().size()).isEqualTo(0);
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
				assertThat(results.size()).isEqualTo(2);
				EntityWrappingQueryResult result = results.get(0);

				assertThat(result).as("The loaded wrapper object shouldn't be null")
						.isNotNull();
				assertThat(result.getUser()).as("The enclosed user shouldn't be null")
						.isNotNull();
				assertThat(result.getUser().getName()).isEqualTo("Harry");
				assertThat(result.getRatings().size()).isEqualTo(1);
				Rating rating = result.getRatings().get(0);
				assertThat(rating.getMovie().getName())
						.isEqualTo("The Hobbit: An Unexpected Journey");
				assertThat(rating.getStars()).isEqualTo(3);
				assertThat(result.getAvgRating()).isCloseTo(3f, offset(0f));
				assertThat(result.getMovies().length).isEqualTo(1);
				assertThat(result.getMovies()[0].getName())
						.isEqualTo("The Hobbit: An Unexpected Journey");

				result = results.get(1);
				assertThat(result).as("The loaded wrapper object shouldn't be null")
						.isNotNull();
				assertThat(result.getUser()).as("The enclosed user shouldn't be null")
						.isNotNull();
				assertThat(result.getUser().getName()).isEqualTo("Gary");
				for (Rating r : result.getRatings()) {
					if (r.getStars() == 4) {
						assertThat(r.getMovie().getName())
								.isEqualTo("The Hobbit: An Unexpected Journey");
					} else {
						assertThat(r.getMovie().getName())
								.isEqualTo("Star Wars: The Force Awakens");
					}
				}

				assertThat(result.getAvgRating()).isCloseTo(4.5f, offset(0f));
				assertThat(result.getMovies().length).isEqualTo(2);
				List<String> titles = new ArrayList<>();
				for (TempMovie movie : result.getMovies()) {
					titles.add(movie.getName());
				}
				assertThat(titles.contains("The Hobbit: An Unexpected Journey")).isTrue();
				assertThat(titles.contains("Star Wars: The Force Awakens")).isTrue();
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
				assertThat(result).as("The query result shouldn't be null").isNotNull();
				assertThat(result.getUser()).as("The mapped user shouldn't be null")
						.isNotNull();
				assertThat(result.getUser().getName()).as("The wrong user was returned")
						.isEqualTo("Abraham");
				assertThat(result.getAgeOfUser()).as("The wrong user was returned")
						.isEqualTo(31);
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
				assertThat(result).isNotNull();
				assertThat(result.getAllRatings().size()).isEqualTo(0);
			}
		});
	}

	@Test // DATAGRAPH-1310
	public void enumsShouldBeConvertedOnLoad() {
		executeUpdate("CREATE (g:User {name:'ABC', gender: 'UNDISCLOSED'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.findUserByName("ABC");
				assertThat(user).isNotNull().extracting(User::getGender).isEqualTo(Gender.UNDISCLOSED);
			}
		});
	}

	@Test // DATAGRAPH-1310
	public void enumsShouldBeConvertedOnSave() {

		long newUserId = transactionTemplate.execute(new TransactionCallback<Long>() {

			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				User abc = new User("Miriam");
				abc.setGender(Gender.FEMALE);
				return userRepository.save(abc).getId();
			}
		});

		GraphDatabaseService graph = neo4jTestServer.graph();
		try (Transaction transaction = graph.beginTx()) {

			final String gender = (String) graph.execute("MATCH (u) WHERE id(u) = $id RETURN u.gender AS gender",
					Collections.singletonMap("id", newUserId)).next().get("gender");
			assertThat(gender).isEqualTo("FEMALE");
			transaction.success();
		}
	}
}
