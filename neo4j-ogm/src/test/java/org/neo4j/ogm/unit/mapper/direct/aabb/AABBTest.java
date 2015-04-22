
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

package org.neo4j.ogm.unit.mapper.direct.aabb;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.unit.mapper.direct.RelationshipTest;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Vince Bickers
 */
public class AABBTest extends RelationshipTest {

    private A a1, a2, a3;
    private B b1, b2, b3;

    private static SessionFactory sessionFactory;

    @Before
    public void init() throws IOException {
        sessionFactory = new SessionFactory("org.neo4j.ogm.unit.mapper.direct.aabb");
        session = sessionFactory.openSession(neo4jRule.baseNeoUrl());
        setUpEntityModel();

    }

    private void setUpEntityModel() {
        a1 = new A();
        a2 = new A();
        a3 = new A();

        b1 = new B();
        b2 = new B();
        b3 = new B();

        a1.b = new B[] { b1, b2 };
        a2.b = new B[] { b1, b3 };
        a3.b = new B[] { b2, b3 };

        b1.a = new A[] { a1, a2 };
        b2.a = new A[] { a1, a3 };
        b3.a = new A[] { a2, a3 };
    }

    @Test
    public void shouldFindAFromB() {

        session.save(a1);
        session.save(a2);
        session.save(a3);

        b1 = session.load(B.class, b1.id);
        b2 = session.load(B.class, b2.id);
        b3 = session.load(B.class, b3.id);

        assertSameArray(new A[]{a1, a2}, b1.a);
        assertSameArray(new A[]{a1, a3}, b2.a);
        assertSameArray(new A[]{a2, a3}, b3.a);

    }

    @Test
    public void shouldFindBFromA() {

        session.save(b1);
        session.save(b2);
        session.save(b3);

        a1 = session.load(A.class, a1.id);
        a2 = session.load(A.class, a2.id);
        a3 = session.load(A.class, a3.id);

        assertSameArray(new B[] { b1, b2 }, a1.b);
        assertSameArray(new B[] { b1, b3 }, a2.b);
        assertSameArray(new B[] { b2, b3 }, a3.b);

    }

    @Test
    public void shouldReflectRemovalA() {

        session.save(a1);
        session.save(a2);
        session.save(a3);

        // it is our responsibility to keep the domain entities synchronized
        b2.a = null;
        a1.b = new B[] { b1 };
        a3.b = new B[] { b3 };

        session.save(b2);


        // when we reload a1
        a1 = session.load(A.class, a1.id);

        // expect the b2 relationship to have gone.
        assertSameArray(new B[] { b1 }, a1.b);


        // when we reload a3
        a3 = session.load(A.class, a3.id);

        // expect the b2 relationship to have gone.
        assertSameArray(new B[] { b3 }, a3.b);


        // but when we reload a2
        //session.clear();

        a2 = session.load(A.class, a2.id);


        // expect its relationships to be intact.
        assertSameArray(new B[] { b1, b3 }, a2.b);

    }

    @Test
    public void shouldBeAbleToAddAnotherB() {
        session.save(a1);

        B b3 = new B();
        b3.a = new A[] { a1 };
        a1.b = new B[] { b1, b2, b3 };

        // fully connected graph, should be able to save anu object
        session.save(b3);

        a1 = session.load(A.class, a1.id);

        assertSameArray(new B[]{ b1, b2, b3 }, a1.b);

    }

    @NodeEntity(label="A")
    public static class A extends E {

        @Relationship(type="EDGE", direction= Relationship.OUTGOING)
        B[] b;
    }

    @NodeEntity(label="B")
    public static class B extends E {

        @Relationship(type="EDGE", direction= Relationship.INCOMING)
        A[] a;
    }


    /**
     * Can be used as the basic class at the root of any entity for these tests,
     * provides the mandatory id field, a simple to-string method
     * and equals/hashcode.
     *
     * Note that without an equals/hashcode implementation, reloading
     * an object which already has a collection of items in it
     * will result in the collection items being added again, because
     * of the behaviour of the ogm merge function when handling
     * arrays and iterables.
     *
     *
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
