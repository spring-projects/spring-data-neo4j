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

package org.neo4j.ogm.unit.mapper.transitive.aabb;

import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.unit.mapper.direct.RelationshipTest;

/**
 * @author Vince Bickers
 */
public class AABBTest extends RelationshipTest {

    private static SessionFactory sessionFactory;

    private A a1, a2, a3;
    private B b1, b2, b3;
    private R r1, r2, r3, r4, r5, r6;

    @Before
    public void init() throws IOException {
        sessionFactory = new SessionFactory("org.neo4j.ogm.unit.mapper.transitive.aabb");
        session = sessionFactory.openSession(neo4jRule.baseNeoUrl());
        setUpEntityModel();
    }

    private void setUpEntityModel() {
        // three source nodes
        a1 = new A();
        a2 = new A();
        a3 = new A();

        // three target nodes
        b1 = new B();
        b2 = new B();
        b3 = new B();

        // six relationships
        r1 = new R(a1, b1);
        r2 = new R(a1, b2); //problem
        r3 = new R(a2, b1);
        r4 = new R(a2, b3);
        r5 = new R(a3, b2);
        r6 = new R(a3, b3);

        // assign relationships to both sides to ensure entity graph is fully connected
        a1.r = new R[] { r1, r2 };
        a2.r = new R[] { r3, r4 };
        a3.r = new R[] { r5, r6 };

        b1.r = new R[] { r1, r3 };
        b2.r = new R[] { r2, r5 };
        b3.r = new R[] { r4, r6 };
    }

    @Test
    public void shouldFindBFromA() {

        // because the graph is fully connected, we should be able to save any object to fully populate the graph
        session.save(b1);

        a1 = session.load(A.class, a1.id);
        a2 = session.load(A.class, a2.id);
        a3 = session.load(A.class, a3.id);

        assertSameArray(new B[] {a1.r[0].b, a1.r[1].b}, new B[] { b1, b2 });
        assertSameArray(new B[] {a2.r[0].b, a2.r[1].b}, new B[] { b1, b3 });
        assertSameArray(new B[] {a3.r[0].b, a3.r[1].b}, new B[] { b2, b3 });

    }

    @Test
    public void shouldFindAFromB() {

        // because the graph is fully connected, we should be able to save any object to fully populate the graph
        session.save(a1);

        b1 = session.load(B.class, b1.id);
        b2 = session.load(B.class, b2.id);
        b3 = session.load(B.class, b3.id);

        assertSameArray(new A[] {b1.r[0].a, b1.r[1].a}, new A[] { a1, a2 });
        assertSameArray(new A[] {b2.r[0].a, b2.r[1].a}, new A[] { a1, a3 });
        assertSameArray(new A[] {b3.r[0].a, b3.r[1].a}, new A[] { a2, a3 });

    }

    @Test
    public void shouldReflectRemovalA() {

        // because the graph is fully connected, we should be able to save any object to fully populate the graph
        session.save(a1);

        // it is programmer's responsibility to keep the domain entities synchronized
        b2.r = null;
        a1.r = new R[] { r1 };
        a3.r = new R[] { r3 };

        session.save(b2);

        // when we reload a1
        a1 = session.load(A.class, a1.id);
        // expect the b2 relationship to have gone.
        assertSameArray(new B[] { b1 }, new B[] { a1.r[0].b });


        // when we reload a3
        a3 = session.load(A.class, a3.id);
        // expect the b2 relationship to have gone.
        assertSameArray(new B[] { b3 }, new B[] { a3.r[0].b });


        // and when we reload a2
        a2 = session.load(A.class, a2.id);
        // expect its relationships to be intact.
        assertSameArray(new B[] { b1, b3 }, new B[] { a2.r[0].b, a2.r[1].b });

    }


    @Test
    @Ignore
    public void shouldHandleAddNewRelationshipBetweenASingleABPair() {
        // fully connected, will persist everything
        session.save(a1);

        R r7 = new R(a1, b1);

        a1.r = new R[] { r2, r7 };
        b1.r = new R[] { r3, r7 };

        session.save(a1);

        b1 = session.load(B.class, b1.id);

        assertSameArray(new R[] { r1, r3, r7}, b1.r);
        assertSameArray(new R[] { r1, r2, r7}, a1.r);

    }

    @NodeEntity(label="A")
    public static class A extends E {

        @Relationship(type="EDGE", direction = Relationship.OUTGOING)
        R[] r;

    }

    @NodeEntity(label="B")
    public static class B extends E {

        @Relationship(type="EDGE", direction = Relationship.INCOMING)
        R[] r;

    }

    @RelationshipEntity(type="EDGE")
    public static class R {

        Long id;

        @StartNode
        A a;
        @EndNode
        B b;

        public R(A a, B b) {
            this.a = a;
            this.b = b;
        }

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