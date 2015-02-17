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

public class ProgrammaticRepositoryTest extends WrappingServerIntegrationTest {

    private MovieRepository movieRepository;

    @Override
    protected int neoServerPort() {
        return 7879;
    }

    @Test
    public void canInstantiateRepositoryProgrammatically() {
        RepositoryFactorySupport factory = new GraphRepositoryFactory(new SessionFactory("org.springframework.data.neo4j.integration.repositories.domain").openSession("http://localhost:7879"));
        movieRepository = factory.getRepository(MovieRepository.class);

        Movie movie = new Movie("PF");
        movieRepository.save(movie);

        assertSameGraph(getDatabase(), "CREATE (m:Movie {title:'PF'})");

        assertEquals(1, IterableUtils.count(movieRepository.findAll()));
    }
}
