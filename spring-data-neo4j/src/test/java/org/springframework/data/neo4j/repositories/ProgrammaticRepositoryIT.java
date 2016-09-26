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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.domain.User;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.data.neo4j.repositories.repo.UserRepository;
import org.springframework.data.neo4j.repository.support.GraphRepositoryFactory;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import static org.junit.Assert.assertEquals;
import static org.neo4j.ogm.testutil.GraphTestUtils.assertSameGraph;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
public class ProgrammaticRepositoryIT extends MultiDriverTestClass {

    private static GraphDatabaseService graphDatabaseService;
    private MovieRepository movieRepository;
    private SessionFactory sessionFactory = new SessionFactory("org.springframework.data.neo4j.repositories.domain");
    private Session session;
    private Neo4jOperations neo4jOperations;

    @BeforeClass
    public static void beforeClass(){
        graphDatabaseService = getGraphDatabaseService();
    }

    @Before
    public void init() {
        session = sessionFactory.openSession();
        neo4jOperations = new Neo4jTemplate(session);
        session.purgeDatabase();
    }

    @Test
    public void canInstantiateRepositoryProgrammatically() {

        RepositoryFactorySupport factory = new GraphRepositoryFactory(session, neo4jOperations);

        movieRepository = factory.getRepository(MovieRepository.class);

        Movie movie = new Movie("PF");
        movieRepository.save(movie);

        assertSameGraph(graphDatabaseService, "CREATE (m:Movie {title:'PF'})");

        assertEquals(1, IterableUtils.count(movieRepository.findAll()));
    }

	/**
     * @see DATAGRAPH-847
     */
    @Test
    public void shouldBeAbleToDeleteAllViaRepository() {

        RepositoryFactorySupport factory = new GraphRepositoryFactory(session, neo4jOperations);

        UserRepository userRepository = factory.getRepository(UserRepository.class);

        User userA = new User("A");
        User userB = new User("B");
        userRepository.save(userA);
        userRepository.save(userB);

        assertEquals(2, userRepository.count());

        userRepository.deleteAll();
        assertEquals(0, userRepository.count());
    }

}
