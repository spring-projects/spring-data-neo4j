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

package org.springframework.data.neo4j.repositories;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repositories.context.RepositoriesTestContext;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.neo4j.ogm.testutil.GraphTestUtils.*;

/**
 * @author Michal Bachman
 */
@ContextConfiguration(classes = {RepositoriesTestContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RepositoryDefinitionTest {

    @Rule
    public final Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule(7879);

    @Autowired
    private MovieRepository movieRepository;

    @Test
    public void shouldProxyAndAutoImplementRepositoryDefinitionAnnotatedRepo() {
        Movie movie = new Movie("PF");
        movieRepository.save(movie);

        assertSameGraph(neo4jRule.getGraphDatabaseService(), "CREATE (m:Movie {title:'PF'})");

        assertEquals(1, IterableUtils.count(movieRepository.findAll()));
    }

}
