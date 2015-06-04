
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

package org.springframework.data.neo4j.integration.template;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.*;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.Utils;
import org.neo4j.ogm.session.result.QueryStatistics;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.data.neo4j.integration.movies.domain.*;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;

import javax.persistence.PersistenceException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;
import static org.neo4j.ogm.session.Utils.map;

/**
 * @author Adam George
 * @author Luanne Misquitta
 */
public class Neo4jTemplateTest {

    @ClassRule
    public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule();

    private Neo4jOperations template;

    @Before
    public void setUpOgmSession() {
        SessionFactory sessionFactory = new SessionFactory("org.springframework.data.neo4j.integration.movies.domain");
        this.template = new Neo4jTemplate(sessionFactory.openSession(neo4jRule.url()));
        addArbitraryDataToDatabase();
    }

    @After
    public void clearDatabase() {
        neo4jRule.clearDatabase();
    }

    /**
     * While this may seem trivial, some of these tests actually used to fail when run against a database containing unrelated data.
     */
    private void addArbitraryDataToDatabase() {
        try (Transaction tx = neo4jRule.getGraphDatabaseService().beginTx()) {
            Node arbitraryNode = neo4jRule.getGraphDatabaseService().createNode(DynamicLabel.label("NotAClass"));
            arbitraryNode.setProperty("name", "Colin");
            Node otherNode = neo4jRule.getGraphDatabaseService().createNode(DynamicLabel.label("NotAClass"));
            otherNode.setProperty("age", 39);
            arbitraryNode.createRelationshipTo(otherNode, DynamicRelationshipType.withName("TEST"));

            tx.success();
        }
    }

    @Test
    public void shouldSaveAndRetrieveNodeEntitiesWithoutExplicitTransactionManagement() {
        Genre filmGenre = new Genre();
        filmGenre.setName("Comedy");
        this.template.save(filmGenre);

        Genre loadedGenre = this.template.load(Genre.class, filmGenre.getId());
        assertNotNull("The entity loaded from the template shouldn't be null", loadedGenre);
        assertEquals("The loaded entity wasn't as expected", filmGenre, loadedGenre);

        Genre anotherGenre = new Genre();
        anotherGenre.setName("Action");
        this.template.save(anotherGenre);

        Collection<Genre> allGenres = this.template.loadAll(Genre.class);
        assertNotNull("The collection of all genres shouldn't be null", allGenres);
        assertEquals("The number of genres in the database wasn't as expected", 2, allGenres.size());
    }

