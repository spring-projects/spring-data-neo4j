package org.neo4j.ogm.defects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.ogm.domain.cineasts.annotated.Actor;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;

@Ignore
public class SessionCypherQueryTest extends WrappingServerIntegrationTest {

    private Session session;

    @Before
    public void setUpSession() {
        SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.domain.cineasts.annotated");
        session = sessionFactory.openSession(baseNeoUrl());
    }

    /**
     * NB: A similar test for Neo4jTemplate is present in Neo4jTemplateTest and should be reactivated once this is fixed.
     */
    @Test
    public void shouldQueryForSpecificObjectUsingBespokeParameterisedCypherQuery() {
        this.session.save(new Actor("Alec Baldwin"));
        this.session.save(new Actor("Helen Mirren"));
        this.session.save(new Actor("Matt Damon"));

//        setUpSession(); // including this makes the test pass
        // FIXME: this loads any actor at random despite Neo4j always returning the correct one in t'JSON!
        Actor loadedActor = this.session.queryForObject(Actor.class, "MATCH (a:Actor) WHERE a.name={param} RETURN a",
                Collections.singletonMap("param", "Alec Baldwin"));
        assertNotNull("The entity wasn't loaded", loadedActor);
        assertEquals("Alec Baldwin", loadedActor.getName());
    }

    /**
     * NB: A similar test for Neo4jTemplate is present in Neo4jTemplateTest and should be reactivated once this is fixed.
     */
    @Test
    public void shouldQueryForObjectCollectionUsingBespokeCypherQuery() {
        this.session.save(new Actor("Jeff"));
        this.session.save(new Actor("John"));
        this.session.save(new Actor("Colin"));

//        setUpSession(); // including this makes the test pass
        // FIXME: this keeps loading Colin!
        Iterable<Actor> actors = this.session.query(Actor.class, "MATCH (a:Actor) WHERE a.name=~'J.*' RETURN a",
                Collections.<String, Object>emptyMap());
        assertNotNull("The entities weren't loaded", actors);
        assertTrue("The entity wasn't loaded", actors.iterator().hasNext());
        for (Actor actor : actors) {
            assertTrue("Shouldn't've loaded " + actor.getName(),
                    actor.getName().equals("John") || actor.getName().equals("Jeff"));
        }
    }

    @Test
    public void shouldQueryForObjectByIdUsingBespokeParameterisedCypherQuery() {
        Actor alec = new Actor("Alec Baldwin");
        this.session.save(alec);
        this.session.save(new Actor("Helen Mirren"));
        this.session.save(new Actor("Matt Damon"));

        setUpSession(); // including this makes the test pass
        // FIXME: this loads any actor at random despite Neo4j always returning the correct one in t'JSON!
        Actor loadedActor = this.session.queryForObject(Actor.class, "MATCH (a:Actor) WHERE ID(a)={0} RETURN a",
                Collections.<String, Object>singletonMap("param", alec));
        assertNotNull("The entity wasn't loaded", loadedActor);
        assertEquals("Alec Baldwin", loadedActor.getName());
    }

}
