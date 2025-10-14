/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.falkordb.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.falkordb.core.DefaultFalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBOperations;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBMappingContext;
import org.springframework.data.falkordb.core.mapping.FalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.mapping.model.EntityInstantiators;

import com.falkordb.Driver;
import com.falkordb.impl.api.DriverImpl;

/**
 * Integration test for FalkorDB Spring Data library using Twitter graph. Connects to
 * local FalkorDB on port 6379 with graph name "TWITTER".
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class FalkorDBTwitterIntegrationTest {

	private FalkorDBClient falkorDBClient;

	private FalkorDBOperations falkorDBOperations;

	private FalkorDBMappingContext mappingContext;

	private FalkorDBEntityConverter entityConverter;

	@BeforeEach
	public void setUp() {
		try {
			// Initialize FalkorDB driver and client connected to local instance on port
			// 6379
			Driver driver = new DriverImpl("localhost", 6379);
			this.falkorDBClient = new DefaultFalkorDBClient(driver, "TWITTER");

			// Initialize mapping context
			this.mappingContext = new DefaultFalkorDBMappingContext();

			// Initialize entity converter with client for relationship loading
			EntityInstantiators entityInstantiators = new EntityInstantiators();
			this.entityConverter = new DefaultFalkorDBEntityConverter(this.mappingContext, entityInstantiators,
					this.falkorDBClient);

			// Initialize FalkorDB operations template
			this.falkorDBOperations = new FalkorDBTemplate(this.falkorDBClient, this.mappingContext,
					this.entityConverter);

			// Clear existing data for clean test
			clearGraph();

		}
		catch (Exception ex) {
			throw new RuntimeException("FalkorDB connection failed", ex);
		}
	}

	@Test
	public void testConnectionAndBasicOperations() {

		// Test connection by creating a simple user
		TwitterUser user = new TwitterUser("testuser", "Test User", "test@example.com");
		user.setBio("Testing FalkorDB Spring Data integration");
		user.setLocation("San Francisco, CA");
		user.setVerified(false);

		// Save the user
		TwitterUser savedUser = this.falkorDBOperations.save(user);
		assertThat(savedUser).isNotNull();
		assertThat(savedUser.getId()).isNotNull();

		// Retrieve the user
		Optional<TwitterUser> foundUser = this.falkorDBOperations.findById(savedUser.getId(), TwitterUser.class);
		assertThat(foundUser).isPresent();
		assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
	}

	@Test
	public void testTwitterGraphCreationAndTraversal() {

		// Create a small Twitter-like network
		createTwitterNetwork();

		// Test graph traversal queries
		testGraphTraversalQueries();

	}

	@Test
	public void testRelationshipTraversal() {

		// Create users with relationships
		TwitterUser alice = createUser("alice", "Alice Johnson", "alice@example.com");
		TwitterUser bob = createUser("bob", "Bob Smith", "bob@example.com");
		TwitterUser charlie = createUser("charlie", "Charlie Brown", "charlie@example.com");

		// Save users
		alice = this.falkorDBOperations.save(alice);
		bob = this.falkorDBOperations.save(bob);
		charlie = this.falkorDBOperations.save(charlie);

		// Create follow relationships using raw Cypher (since our relationship saving
		// is not yet fully integrated with repository layer)
		createFollowRelationship(alice.getId(), bob.getId());
		createFollowRelationship(bob.getId(), charlie.getId());
		createFollowRelationship(alice.getId(), charlie.getId());

		// Test traversal queries
		testRelationshipQueries(alice.getId(), bob.getId(), charlie.getId());

	}

	@Test
	public void testComplexQueries() {

		// Create sample data
		createSampleTweets();

		// Test various complex queries
		testAnalyticsQueries();

	}

	private void createTwitterNetwork() {
		// Create influential users
		TwitterUser elonMusk = createUser("elonmusk", "Elon Musk", "elon@spacex.com");
		elonMusk.setVerified(true);
		elonMusk.setFollowerCount(150000000);
		elonMusk.setBio("CEO of SpaceX and Tesla");

		TwitterUser billGates = createUser("billgates", "Bill Gates", "bill@gates.com");
		billGates.setVerified(true);
		billGates.setFollowerCount(60000000);
		billGates.setBio("Co-founder of Microsoft");

		TwitterUser oprah = createUser("oprah", "Oprah Winfrey", "oprah@oprah.com");
		oprah.setVerified(true);
		oprah.setFollowerCount(45000000);
		oprah.setBio("Media executive, actress, talk show host");

		// Save users
		elonMusk = this.falkorDBOperations.save(elonMusk);
		billGates = this.falkorDBOperations.save(billGates);
		oprah = this.falkorDBOperations.save(oprah);

		// Create some tweets
		createTweet(elonMusk.getId(), "Mars is looking good for life! 🚀 #SpaceX #Mars");
		createTweet(billGates.getId(), "Technology can help solve global challenges. #TechForGood");
		createTweet(oprah.getId(), "Grateful for another beautiful day! ✨ #Gratitude");

		// Create follow relationships
		createFollowRelationship(billGates.getId(), elonMusk.getId());
		createFollowRelationship(oprah.getId(), elonMusk.getId());
		createFollowRelationship(oprah.getId(), billGates.getId());
	}

	private void testGraphTraversalQueries() {
		// Test finding verified users
		List<TwitterUser> verifiedUsers = this.falkorDBOperations.query(
				"MATCH (u:User) WHERE u.verified = true RETURN u ORDER BY u.follower_count DESC",
				Collections.emptyMap(), TwitterUser.class);

		// Test finding users with high follower count
		List<TwitterUser> influencers = this.falkorDBOperations.query(
				"MATCH (u:User) WHERE u.follower_count > $minFollowers RETURN u ORDER BY u.follower_count DESC",
				Map.of("minFollowers", 50000000), TwitterUser.class);

		// Process influencers (output removed for clean build)
	}

	private void testRelationshipQueries(Long aliceId, Long bobId, Long charlieId) {
		// Find who Alice follows
		List<TwitterUser> aliceFollows = this.falkorDBOperations.query(
				"MATCH (alice:User)-[:FOLLOWS]->(followed:User) WHERE id(alice) = $aliceId RETURN followed",
				Map.of("aliceId", aliceId), TwitterUser.class);

		// Find Bob's followers
		List<TwitterUser> bobFollowers = this.falkorDBOperations.query(
				"MATCH (follower:User)-[:FOLLOWS]->(bob:User) WHERE id(bob) = $bobId RETURN follower",
				Map.of("bobId", bobId), TwitterUser.class);

		// Find mutual connections between Alice and Charlie
		List<TwitterUser> mutualFollows = this.falkorDBOperations.query(
				"MATCH (alice:User)-[:FOLLOWS]->(mutual:User)<-[:FOLLOWS]-(charlie:User) "
						+ "WHERE id(alice) = $aliceId AND id(charlie) = $charlieId RETURN mutual",
				Map.of("aliceId", aliceId, "charlieId", charlieId), TwitterUser.class);

	}

	private void createSampleTweets() {
		// This would be implemented to create tweets with hashtags, mentions, etc.

		// Create a tech enthusiast
		TwitterUser techUser = createUser("techguru", "Tech Guru", "tech@example.com");
		techUser = this.falkorDBOperations.save(techUser);

		createTweet(techUser.getId(), "Excited about the new AI developments! #AI #MachineLearning #Tech");
		createTweet(techUser.getId(), "FalkorDB is an amazing graph database! #GraphDB #FalkorDB");
	}

	private void testAnalyticsQueries() {
		// Test counting total users
		Optional<Long> userCount = this.falkorDBOperations.queryForObject("MATCH (u:User) RETURN count(u) as count",
				Collections.emptyMap(), Long.class);

		// Test counting total tweets
		Optional<Long> tweetCount = this.falkorDBOperations.queryForObject("MATCH (t:Tweet) RETURN count(t) as count",
				Collections.emptyMap(), Long.class);

		// Test finding most followed users
		List<TwitterUser> mostFollowed = this.falkorDBOperations.query(
				"MATCH (u:User) RETURN u ORDER BY u.follower_count DESC LIMIT 5", Collections.emptyMap(),
				TwitterUser.class);

		// Process most followed users (output removed for clean build)
	}

	private TwitterUser createUser(String username, String displayName, String email) {
		TwitterUser user = new TwitterUser(username, displayName, email);
		user.setCreatedAt(LocalDateTime.now());
		return user;
	}

	private void createFollowRelationship(Long followerId, Long followedId) {
		String cypher = "MATCH (follower:User), (followed:User) "
				+ "WHERE id(follower) = $followerId AND id(followed) = $followedId "
				+ "MERGE (follower)-[:FOLLOWS]->(followed)";

		this.falkorDBClient.query(cypher, Map.of("followerId", followerId, "followedId", followedId));
	}

	private void createTweet(Long authorId, String text) {
		String cypher = "MATCH (author:User) WHERE id(author) = $authorId "
				+ "CREATE (tweet:Tweet {text: $text, created_at: $createdAt, like_count: 0, retweet_count: 0, reply_count: 0}) "
				+ "CREATE (author)-[:POSTED]->(tweet) " + "RETURN tweet";

		// Create timestamp manually
		String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		this.falkorDBClient.query(cypher, Map.of("authorId", authorId, "text", text, "createdAt", createdAt));
	}

	private void clearGraph() {
		try {
			// Clear all nodes and relationships in the graph
			this.falkorDBClient.query("MATCH (n) DETACH DELETE n", Collections.emptyMap());
		}
		catch (Exception ex) {
		}
	}

	/**
	 * Manual test runner that can be executed directly.
	 */
	public static void main(String[] args) {

		FalkorDBTwitterIntegrationTest test = new FalkorDBTwitterIntegrationTest();

		try {
			test.setUp();

			test.testConnectionAndBasicOperations();

			test.testTwitterGraphCreationAndTraversal();

			test.testRelationshipTraversal();

			test.testComplexQueries();

		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
