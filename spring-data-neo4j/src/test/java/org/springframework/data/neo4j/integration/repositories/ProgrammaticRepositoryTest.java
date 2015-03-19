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

package org.springframework.data.neo4j.integration.repositories;

import org.junit.Test;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.springframework.data.neo4j.integration.repositories.domain.Movie;
import org.springframework.data.neo4j.integration.repositories.repo.MovieRepository;
import org.springframework.data.neo4j.repository.support.GraphRepositoryFactory;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import static org.neo4j.ogm.testutil.GraphTestUtils.assertSameGraph;
import static org.junit.Assert.assertEquals;

/**
 * @author Michal Bachman
 */
public class ProgrammaticRepositoryTest extends WrappingServerIntegrationTest {

    private MovieRepository movieRepository;

    @Override
    protected int neoServerPort() {
        return 7879;
    }

    @Test
    public void canInstantiateRepositoryProgrammatically() {
        RepositoryFactorySupport factory = new GraphRepositoryFactory(
                new SessionFactory("org.springframework.data.neo4j.integration.repositories.domain").openSession(baseNeoUrl()));
        movieRepository = factory.getRepository(MovieRepository.class);

        Movie movie = new Movie("PF");
        movieRepository.save(movie);

        assertSameGraph(getDatabase(), "CREATE (m:Movie {title:'PF'})");

        assertEquals(1, IterableUtils.count(movieRepository.findAll()));
    }
}
