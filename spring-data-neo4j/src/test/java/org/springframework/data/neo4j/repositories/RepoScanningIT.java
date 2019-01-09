/*
 * Copyright 2011-2019 the original author or authors.
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