    @Test
    public void shouldSaveAndRetrieveRelationshipEntitiesWithoutExplicitTransactionManagement() {
        User critic = new User("Gary");
        TempMovie film = new TempMovie("Fast and Furious XVII");
        Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");

        this.template.save(filmRating);

        Rating loadedRating = this.template.load(Rating.class, filmRating.getId());
        assertNotNull("The loaded rating shouldn't be null", loadedRating);
        assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
        assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
        assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());
    }

    @Test
    public void shouldExecuteArbitraryReadQuery() {
        User user = new User("Harmanpreet Singh");
        TempMovie bollywood = new TempMovie("Desi Boyz");
        TempMovie hollywood = new TempMovie("Mission Impossible");
        template.save(user.rate(bollywood, 1, "Bakwaas"));
        template.save(user.rate(hollywood, 4, "Pretty good"));

        Iterable<Map<String, Object>> queryResults =
                this.template.query("MATCH (u:User)-[r]->(m:Movie) RETURN AVG(r.stars) AS avg", Collections.<String, Object>emptyMap());
        Iterator<Map<String, Object>> queryResultIterator = queryResults.iterator();
        assertTrue("There should've been some query result returned", queryResultIterator.hasNext());
        assertEquals(2.5, (Double) queryResultIterator.next().get("avg"), 0.01);
    }

    @Test
    public void shouldQueryForSpecificObjectUsingBespokeParameterisedCypherQuery() {
        this.template.save(new Actor("ab", "Alec Baldwin"));
        this.template.save(new Actor("hm", "Helen Mirren"));
        this.template.save(new Actor("md", "Matt Damon"));

        Actor loadedActor = this.template.queryForObject(Actor.class, "MATCH (a:Actor) WHERE a.name={param} RETURN a",
                Collections.singletonMap("param", "Alec Baldwin"));
        assertNotNull("The entity wasn't loaded", loadedActor);
        assertEquals("Alec Baldwin", loadedActor.getName());
    }

    @Test
    public void shouldQueryForObjectCollectionUsingBespokeCypherQuery() {
        this.template.save(new User("Jeff"));
        this.template.save(new User("John"));
        this.template.save(new User("Colin"));

        Iterable<User> users = this.template.queryForObjects(User.class, "MATCH (u:User) WHERE u.name=~'J.*' RETURN u",
                Collections.<String, Object>emptyMap());
        assertNotNull("The entity wasn't loaded", users);
        assertTrue("The entity wasn't loaded", users.iterator().hasNext());
        for (User loadedUser : users) {
            assertTrue("Shouldn't've loaded " + loadedUser.getName(),
                    loadedUser.getName().equals("John") || loadedUser.getName().equals("Jeff"));
        }
    }

    @Test
    public void shouldRetrieveEntitiesByMatchingProperty() {
        this.template.save(new Genre("Thriller"));
        this.template.save(new Genre("Horror"));
        this.template.save(new Genre("Period Drama"));

        Genre loadedGenre = this.template.loadByProperty(Genre.class, "name", "Horror");
        assertNotNull("No genre was loaded", loadedGenre);
        assertEquals("Horror", loadedGenre.getName());
    }

    @Test
    public void shouldExecuteArbitraryUpdateQuery() {
        assertTrue("There shouldn't be any genres in the database", this.template.loadAll(Genre.class).isEmpty());

        this.template.execute("CREATE (:Genre {name:'Comedy'}), (:Genre {name:'Action'})");

        Iterator<Genre> genres = this.template.loadAll(Genre.class, 0).iterator();
        assertEquals("There weren't any genres created", 2, Utils.size(genres));
    }

    /**
     * @see DATAGRAPH-607
     */
    @Test(expected = java.lang.RuntimeException.class)
    public void shouldThrowExeceptionForExecuteQueryThatReturnsResults() {
        this.template.execute("CREATE (g1:Genre {name:'Comedy'}), (g2:Genre {name:'Action'}) return g1");
    }

    @Test
    public void shouldCountNumberOfEntitiesOfParticularTypeInGraphDatabase() {
        GraphDatabaseService database = neo4jRule.getGraphDatabaseService();
        try (Transaction tx = database.beginTx()) {
            // create test entities where label matches simple name
            Label genreTypeLabel = DynamicLabel.label(Genre.class.getSimpleName());
            for (int i = 0; i < 5; i++) {
                database.createNode(genreTypeLabel);
            }

            // create test entities where label's controlled by annotations
            Label filmTypeLabel = DynamicLabel.label(TempMovie.class.getAnnotation(NodeEntity.class).label());
            for (int i = 0; i < 3; i++) {
                database.createNode(filmTypeLabel);
            }

            // throw in a User to check it doesn't confuse things
            database.createNode(DynamicLabel.label(User.class.getSimpleName()));

            tx.success();
        }

        assertEquals(5, this.template.count(Genre.class));
        assertEquals(3, this.template.count(TempMovie.class));
    }

    @Test
    public void shouldDeleteExistingEntitiesByGraphId() {
        ExecutionEngine ee = new ExecutionEngine(neo4jRule.getGraphDatabaseService());
        Long genreId = ee.execute("CREATE (t:Genre {name:'Thriller'}), (r:Genre {name:'RomCom'}) RETURN id(r) AS gid")
                .<Long>columnAs("gid").next();

        Genre entity = this.template.load(Genre.class, genreId);
        assertEquals("RomCom", entity.getName());
        this.template.delete(entity);

        Collection<Genre> allGenres = this.template.loadAll(Genre.class, 0);
        assertEquals("The genre wasn't deleted", 1, allGenres.size());
        assertEquals("The wrong genre was deleted", "Thriller", allGenres.iterator().next().getName());
    }

    @Test(expected = PersistenceException.class)
    public void shouldConvertOGMExceptionsToPersistenceExceptions() {
        this.template.loadAll(Void.class);
    }

    /**
     * @see DATAGRAPH-604
     */
    @Test(expected = PersistenceException.class)
    public void shouldHandleErrorsOnExecute() {
        this.template.execute("CREAT (node:NODE)");
    }

    /**
     * @see DATAGRAPH-607
     */
    @Test
    public void shouldReturnQueryStats() {
        QueryStatistics stats = this.template.execute("CREATE (a:Actor {name:'Keanu Reeves'}) CREATE (m:Movie {title:'The Matrix'}) " +
                "CREATE (a)-[:ACTED_IN {role:'Neo'}]->(m)");
        assertTrue(stats.containsUpdates());
        assertEquals(2, stats.getNodesCreated());
        assertEquals(3, stats.getPropertiesSet());
        assertEquals(1, stats.getRelationshipsCreated());
        assertEquals(2, stats.getLabelsAdded());

        stats = this.template.execute("MATCH (a:Actor)-->(m:Movie) REMOVE a:Actor SET m.title=null");
        assertTrue(stats.containsUpdates());
        assertEquals(1, stats.getLabelsRemoved());
        assertEquals(1, stats.getPropertiesSet());

        stats = this.template.execute("MATCH n-[r]-(m:Movie) delete n,r,m");
        assertTrue(stats.containsUpdates());
        assertEquals(2, stats.getNodesDeleted());
        assertEquals(1, stats.getRelationshipsDeleted());
    }

    /**
     * @see DATAGRAPH-607
     */
    @Test
    public void shouldReturnSchemaQueryStats() {
        QueryStatistics stats = this.template.execute("CREATE INDEX ON :Actor(name)");
        assertEquals(1, stats.getIndexesAdded());

        stats = this.template.execute("CREATE CONSTRAINT ON (movie:Movie) ASSERT movie.title IS UNIQUE");
        assertEquals(1, stats.getConstraintsAdded());

        stats = this.template.execute("DROP CONSTRAINT ON (movie:Movie) ASSERT movie.title is UNIQUE");
        assertEquals(1, stats.getConstraintsRemoved());

        stats = this.template.execute("DROP INDEX ON :Actor(name)");
        assertEquals(1, stats.getIndexesRemoved());
    }

    /**
     * @see DATAGRAPH-607
     */
    @Test
    public void shouldReturnQueryStatsForQueryWithParams() {
        QueryStatistics stats = this.template.execute("CREATE (a:Actor {name:{actorName}}) CREATE (m:Movie {title:{movieTitle}}) " +
                "CREATE (a)-[:ACTED_IN {role:'Neo'}]->(m)",map("actorName","Keanu Reeves", "movieTitle","THe Matrix"));
        assertTrue(stats.containsUpdates());
        assertEquals(2, stats.getNodesCreated());
        assertEquals(3, stats.getPropertiesSet());
        assertEquals(1, stats.getRelationshipsCreated());
        assertEquals(2, stats.getLabelsAdded());

        stats = this.template.execute("MATCH (a:Actor)-->(m:Movie) REMOVE a:Actor SET m.title=null");
        assertTrue(stats.containsUpdates());
        assertEquals(1, stats.getLabelsRemoved());
        assertEquals(1, stats.getPropertiesSet());

        stats = this.template.execute("MATCH n-[r]-(m:Movie) delete n,r,m");
        assertTrue(stats.containsUpdates());
        assertEquals(2, stats.getNodesDeleted());
        assertEquals(1, stats.getRelationshipsDeleted());
    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void shouldRetrieveEntitiesByMatchingProperties() {
        this.template.save(new Cinema("Ritzy", 5000));
        this.template.save(new Cinema("Picturehouse", 7500));

        Filter name = new Filter("name", "Ritzy");
        Cinema loadedCinema = this.template.loadByProperties(Cinema.class, new Filters().add(name));
        assertNotNull("No cinema was loaded", loadedCinema);
        assertEquals("Ritzy", loadedCinema.getName());

        Filter capacity = new Filter("capacity", 1000);
        capacity.setComparisonOperator(ComparisonOperator.GREATER_THAN);
        Collection<Cinema> loadedCinemas = this.template.loadAllByProperties(Cinema.class, new Filters().add(capacity));
        assertNotNull(loadedCinemas);
        assertEquals(2, loadedCinemas.size());
        assertTrue(loadedCinemas.contains(new Cinema("Ritzy", 5000)));
        assertTrue(loadedCinemas.contains(new Cinema("Picturehouse", 7500)));
    }

}
