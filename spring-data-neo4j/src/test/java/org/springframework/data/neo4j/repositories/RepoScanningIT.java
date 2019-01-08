/*
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.repositories;

import static org.neo4j.ogm.testutil.GraphTestUtils.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repositories.domain.User;
import org.springframework.data.neo4j.repositories.repo.PersistenceContextInTheSamePackage;
import org.springframework.data.neo4j.repositories.repo.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 */
@ContextConfiguration(classes = { PersistenceContextInTheSamePackage.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class RepoScanningIT extends MultiDriverTestClass {

	private static GraphDatabaseService graphDatabaseService;

	@Autowired private UserRepository userRepository;

	@BeforeClass
	public static void beforeClass() {
		graphDatabaseService = getGraphDatabaseService();
	}

	@Before
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void enableNeo4jRepositoriesShouldScanSelfPackageByDefault() {

		User user = new User("Michal");
		userRepository.save(user);

		assertSameGraph(graphDatabaseService, "CREATE (u:User {name:'Michal'})");
	}

}
