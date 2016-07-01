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

package org.springframework.data.neo4j.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repositories.domain.User;
import org.springframework.data.neo4j.repositories.repo.PersistenceContextInTheSamePackage;
import org.springframework.data.neo4j.repositories.repo.UserRepository;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.neo4j.ogm.testutil.GraphTestUtils.assertSameGraph;

/**
 * @author Michal Bachman
 */
@ContextConfiguration(classes = {PersistenceContextInTheSamePackage.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RepoScanningTest extends MultiDriverTestClass {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Neo4jOperations neo4jOperations;

    private GraphDatabaseService graphDatabaseService = getGraphDatabaseService();

    @Before
    public void clearDatabase() {
        graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
        neo4jOperations.clear();
    }

    @Test
    public void enableNeo4jRepositoriesShouldScanSelfPackageByDefault() {
        User user = new User("Michal");
        userRepository.save(user);

        assertSameGraph(graphDatabaseService, "CREATE (u:User {name:'Michal'})");
    }

}
