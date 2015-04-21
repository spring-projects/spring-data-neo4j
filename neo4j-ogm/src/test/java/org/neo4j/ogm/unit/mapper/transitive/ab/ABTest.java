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

package org.neo4j.ogm.unit.mapper.transitive.ab;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vince Bickers
 */
public class ABTest extends InMemoryServerTest {


    private A a;
    private B b;
    private R r;

    private static SessionFactory sessionFactory;

    @Before
    public void init() throws IOException {
        sessionFactory = new SessionFactory("org.neo4j.ogm.unit.mapper.transitive.ab");
        session = sessionFactory.openSession(neo4jRule.baseNeoUrl());
        setUpEntityModel();
    }

    private void setUpEntityModel() {

        a = new A();
        b = new B();
        r = new R();

        r.a = a;
        r.b = b;

        a.r = r;
        b.r = r;

    }

    @Test
    public void shouldFindBFromA() {

        session.save(b);

        a = session.load(A.class, a.id);

        assertEquals(b, a.r.b);

    }

    @Test
    public void shouldFindAFromB() {

        session.save(a);

        b = session.load(B.class, b.id);

        assertEquals(a, b.r.a);

    }

    @Test
    public void shouldReflectRemovalA() {

        session.save(a);

        // given that we remove the relationship

        b.r = null;
        a.r = null;

        session.save(b);

        // when we reload a
        a = session.load(A.class, a.id);

        // expect the relationship to have gone.
        assertNull(a.r);

    }


    @NodeEntity(label="A")
    public static class A extends E {
        @Relationship(type="EDGE", direction = Relationship.OUTGOING)
        R r;
    }

    @NodeEntity(label="B")
    public static class B extends E {
        @Relationship(type="EDGE", direction = Relationship.INCOMING)
        R r;
    }

    @RelationshipEntity(type="EDGE")
    public static class R {

        Long id;

        @StartNode
        A a;
        @EndNode
        B b;

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + a.id + "->" + b.id;
        }

    }

    /**
     * Can be used as the basic class at the root of any entity for these tests,
     * provides the mandatory id field, a unique ref, a simple to-string method
     * and equals/hashcode implementation.
     *
     * Note that without an equals/hashcode implementation, reloading
     * an object which already has a collection of items in it
     * will result in the collection items being added again, because
     * of the behaviour of the ogm merge function when handling
     * arrays and iterables.
     */
    public abstract static class E {

        public Long id;
        public String key;

        public E() {
            this.key = UUID.randomUUID().toString();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":" + id + ":" + key;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            return (key.equals(((E)o).key));
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

}
