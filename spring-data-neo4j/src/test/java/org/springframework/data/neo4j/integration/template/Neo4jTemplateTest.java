
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.PersistenceException;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.Utils;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.data.neo4j.integration.movies.domain.Actor;
import org.springframework.data.neo4j.integration.movies.domain.Genre;
import org.springframework.data.neo4j.integration.movies.domain.Rating;
import org.springframework.data.neo4j.integration.movies.domain.TempMovie;
import org.springframework.data.neo4j.integration.movies.domain.User;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;

/**
 * @author Adam George
 */
public class Neo4jTemplateTest {

    @ClassRule
    public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule();

    private Neo4jOperations template;

    @Before
    public void setUpOgmSession() {
        SessionFactory sessionFactory = new SessionFactory("org.springframework.data.neo4j.integration.movies.domain");
        this.template = new Neo4jTemplate(sessionFactory.openSession(neo4jRule.baseNeoUrl()));
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
    @Ignore("Defect in Neo4jSession means this doesn't work yet")
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

}
