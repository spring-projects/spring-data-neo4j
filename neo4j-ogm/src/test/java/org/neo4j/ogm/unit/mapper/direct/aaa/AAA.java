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

package org.neo4j.ogm.unit.mapper.direct.aaa;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.unit.mapper.direct.RelationshipTest;

/**
 * @author Luanne Misquitta
 */
public class AAA extends RelationshipTest{
	private A a1, a2, a3, a4;
	private A loadedA1, loadedA2, loadedA3, loadedA4;

	private static SessionFactory sessionFactory;

	@Before
	public void init() throws IOException {
		setUp();
		sessionFactory = new SessionFactory("org.neo4j.ogm.unit.mapper.direct.aaa");
		session = sessionFactory.openSession("http://localhost:" + neoPort);
		setUpEntityModel();

	}

	private void setUpEntityModel() {
		a1 = new A();
		a2 = new A();
		a3 = new A();
		a4 = new A();

		a1.a = new A[] { a2, a3 };
		a2.a = new A[] { a1, a3 };
		a3.a = new A[] { a2, a4 };


	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldFindStartAFromEndA() {

		session.save(a1);
		session.save(a2);
		session.save(a3);
		session.save(a4);

		loadedA1 = session.load(A.class, a1.id);
		loadedA2 = session.load(A.class, a2.id);
		loadedA3 = session.load(A.class, a3.id);

		assertSameArray(new A[]{a2, a3}, loadedA1.a);
		assertSameArray(new A[]{a1, a3}, loadedA2.a);
		assertSameArray(new A[]{a2, a4}, loadedA3.a);

	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldFindEndAFromStartA() {

		session.save(a1);
		session.save(a2);
		session.save(a3);
		session.save(a4);

		loadedA1 = session.load(A.class, a1.id);
		loadedA2 = session.load(A.class, a2.id);
		loadedA3 = session.load(A.class, a3.id);
		loadedA4 = session.load(A.class, a4.id);

		assertSameArray(new A[]{loadedA2, loadedA3}, a1.a);
		assertSameArray(new A[] { loadedA1, loadedA3 }, a2.a);
		assertSameArray(new A[]{loadedA2, loadedA4}, a3.a);

	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldPreserveAAfterReflectRemovalOtherA() {

		session.save(a1);
		session.save(a2);
		session.save(a3);
		session.save(a4);

		// it is our responsibility to keep the domain entities synchronized
		a2.a = null;

		session.save(a2);

		//when we reload a2
		loadedA2 = session.load(A.class, a2.id);
		// expect its relationships have gone.
		assertNull(loadedA2.a);

		// when we reload a1
		loadedA1 = session.load(A.class, a1.id);
		// expect the original relationships to remain intact.
		assertSameArray(new A[]{a2, a3}, loadedA1.a);

		// when we reload a3
		loadedA3 = session.load(A.class, a3.id);
		// expect the original relationships to remain intact.
		assertSameArray(new A[]{a2, a4}, loadedA3.a);

		//when we reload a4
		loadedA4 = session.load(A.class, a4.id);
		//expect the original relationships to remain intact.
		assertNull(loadedA4.a);
	}


	@NodeEntity(label="A")
	public static class A extends E {

		@Relationship(type="EDGE", direction= Relationship.OUTGOING)
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
