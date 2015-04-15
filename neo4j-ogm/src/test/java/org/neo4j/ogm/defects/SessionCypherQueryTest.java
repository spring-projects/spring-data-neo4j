/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.defects;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.cineasts.annotated.Actor;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Adam George
 * @author Vince Bickers
 */
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
        this.session.save(new Actor("Helen Mirren"));
        Actor alec = new Actor("Alec Baldwin");
        this.session.save(alec);
        this.session.save(new Actor("Matt Damon"));

        Actor loadedActor = this.session.queryForObject(Actor.class, "MATCH (a:Actor) WHERE ID(a)={param} RETURN a",
                Collections.<String, Object>singletonMap("param", alec.getId()));
        assertNotNull("The entity wasn't loaded", loadedActor);
        assertEquals("Alec Baldwin", loadedActor.getName());
    }

}
