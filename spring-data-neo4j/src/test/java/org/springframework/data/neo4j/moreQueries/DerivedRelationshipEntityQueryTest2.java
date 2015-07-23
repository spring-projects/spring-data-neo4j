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

package org.springframework.data.neo4j.moreQueries;

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
import org.springframework.data.neo4j.moreQueries.domain.Rating;
import org.springframework.data.neo4j.moreQueries.context.MoviesContext;
import org.springframework.data.neo4j.moreQueries.repo.RatingRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DerivedRelationshipEntityQueryTest2 {

	@ClassRule
	public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule(7879);

	private static Session session;

	@Autowired
	private RatingRepository ratingRepository;

	@Before
	public void init() throws IOException {
		session = new SessionFactory("org.springframework.data.neo4j.moreQueries.domain").openSession(neo4jRule.url());
	}

	@After
	public void clearDatabase() {
		neo4jRule.clearDatabase();
	}

	private void executeUpdate(String cypher) {
		new ExecutionEngine(neo4jRule.getGraphDatabaseService()).execute(cypher);
	}

	@Test
	public void shouldFindRelEntitiesWithBothStartEndNestedSamePropertyNames() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {name:'The Matrix'}) CREATE (m:Movie {name:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");
		List<Rating> ratings = ratingRepository.findByUserNameAndMovieName("Michal", "Speed");
		assertEquals(1, ratings.size());
		assertEquals("Michal", ratings.get(0).user.name);
		assertEquals("Speed", ratings.get(0).movie.name);

		ratings = ratingRepository.findByUserNameAndMovieName("Michal", "Chocolat");
		assertEquals(0, ratings.size());
	}

	@Test
	public void shouldFindRelEntitiesWithNestedProperty() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {name:'The Matrix'}) CREATE (m:Movie {name:'Chocolat'})" +
				" CREATE (u:User {name:'Michal', level:5}) CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2)");
		List<Rating> ratings = ratingRepository.findByUserLevel("5");
		assertEquals(2, ratings.size());

		ratings = ratingRepository.findByUserLevel("2");
		assertEquals(0, ratings.size());
	}
}
