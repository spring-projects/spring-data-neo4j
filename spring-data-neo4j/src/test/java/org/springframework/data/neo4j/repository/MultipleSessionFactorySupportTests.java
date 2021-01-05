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
package org.springframework.data.neo4j.repository;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.examples.friends.domain.Friendship;
import org.springframework.data.neo4j.examples.friends.domain.Person;
import org.springframework.data.neo4j.examples.friends.repo.FriendshipRepository;
import org.springframework.data.neo4j.examples.restaurants.domain.Diner;
import org.springframework.data.neo4j.examples.restaurants.domain.Restaurant;
import org.springframework.data.neo4j.examples.restaurants.repo.RestaurantRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ReflectionUtils;

/**
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { MultipleSessionFactorySupportTests.BaseConfiguration.class,
		MultipleSessionFactorySupportTests.FriendsConfiguration.class,
		MultipleSessionFactorySupportTests.RestaurantsConfiguration.class })
@RunWith(SpringRunner.class)
public class MultipleSessionFactorySupportTests {

	@Rule
	public final LoggerRule loggerRule = new LoggerRule();

	private static final String BEAN_NAME_FRIENDS_SESSION_FACTORY = "sessionFactoryFriends";
	private static final String BEAN_NAME_FRIENDS_TX_MANAGER = "txManagerFriends";

	private static final String BEAN_NAME_RESTAURANTS_SESSION_FACTORY = "sessionFactoryRestaurants";
	private static final String BEAN_NAME_RESTAURANTS_TX_MANAGER = "txManagerRestaurants";

	private static final String PACKAGE_FOR_DOMAIN_OF_INSTANCE1 = "org.springframework.data.neo4j.examples.friends";
	private static final String PACKAGE_FOR_DOMAIN_OF_INSTANCE2 = "org.springframework.data.neo4j.examples.restaurants";

	private static final String QUERY_COUNT_PERSON_NODES = "MATCH (n:Person) RETURN count(n) as cnt";
	private static final String QUERY_COUNT_DINER_NODES = "MATCH (n:Diner) RETURN count(n) as cnt";

	private static final boolean neo4jOgm324Plus = ReflectionUtils.findMethod(Neo4jSession.class, "determineLabelsOrTypeForLoading", Class.class) != null;

	private static ServerControls instance1;

	private static ServerControls instance2;

	@Autowired @Qualifier(BEAN_NAME_FRIENDS_SESSION_FACTORY) private Session friendsSession;

	@Autowired private FriendshipRepository friendshipRepository;

	@Autowired @Qualifier(BEAN_NAME_RESTAURANTS_SESSION_FACTORY) private Session restaurantsSession;

	@Autowired private RestaurantRepository restaurantRepository;

	@BeforeClass
	public static void initializeDatabase() {

		instance1 = TestServerBuilders.newInProcessBuilder().newServer();
		instance2 = TestServerBuilders.newInProcessBuilder().newServer();
	}

	@Test // DATAGRAPH-1094
	public void sessionsShouldUseCorrectMappingContext() {

		assumeThat(neo4jOgm324Plus)
				.withFailMessage("This tests is only valid for Neo4j-OGM prior to 3.2.4")
				.isFalse();

		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> friendsSession.loadAll(Restaurant.class));
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> restaurantsSession.loadAll(Person.class));
	}

	@Test // DATAGRAPH-1094 DATAGRAPH-1274
	public void sessionsShouldUseCorrectMappingContext324() {

		assumeThat(neo4jOgm324Plus)
				.withFailMessage("This tests is only valid for Neo4j-OGM 3.2.4+")
				.isTrue();

		assertThat(friendsSession.loadAll(Restaurant.class)).isEmpty();
		assertThat(restaurantsSession.loadAll(Person.class)).isEmpty();

		assertThat(loggerRule.getFormattedMessages())
				.anyMatch(s -> s.contains("Unable to find database label for entity org.springframework.data.neo4j.examples.restaurants.domain.Restaurant : no results will be returned."))
				.anyMatch(s -> s.contains("Unable to find database label for entity org.springframework.data.neo4j.examples.friends.domain.Person : no results will be returned."));
	}

	@Test // DATAGRAPH-1094
	public void repositoriesShouldUseCorrectSessionFactory() {

		this.friendshipRepository.save(buildFriendship());
		this.restaurantRepository.save(openRestaurant());

		assertThat(this.friendsSession.loadAll(Person.class)).hasSize(2);
		assertThat(this.restaurantsSession.loadAll(Diner.class)).hasSize(2);

		assertThat(executeCountQuery(QUERY_COUNT_PERSON_NODES, instance1.graph())).isEqualTo(2L);
		assertThat(executeCountQuery(QUERY_COUNT_PERSON_NODES, instance2.graph())).isEqualTo(0L);
		assertThat(executeCountQuery(QUERY_COUNT_DINER_NODES, instance1.graph())).isEqualTo(0L);
		assertThat(executeCountQuery(QUERY_COUNT_DINER_NODES, instance2.graph())).isEqualTo(2L);
	}

	@AfterClass
	public static void tearDownDatabase() {
		instance1.close();
		instance2.close();
	}

	private Friendship buildFriendship() {

		Person person1 = new Person();
		person1.setFirstName("Homer");
		person1.setLastName("Simpson");

		Person person2 = new Person();
		person2.setFirstName("Lenny");
		person2.setLastName("Leonard");

		return new Friendship(person1, person2);
	}

	private Restaurant openRestaurant() {

		Restaurant restaurant = new Restaurant("The Restaurant at the End of the Universe", "A nice place.");
		restaurant.addRegularDiner(new Diner());
		restaurant.addRegularDiner(new Diner());
		return restaurant;
	}

	private long executeCountQuery(String countQuery, GraphDatabaseService onInstance) {

		try (Transaction tx = onInstance.beginTx()) {
			long result = (long) onInstance.execute(countQuery).next().get("cnt");
			tx.success();
			return result;
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class BaseConfiguration {}

	@Configuration
	@EnableNeo4jRepositories(sessionFactoryRef = BEAN_NAME_FRIENDS_SESSION_FACTORY,
			transactionManagerRef = BEAN_NAME_FRIENDS_TX_MANAGER, basePackages = PACKAGE_FOR_DOMAIN_OF_INSTANCE1)
	static class FriendsConfiguration {

		@Bean(BEAN_NAME_FRIENDS_TX_MANAGER)
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean(BEAN_NAME_FRIENDS_SESSION_FACTORY)
		public SessionFactory sessionFactory() {

			org.neo4j.ogm.config.Configuration ogmConfiguration = new org.neo4j.ogm.config.Configuration.Builder()
					.uri(instance1.boltURI().toString()).build();
			return new SessionFactory(ogmConfiguration, PACKAGE_FOR_DOMAIN_OF_INSTANCE1 + ".domain");
		}
	}

	@Configuration
	@EnableNeo4jRepositories(sessionFactoryRef = BEAN_NAME_RESTAURANTS_SESSION_FACTORY,
			transactionManagerRef = BEAN_NAME_RESTAURANTS_TX_MANAGER, basePackages = PACKAGE_FOR_DOMAIN_OF_INSTANCE2)
	static class RestaurantsConfiguration {

		@Bean(BEAN_NAME_RESTAURANTS_TX_MANAGER)
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean(BEAN_NAME_RESTAURANTS_SESSION_FACTORY)
		public SessionFactory sessionFactory() {

			org.neo4j.ogm.config.Configuration ogmConfiguration = new org.neo4j.ogm.config.Configuration.Builder()
					.uri(instance2.boltURI().toString()).build();
			return new SessionFactory(ogmConfiguration, PACKAGE_FOR_DOMAIN_OF_INSTANCE2 + ".domain");
		}
	}

	static class LoggerRule implements TestRule {

		private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		private final ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
				ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

		@Override
		public Statement apply(Statement base, Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					setup();
					base.evaluate();
					teardown();
				}
			};
		}

		private void setup() {
			logger.addAppender(listAppender);
			listAppender.start();
		}

		private void teardown() {
			listAppender.stop();
			listAppender.list.clear();
			logger.detachAppender(listAppender);
		}

		public List<String> getFormattedMessages() {
			return listAppender.list.stream().map(e -> e.getFormattedMessage()).collect(Collectors.toList());
		}

	}
}
