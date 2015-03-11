package org.neo4j.cineasts.integration;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.PersistenceContext;
import org.neo4j.cineasts.domain.Actor;
import org.neo4j.cineasts.domain.Director;
import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.repository.ActorRepository;
import org.neo4j.cineasts.repository.DirectorRepository;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.UserRepository;
import org.neo4j.cineasts.service.Neo4jDatabaseCleaner;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = {PersistenceContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DatabasePopulationTest extends WrappingServerIntegrationTest {

    @Autowired
    ActorRepository actorRepository;

    @Autowired
    DirectorRepository directorRepository;

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    Neo4jDatabaseCleaner cleaner;

    @Override
    protected int neoServerPort() {
        return PersistenceContext.NEO4J_PORT;
    }

    @Test
    public void databaseShouldBeCleared() {

        Actor tomHanks = new Actor("1", "Tom Hanks");
        tomHanks = actorRepository.save(tomHanks);

        Movie forrest = new Movie("1", "Forrest Gump");
        forrest = movieRepository.save(forrest);

        Director robert = new Director("1", "Robert Zemeckis");
        robert.directed(forrest);
        robert = directorRepository.save(robert);

        cleaner.cleanDb();

    }

}
