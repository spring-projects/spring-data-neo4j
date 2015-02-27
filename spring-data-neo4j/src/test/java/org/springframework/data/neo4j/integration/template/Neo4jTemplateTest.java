package org.springframework.data.neo4j.integration.template;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.Utils;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.springframework.data.neo4j.integration.movies.domain.*;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;

public class Neo4jTemplateTest extends WrappingServerIntegrationTest {

    private Neo4jOperations template;

    @Before
    public void setUpOgmSession() {
        SessionFactory sessionFactory = new SessionFactory("org.springframework.data.neo4j.integration.movies.domain");
        this.template = new Neo4jTemplate(sessionFactory.openSession(baseNeoUrl()));
        addArbitraryDataToDatabase();
    }

    /**
     * While this may seem trivial, some of these tests actually used to fail when run against a database containing unrelated data.
     */
    private void addArbitraryDataToDatabase() {
        try (Transaction tx = getDatabase().beginTx()) {
            Node arbitraryNode = getDatabase().createNode(DynamicLabel.label("NotAClass"));
            arbitraryNode.setProperty("name", "Colin");
            Node otherNode = getDatabase().createNode(DynamicLabel.label("NotAClass"));
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
    public void shouldExecuteArbitraryUpdateQuery() {
        // TODO: update section 6.3 of "Good Relationships" now we've added support for this
        assertTrue("There shouldn't be any genres in the database", this.template.loadAll(Genre.class).isEmpty());

        this.template.execute("CREATE (:Genre {name:'Comedy'}), (:Genre {name:'Action'})");

        Iterator<Genre> genres = this.template.loadAll(Genre.class, 0).iterator();
        assertEquals("There weren't any genres created", 2, Utils.size(genres));
    }

    @Test
    public void shouldCountNumberOfEntitiesOfParticularTypeInGraphDatabase() {
        try (Transaction tx = getDatabase().beginTx()) {
            // create test entities where label matches simple name
            Label genreTypeLabel = DynamicLabel.label(Genre.class.getSimpleName());
            for (int i = 0; i < 5; i++) {
                getDatabase().createNode(genreTypeLabel);
            }

            // create test entities where label's controlled by annotations
            Label filmTypeLabel = DynamicLabel.label(TempMovie.class.getAnnotation(NodeEntity.class).label());
            for (int i = 0; i < 3; i++) {
                getDatabase().createNode(filmTypeLabel);
            }

            // throw in a User to check it doesn't confuse things
            getDatabase().createNode(DynamicLabel.label(User.class.getSimpleName()));

            tx.success();
        }

        assertEquals(5, this.template.count(Genre.class));
        assertEquals(3, this.template.count(TempMovie.class));
    }

}
