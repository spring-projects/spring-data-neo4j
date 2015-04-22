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

package org.neo4j.ogm.unit.mapper.direct.abb;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.unit.mapper.direct.RelationshipTest;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Vince Bickers
 */
public class ABBTest extends RelationshipTest {


    private A a;
    private B b1, b2;

    private static SessionFactory sessionFactory;

    @Before
    public void init() throws IOException {
        sessionFactory = new SessionFactory("org.neo4j.ogm.unit.mapper.direct.abb");
        session = sessionFactory.openSession(neo4jRule.baseNeoUrl());
        setUpEntityModel();
    }

    private void setUpEntityModel() {
        a = new A();
        b1 = new B();
        b2 = new B();

        a.b = new B[] { b1, b2 };
        b1.a = a;
        b2.a = a;

    }

    @Test
    public void shouldFindAFromB() {

        session.save(a);

        b1 = session.load(B.class, b1.id);
        b2 = session.load(B.class, b2.id);

        assertEquals(a, b1.a);
        assertEquals(a, b2.a);

    }

    @Test
    public void shouldFindBFromA() {

        session.save(b1);
        session.save(b2);

        a = session.load(A.class, a.id);

        assertSameArray(new B[]{b1, b2}, a.b);


    }

    @Test
    public void shouldReflectRemovalA() {

        session.save(a);

        // local model must be self-consistent
        b1.a = null;
        a.b = new B[] { b2 };

        session.save(b1);
        session.save(b2);

        // when we reload a
        a = session.load(A.class, a.id);

        // expect the b1 relationship to have gone.
        assertSameArray(new B[]{b2}, a.b);

    }

    @Test
    public void shouldBeAbleToAddAnotherB() {
        session.save(a);

        B b3 = new B();
        b3.a = a;
        a.b = new B[] { b1, b2, b3 };

        // fully connected graph, should be able to save any object
        session.save(b3);

        a = session.load(A.class, a.id);

        assertSameArray(new B[]{ b1, b2, b3 }, a.b);

    }

    @NodeEntity(label="A")
    public static class A extends E {

        @Relationship(type="EDGE", direction= Relationship.OUTGOING)
        B[] b;
    }

    @NodeEntity(label="B")
    public static class B extends E {

        @Relationship(type="EDGE", direction= Relationship.INCOMING)
        A a;
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
