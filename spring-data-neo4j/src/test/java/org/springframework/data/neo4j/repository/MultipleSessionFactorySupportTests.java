/*
 * Copyright (c) 2018 "Neo4j, Inc." / "Pivotal Software, Inc."
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
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

/**
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { MultipleSessionFactorySupportTests.BaseConfiguration.class,
		MultipleSessionFactorySupportTests.FriendsConfiguration.class,
		MultipleSessionFactorySupportTests.RestaurantsConfiguration.class })
@RunWith(SpringRunner.class)
public class MultipleSessionFactorySupportTests {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultipleSessionFactorySupportTests.class);

	private static final String BEAN_NAME_FRIENDS_SESSION_FACTORY = "sessionFactoryFriends";
	private static final String BEAN_NAME_FRIENDS_TX_MANAGER = "txManagerFriends";

	private static final String BEAN_NAME_RESTAURANTS_SESSION_FACTORY = "sessionFactoryRestaurants";
	private static final String BEAN_NAME_RESTAURANTS_TX_MANAGER = "txManagerRestaurants";

	private static final String PACKAGE_FOR_DOMAIN_OF_INSTANCE1 = "org.springframework.data.neo4j.examples.friends";
	private static final String PACKAGE_FOR_DOMAIN_OF_INSTANCE2 = "org.springframework.data.neo4j.examples.restaurants";

	private static final String QUERY_COUNT_PERSON_NODES = "MATCH (n:Person) RETURN count(n) as cnt";
	private static final String QUERY_COUNT_DINER_NODES = "MATCH (n:Diner) RETURN count(n) as cnt";

	private static DatabaseHolder instance1;

	private static DatabaseHolder instance2;

	@Autowired @Qualifier(BEAN_NAME_FRIENDS_SESSION_FACTORY) private Session friendsSession;

	@Autowired private FriendshipRepository friendshipRepository;

	@Autowired @Qualifier(BEAN_NAME_RESTAURANTS_SESSION_FACTORY) private Session restaurantsSession;

	@Autowired private RestaurantRepository restaurantRepository;

	@BeforeClass
	public static void initializeDatabase() throws IOException {

		instance1 = createAndStartEmbeddedInstance();
		instance2 = createAndStartEmbeddedInstance();
	}

	@Test // DATAGRAPH-1094
	public void sessionsShouldUseCorrectMappingContext() {

		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> friendsSession.loadAll(Restaurant.class));
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> restaurantsSession.loadAll(Person.class));
	}

	@Test // DATAGRAPH-1094
	public void repositoriesShouldUseCorrectSessionFactory() {

		this.friendshipRepository.save(buildFriendship());
		this.restaurantRepository.save(openRestaurant());

		assertThat(this.friendsSession.loadAll(Person.class)).hasSize(2);
		assertThat(this.restaurantsSession.loadAll(Diner.class)).hasSize(2);

		assertThat(executeCountQuery(QUERY_COUNT_PERSON_NODES, instance1.graphDatabaseService)).isEqualTo(2L);
		assertThat(executeCountQuery(QUERY_COUNT_PERSON_NODES, instance2.graphDatabaseService)).isEqualTo(0L);
		assertThat(executeCountQuery(QUERY_COUNT_DINER_NODES, instance1.graphDatabaseService)).isEqualTo(0L);
		assertThat(executeCountQuery(QUERY_COUNT_DINER_NODES, instance2.graphDatabaseService)).isEqualTo(2L);
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

		try (Transaction ignored = onInstance.beginTx()) {
			return (long) onInstance.execute(countQuery).next().get("cnt");
		}
	}

	private static DatabaseHolder createAndStartEmbeddedInstance() throws IOException {

		File dataStore1 = Files.createTempDirectory("neo4j.db").toFile();
		GraphDatabaseService graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dataStore1)
				.newGraphDatabase();
		return new DatabaseHolder(dataStore1, graphDatabaseService);
	}

	/**
	 * Just keep database and directory together.
	 */
	static class DatabaseHolder {

		final File dataStoreDirectory;

		final GraphDatabaseService graphDatabaseService;

		public DatabaseHolder(File dataStoreDirectory, GraphDatabaseService graphDatabaseService) {

			this.dataStoreDirectory = dataStoreDirectory;
			this.graphDatabaseService = graphDatabaseService;
		}

		void close() {

			this.graphDatabaseService.shutdown();
			try {
				FileUtils.deleteDirectory(dataStoreDirectory);
			} catch (IOException e) {
				LOGGER.warn("Failed to delete database store in {}" + dataStoreDirectory.getAbsolutePath());
			}
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
			return new SessionFactory(new EmbeddedDriver(instance1.graphDatabaseService),
					PACKAGE_FOR_DOMAIN_OF_INSTANCE1 + ".domain");
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
			return new SessionFactory(new EmbeddedDriver(instance2.graphDatabaseService),
					PACKAGE_FOR_DOMAIN_OF_INSTANCE2 + ".domain");
		}
	}
}
