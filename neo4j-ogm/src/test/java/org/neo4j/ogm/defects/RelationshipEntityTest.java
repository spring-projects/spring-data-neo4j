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
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author: Vince Bickers
 */
public class RelationshipEntityTest extends WrappingServerIntegrationTest {


    private U u;
    private M m;
    private R r1;

    private Session session;

    @Before
    public void setUpSession() {
        SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.defects");
        session = sessionFactory.openSession(baseNeoUrl());

        // setup domain
        u = new U("Luanne");
        m = new M("Taken");
        r1 = new R(u, m, "great!", 4);

        u.rset.add(r1);
        m.rset.add(r1);
    }


    @Test
    public void shouldAddR() {

        session.save(u);

        R r2 = new R(u, m, "even better next time!", 5);
        u.rset.add(r2);
        m.rset.add(r2);

        session.save(u);

        m = session.load(M.class, m.id);

        assertEquals(2, m.rset.size());
    }

    @Test
    public void shouldUpdateExistingR() {

        session.save(u);

        r1.stars=3;

        session.save(u);

        m = session.load(M.class, m.id);

        assertEquals(1, m.rset.size());
        assertEquals(3, m.rset.iterator().next().stars.intValue());
    }


    @Test
    public void shouldDeleteR() {

        session.save(u);

        u.rset.clear();
        m.rset.clear();

        session.save(u);

        m = session.load(M.class, m.id);
        u = session.load(U.class, u.id);

        assertEquals(0, m.rset.size());
        assertEquals(0, u.rset.size());

    }

    @Test
    public void shouldReplaceExistingR() {

        session.save(u);

        R r3 = new R(u, m, "Only Gravity sucks more than this film", 0);

        u.rset.clear();
        u.rset.add(r3);

        m.rset.clear();
        m.rset.add(r3);

        session.save(u);

        m = session.load(M.class, m.id);

        assertEquals(1, m.rset.size());
        assertEquals(0, m.rset.iterator().next().stars.intValue());
    }

    @Test
    public void shouldDirectlyAddR() {

        session.save(r1);
        session.clear();
        r1 = session.load(R.class, r1.id);

        assertEquals(1, r1.m.rset.size());
        assertEquals(1, r1.u.rset.size());
    }

    @Test
    public void shouldDirectlyUpdateR() {

        session.save(r1);
        r1 = session.load(R.class, r1.id);
        r1.stars = 5;
        session.save(r1);
        session.clear();
        assertEquals(1, r1.m.rset.size());
        assertEquals(1, r1.u.rset.size());
        assertEquals(Integer.valueOf(5), r1.stars);

        u = session.load(U.class,u.id);
        assertEquals(1, u.rset.size());
        m = session.load(M.class,m.id);
        assertEquals(1,m.rset.size());
        assertEquals(Integer.valueOf(5),m.rset.iterator().next().stars);
    }

    @Test
    public void shouldDirectlyDeleteR() {
        session.save(r1);

        r1 = session.load(R.class, r1.id);
        u = session.load(U.class,u.id);
        m = session.load(M.class,m.id);
        u.rset.clear();
        m.rset.clear();
        session.delete(r1);

        assertNull(session.load(R.class, r1.id));

        m = session.load(M.class,m.id);
        assertNotNull(m);
        assertEquals(0, m.rset.size());

        u = session.load(U.class,u.id);
        assertNotNull(u);
        assertEquals(0,u.rset.size());
    }


    @NodeEntity(label="U")
    public static class U {
        Long id;
        String name;

        public U() {}

        public U(String name) {
            this.name = name;
        }

        @Relationship(type="EDGE", direction = Relationship.OUTGOING)
        Set<R> rset = new HashSet<>();
    }

    @RelationshipEntity(type="EDGE")
    public static class R {

        Long id;
        String comments;
        Integer stars;

        @StartNode
        U u;

        @EndNode
        M m;

        public R() {}

        public R(U u, M m, String comments, Integer stars) {
            this.u = u;
            this.m = m;
            this.comments = comments;
            this.stars = stars;
        }

    }

    @NodeEntity(label="M")
    public static class M {

        Long id;
        String title;

        public M () {}

        public M (String title) {
            this.title = title;
        }

        @Relationship(type="EDGE", direction= Relationship.INCOMING)
        Set<R> rset = new HashSet<>();
    }
}
