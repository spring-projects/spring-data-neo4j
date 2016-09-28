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

package org.springframework.data.neo4j.template;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Utils;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.examples.movies.domain.*;
import org.springframework.data.neo4j.template.context.Neo4jTemplateConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;
import static org.neo4j.ogm.session.Utils.map;

/**
 * @author Adam George
 * @author Luanne Misquitta
 * @author Vince Bickers
 */
@ContextConfiguration(classes = {Neo4jTemplateConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class Neo4jTemplateIT extends MultiDriverTestClass {

    private static GraphDatabaseService graphDatabaseService;

    @Autowired private Neo4jOperations template;

    @BeforeClass
    public static void beforeClass(){
        graphDatabaseService = getGraphDatabaseService();
    }

    @Before
    public void setUpOgmSession() {
        clearDatabase();
        addArbitraryDataToDatabase();
    }

    public void clearDatabase() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
            tx.success();
        }
    }

    /**
     * While this may seem trivial, some of these tests actually used to fail when run against a database containing unrelated data.
     */
    private void addArbitraryDataToDatabase() {

        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node arbitraryNode = graphDatabaseService.createNode(DynamicLabel.label("NotAClass"));
            arbitraryNode.setProperty("name", "Colin");
            Node otherNode = graphDatabaseService.createNode(DynamicLabel.label("NotAClass"));
            otherNode.setProperty("age", 39);
            arbitraryNode.createRelationshipTo(otherNode, DynamicRelationshipType.withName("TEST"));

            tx.success();
        }
    }

    @Test
    @Transactional
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
    @Transactional
    public void shouldSaveAndRetrieveRelationshipEntitiesWithoutExplicitTransactionManagement() {
        User critic = new User("Gary");
        TempMovie film = new TempMovie("Fast and Furious XVII");
        Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");

        this.template.save(filmRating);

        Rating loadedRating = this.template.load(Rating.class, filmRating.getId());
        assertNotNull("The loaded rating shouldn't be null", loadedRating);
        assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
        assertEquals("The rated film wasn't saved correctly", film.getName(), loadedRating.getMovie().getName());
        assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());
    }

    @Test
    @Transactional
    public void shouldExecuteArbitraryReadQuery() {
        User user = new User("Harmanpreet Singh");
        TempMovie bollywood = new TempMovie("Desi Boyz");
        TempMovie hollywood = new TempMovie("Mission Impossible");
        template.save(user.rate(bollywood, 1, "Bakwaas"));
        template.save(user.rate(hollywood, 4, "Pretty good"));

        Result queryResults =
                this.template.query("MATCH (u:User)-[r]->(m:Movie) RETURN AVG(r.stars) AS avg", Collections.EMPTY_MAP);
        Iterator<Map<String, Object>> queryResultIterator = queryResults.iterator();
        assertTrue("There should've been some query result returned", queryResultIterator.hasNext());
        assertEquals(2.5, (Double) queryResultIterator.next().get("avg"), 0.01);
    }

    @Test
    @Transactional
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
    @Transactional
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
    @Transactional
    public void shouldRetrieveEntitiesByMatchingProperty() {
        this.template.save(new Genre("Thriller"));
        this.template.save(new Genre("Horror"));
        this.template.save(new Genre("Period Drama"));

        Genre loadedGenre = this.template.loadByProperty(Genre.class, "name", "Horror");
        assertNotNull("No genre was loaded", loadedGenre);
        assertEquals("Horror", loadedGenre.getName());
    }

	/**
     * @see DATAGRAPH-685
     */
    @Test
    @Transactional
    public void shouldRetrieveEntitiesByMatchingPropertyAndDepth() {
        User user = new User("Harmanpreet Singh");
        TempMovie bollywood = new TempMovie("Desi Boyz");
        TempMovie hollywood = new TempMovie("Mission Impossible");


        template.save(user.rate(bollywood, 1, "Bakwaas"));
        template.save(user.rate(hollywood, 4, "Pretty good"));

        template.clear();

        User u = template.loadByProperty(User.class, "name", "Harmanpreet Singh",0);
        assertEquals(0,u.getRatings().size());

        u = template.loadByProperty(User.class, "name", "Harmanpreet Singh",2);
        assertEquals(2,u.getRatings().size());
        assertNotNull(u.getRatings().iterator().next().getMovie().getRatings());
    }

    /**
     * @see DATAGRAPH-685
     */
    @Test
    public void shouldRetrieveAllEntitiesByMatchingPropertyAndDepth() {
        saveUsers();

        Collection<TempMovie> m = template.loadAllByProperty(TempMovie.class, "name", "Desi Boyz",0);
        assertEquals(2,m.size());
        assertEquals(0, m.iterator().next().getRatings().size());

        m = template.loadAllByProperty(TempMovie.class, "name", "Desi Boyz",1);
        assertEquals(2,m.size());
        assertEquals(1, m.iterator().next().getRatings().size());
    }

    @Transactional
    public void saveUsers() {
        User user = new User("Harmanpreet Singh");
        TempMovie bollywood = new TempMovie("Desi Boyz");
        TempMovie hollywood = new TempMovie("Desi Boyz");
        template.save(user.rate(bollywood, 1, "Bakwaas"));
        template.save(user.rate(hollywood, 4, "Pretty good"));
    }


    /**
     * @see DATAGRAPH-685
     */
    @Test
    public void shouldRetrieveEntitiesByMatchingPropertiesAndDepth() {
        saveUsers2();


        Filter nameFilter = new Filter("name","Harmanpreet Singh");
        Filter middleNameFilter = new Filter("middleName","A");
        middleNameFilter.setBooleanOperator(BooleanOperator.AND);
        Filters filters = new Filters();
        filters.add(nameFilter, middleNameFilter);

        User u = template.loadByProperties(User.class,filters,0);
        assertEquals(0,u.getRatings().size());

        u = template.loadByProperties(User.class, filters,2);
        assertEquals(2,u.getRatings().size());
        assertNotNull(u.getRatings().iterator().next().getMovie().getRatings());
    }

    @Transactional
    public void saveUsers2() {
        User user = new User("Harmanpreet Singh");
        user.setMiddleName("A");
        User user2 = new User("Harmanpreet Singh");
        user2.setMiddleName("B");
        TempMovie bollywood = new TempMovie("Desi Boyz");
        TempMovie hollywood = new TempMovie("Mission Impossible");
        template.save(user.rate(bollywood, 1, "Bakwaas"));
        template.save(user.rate(hollywood, 4, "Pretty good"));
        template.save(user2);
    }

    /**
     * @see DATAGRAPH-685
     */
    @Test
    public void shouldRetrieveAllEntitiesByMatchingPropertiesAndDepth() {
        saveUsers3();


        Filter nameFilter = new Filter("name","Harmanpreet Singh");
        Filter middleNameFilter = new Filter("middleName","A");
        middleNameFilter.setBooleanOperator(BooleanOperator.AND);
        Filters filters = new Filters();
        filters.add(nameFilter, middleNameFilter);

        Collection<User> u = template.loadAllByProperties(User.class,filters,0);
        assertEquals(2,u.size());
        assertEquals(0,u.iterator().next().getRatings().size());

        u = template.loadAllByProperties(User.class, filters,2);
        assertEquals(2,u.size());
        assertNotNull(u.iterator().next().getRatings().iterator().next().getMovie().getRatings());
    }

    @Transactional
    public void saveUsers3() {
        User user = new User("Harmanpreet Singh");
        user.setMiddleName("A");
        User user2 = new User("Harmanpreet Singh");
        user2.setMiddleName("A");
        TempMovie bollywood = new TempMovie("Desi Boyz");
        TempMovie hollywood = new TempMovie("Mission Impossible");
        template.save(user.rate(bollywood, 1, "Bakwaas"));
        template.save(user.rate(hollywood, 4, "Pretty good"));
        template.save(user2);
    }

    @Test
    public void shouldExecuteArbitraryUpdateQuery() {
        assertTrue("There shouldn't be any genres in the database", this.template.loadAll(Genre.class).isEmpty());

        this.template.query("CREATE (:Genre {name:'Comedy'}), (:Genre {name:'Action'})", Collections.EMPTY_MAP);

        Iterator<Genre> genres = this.template.loadAll(Genre.class, 0).iterator();
        assertEquals("There weren't any genres created", 2, Utils.size(genres));
    }

    /**
     * @see DATAGRAPH-607
     */
    @Test(expected = java.lang.RuntimeException.class)
    @Ignore // review 2.0
    public void shouldThrowExceptionForExecuteQueryThatReturnsResults() {
        this.template.query("CREATE (g1:Genre {name:'Comedy'}), (g2:Genre {name:'Action'}) return g1", Collections.EMPTY_MAP);
    }

    @Test
    public void shouldCountNumberOfEntitiesOfParticularTypeInGraphDatabase() {
        GraphDatabaseService database = graphDatabaseService;
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
        Long genreId = graphDatabaseService.execute("CREATE (t:Genre {name:'Thriller'}), (r:Genre {name:'RomCom'}) RETURN id(r) AS gid")
                .<Long>columnAs("gid").next();

        Genre entity = this.template.load(Genre.class, genreId);
        assertEquals("RomCom", entity.getName());
        this.template.delete(entity);

        Collection<Genre> allGenres = this.template.loadAll(Genre.class, 0);
        assertEquals("The genre wasn't deleted", 1, allGenres.size());
        assertEquals("The wrong genre was deleted", "Thriller", allGenres.iterator().next().getName());
    }

    /**
     * @see DATAGRAPH-738
     */
    @Test(expected = InvalidDataAccessApiUsageException.class)
    public void shouldConvertOGMExceptionsToPersistenceExceptions() {
        this.template.loadAll(Rating.class, 0);
    }

    /**
     * @see DATAGRAPH-604, DATAGRAPH-738
     */
    @Test(expected = Exception.class)
    public void shouldHandleErrorsOnExecute() {
        this.template.query("CREAT (node:NODE)", Collections.EMPTY_MAP);
    }

    /**
     * @see DATAGRAPH-607
     */
    @Test
    public void shouldReturnQueryStats() {
        QueryStatistics stats = this.template.query("CREATE (a:Actor {name:'Keanu Reeves'}) CREATE (m:Movie {title:'The Matrix'}) " +
                "CREATE (a)-[:ACTED_IN {role:'Neo'}]->(m)", Collections.EMPTY_MAP).queryStatistics();

        assertEquals(2, stats.getNodesCreated());
        assertEquals(3, stats.getPropertiesSet());
        assertEquals(1, stats.getRelationshipsCreated());
        assertEquals(2, stats.getLabelsAdded());

        stats = this.template.query("MATCH (a:Actor)-->(m:Movie) REMOVE a:Actor SET m.title=null", Collections.EMPTY_MAP).queryStatistics();
        assertTrue(stats.containsUpdates());
        assertEquals(1, stats.getLabelsRemoved());
        assertEquals(1, stats.getPropertiesSet());

        stats = this.template.query("MATCH (n)-[r]-(m:Movie) delete n,r,m",Collections.EMPTY_MAP).queryStatistics();
        assertTrue(stats.containsUpdates());
        assertEquals(2, stats.getNodesDeleted());
        assertEquals(1, stats.getRelationshipsDeleted());
    }

    /**
     * @see DATAGRAPH-607
     */
    @Test
    public void shouldReturnSchemaQueryStats() {
        QueryStatistics stats = this.template.query("CREATE INDEX ON :Actor(name)", Collections.EMPTY_MAP).queryStatistics();
        assertEquals(1, stats.getIndexesAdded());

        stats = this.template.query("CREATE CONSTRAINT ON (movie:Movie) ASSERT movie.title IS UNIQUE", Collections.EMPTY_MAP).queryStatistics();
        assertEquals(1, stats.getConstraintsAdded());

        stats = this.template.query("DROP CONSTRAINT ON (movie:Movie) ASSERT movie.title is UNIQUE", Collections.EMPTY_MAP).queryStatistics();
        assertEquals(1, stats.getConstraintsRemoved());

        stats = this.template.query("DROP INDEX ON :Actor(name)", Collections.EMPTY_MAP).queryStatistics();
        assertEquals(1, stats.getIndexesRemoved());
    }

    /**
     * @see DATAGRAPH-607
     */
    @Test
    public void shouldReturnQueryStatsForQueryWithParams() {
        QueryStatistics stats = this.template.query("CREATE (a:Actor {name:{actorName}}) CREATE (m:Movie {title:{movieTitle}}) " +
                "CREATE (a)-[:ACTED_IN {role:'Neo'}]->(m)", map("actorName", "Keanu Reeves", "movieTitle", "THe Matrix")).queryStatistics();
        assertTrue(stats.containsUpdates());
        assertEquals(2, stats.getNodesCreated());
        assertEquals(3, stats.getPropertiesSet());
        assertEquals(1, stats.getRelationshipsCreated());
        assertEquals(2, stats.getLabelsAdded());

        stats = this.template.query("MATCH (a:Actor)-->(m:Movie) REMOVE a:Actor SET m.title=null", Collections.EMPTY_MAP).queryStatistics();
        assertTrue(stats.containsUpdates());
        assertEquals(1, stats.getLabelsRemoved());
        assertEquals(1, stats.getPropertiesSet());

        stats = this.template.query("MATCH (n)-[r]-(m:Movie) delete n,r,m", Collections.EMPTY_MAP).queryStatistics();
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

    /**
     * @see DATAGRAPH-697
     */
    @Test
    public void shouldAllowResultsToBeReturnedFromModifyingQueries() {
        Result results = this.template.query(
                "CREATE (a:Actor {name:{actorName}}) CREATE (m:Movie {title:{movieTitle}}) " +
                "CREATE (a)-[:ACTED_IN {role:'Neo'}]->(m) return a.name as actorName, m.title as movieName", map("actorName", "Keanu Reeves", "movieTitle", "The Matrix"));

        QueryStatistics stats = results.queryStatistics();
        assertEquals(2, stats.getNodesCreated());
        assertEquals(3, stats.getPropertiesSet());
        assertEquals(1, stats.getRelationshipsCreated());
        assertEquals(2, stats.getLabelsAdded());

        Iterable<Map<String,Object>> iterableResults = results.queryResults();
        assertNotNull(iterableResults);
        for(Map<String,Object> row : iterableResults) {
            assertEquals("Keanu Reeves",row.get("actorName"));
            assertEquals("The Matrix", row.get("movieName"));
        }
    }

    /**
     * @see DATAGRAPH-863
     */
    @Test
    public void shouldAllowDeletionOfNodeEntityAgainstEmptyDatabase() {
        try {
            clearDatabase();
            this.template.deleteAll(Movie.class);
        } catch (Exception e) {
            fail("Should not have thrown exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * @see DATAGRAPH-863
     */
    @Test
    public void shouldAllowDeletionOfRelationshipEntityAgainstEmptyDatabase() {
        try {
            clearDatabase();
            this.template.deleteAll(Rating.class);
        } catch (Exception e) {
            fail("Should not have thrown exception: " + e.getLocalizedMessage());
        }
    }
}
