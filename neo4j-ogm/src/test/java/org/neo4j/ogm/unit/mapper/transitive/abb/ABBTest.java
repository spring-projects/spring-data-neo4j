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

package org.neo4j.ogm.unit.mapper.transitive.abb;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.*;
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
    private R r1, r2;

    private static SessionFactory sessionFactory;

    @Before
    public void init() throws IOException {
        setUp();
        sessionFactory = new SessionFactory("org.neo4j.ogm.unit.mapper.transitive.abb");
        session = sessionFactory.openSession("http://localhost:" + neoPort);
        setUpEntityModel();
    }

    private void setUpEntityModel() {

        a = new A();

        b1 = new B();
        b2 = new B();

        r1 = new R();
        r2 = new R();

        r1.a = a;
        r1.b = b1;

        r2.a = a;
        r2.b = b2;

        a.r = new R[] { r1, r2 };
        b1.r = r1;
        b2.r = r2;

    }

    @Test
    public void shouldFindBFromA() {

        session.save(b1);




        a = session.load(A.class, a.id);

        assertEquals(2, a.r.length);
        assertSameArray(new B[]{a.r[0].b, a.r[1].b}, new B[]{b1, b2});

    }

    @Test
    public void shouldFindAFromB() {

        session.save(a);

        b1 = session.load(B.class, b1.id);
        b2 = session.load(B.class, b2.id);

        assertEquals(a, b1.r.a);
        assertEquals(a, b2.r.a);

    }

    @Test
    public void shouldReflectRemovalA() {

        session.save(a);

        // local model must be self-consistent
        b1.r = null;
        a.r = new R[] { r2 };

        session.save(b1);

        // when we reload a
        a = session.load(A.class, a.id);

        // expect the b1 relationship to have gone.
        assertEquals(1, a.r.length);
        assertSameArray(new B[] {b2}, new B[] { a.r[0].b } );

    }

    @Test
    public void shouldBeAbleToAddNewB() {

        session.save(a);

        B b3 = new B();
        R r3 = new R();

        r3.a = a;
        r3.b = b3;
        b3.r = r3;
        a.r = new R[] { r1, r2, r3 };

        // fully connected graph, should be able to save any object
        session.save(a);

        // try others?

        b3 = session.load(B.class, b3.id);

        assertSameArray(new A[]{ a }, new A[] { b3.r.a });

    }

    @Test
    public void shouldBeAbleToAddNewR() {

        session.save(a);

        B b3 = new B();
        R r3 = new R();

        r3.a = a;
        r3.b = b3;
        b3.r = r3;
        a.r = new R[] { r1, r2, r3 };

        // fully connected graph, should be able to save any object
        session.save(r3);

        b3 = session.load(B.class, b3.id);

        assertSameArray(new A[] { a }, new A[] { b3.r.a });
        assertSameArray(new R[] { r1, r2, r3 }, a.r);
        assertSameArray(new B[] { b1, b2, b3 }, new B[] { a.r[0].b, a.r[1].b, a.r[2].b });

    }

    @NodeEntity(label="A")
    public static class A extends E {
        @Relationship(type="EDGE", direction = Relationship.OUTGOING)
        R[] r;
    }

    @NodeEntity(label="B")
    public static class B extends E {
        @Relationship(type="EDGE", direction = Relationship.INCOMING)
        R r;
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



    @RelationshipEntity(type="EDGE")
    public static class R {

        Long id;

        @StartNode
        A a;
        @EndNode
        B b;

        public String toString() {
            return this.getClass().getSimpleName() + ":" + a.id + "->" + b.id;
        }

    }

}
