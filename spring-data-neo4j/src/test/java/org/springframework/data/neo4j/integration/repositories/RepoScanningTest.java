/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc.", "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.repositories;

import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.repositories.repo.PersistenceContextInTheSamePackage;
import org.springframework.data.neo4j.integration.repositories.domain.User;
import org.springframework.data.neo4j.integration.repositories.repo.UserRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.neo4j.ogm.testutil.GraphTestUtils.assertSameGraph;

/**
 * @author Michal Bachman
 */
@ContextConfiguration(classes = {PersistenceContextInTheSamePackage.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RepoScanningTest extends WrappingServerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected int neoServerPort() {
        return 7879;
    }

    @Test
    public void enableNeo4jRepositoriesShouldScanSelfPackageByDefault() {
        User user = new User("Michal");
        userRepository.save(user);

        assertSameGraph(getDatabase(), "CREATE (u:User {name:'Michal'})");
    }
}
