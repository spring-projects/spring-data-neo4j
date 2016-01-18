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
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repositories.context.RepositoriesTestContext;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.data.neo4j.repositories.repo.UserRepository;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.neo4j.ogm.testutil.GraphTestUtils.assertSameGraph;

/**
 * @author Michal Bachman
 */
@ContextConfiguration(classes = {RepositoriesTestContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RepositoryDefinitionTest extends MultiDriverTestClass {

    private GraphDatabaseService graphDatabaseService = getGraphDatabaseService();

    @Autowired
    private Session session;

    @Before
    public void init() {
        session.purgeDatabase();
    }

    @Autowired
    private MovieRepository movieRepository;

    @Test
    public void shouldProxyAndAutoImplementRepositoryDefinitionAnnotatedRepo() {
        Movie movie = new Movie("PF");
        movieRepository.save(movie);

        assertSameGraph(graphDatabaseService, "CREATE (m:Movie {title:'PF'})");

        assertEquals(1, IterableUtils.count(movieRepository.findAll()));
    }

}
