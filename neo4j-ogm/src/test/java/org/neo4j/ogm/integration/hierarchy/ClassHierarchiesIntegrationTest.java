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

package org.neo4j.ogm.integration.hierarchy;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.integration.hierarchy.domain.annotated.*;
import org.neo4j.ogm.integration.hierarchy.domain.people.Bloke;
import org.neo4j.ogm.integration.hierarchy.domain.people.Entity;
import org.neo4j.ogm.integration.hierarchy.domain.people.Female;
import org.neo4j.ogm.integration.hierarchy.domain.people.Male;
import org.neo4j.ogm.integration.hierarchy.domain.people.Person;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainAbstractWithAnnotatedParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAbstractParentAndAnnotatedSuperclass;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAnnotatedAbstractNamedParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAnnotatedAbstractParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAnnotatedConcreteNamedParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAnnotatedConcreteParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAnnotatedConcreteSuperclass;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAnnotatedInterfaceParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAnnotatedNamedInterfaceParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithAnnotatedSuperInterface;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithPlainAbstractParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithPlainConcreteParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithPlainConcreteParentImplementingInterface;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithPlainInterfaceChild;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainChildWithPlainInterfaceParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainConcreteParent;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainSingleClass;
import org.neo4j.ogm.integration.hierarchy.domain.trans.PlainChildOfTransientInterface;
import org.neo4j.ogm.integration.hierarchy.domain.trans.PlainChildOfTransientParent;
import org.neo4j.ogm.integration.hierarchy.domain.trans.PlainClassWithTransientFields;
import org.neo4j.ogm.integration.hierarchy.domain.trans.TransientChildWithPlainConcreteParent;
import org.neo4j.ogm.integration.hierarchy.domain.trans.TransientSingleClass;
import org.neo4j.ogm.integration.hierarchy.domain.trans.TransientSingleClassWithId;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.neo4j.ogm.testutil.GraphTestUtils.*;

/**
 * Integration test for label-based mapping of class hierarchies.
 *
 * The rules should be as follows:
 * <ul>
 * <li>any plain concrete class in the hierarchy generates a label by default</li>
 * <li>plain abstract class does not generate a label by default</li>
 * <li>plain interface does not generate a label by default</li>
 * <li>any class annotated with @NodeEntity or @NodeEntity(label="something") generates a label</li>
 * <li>empty or null labels must not be allowed</li>
 * <li>classes / hierarchies that are not to be persisted must be annotated with @Transient</li>
 * </ul>
 *
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
public class ClassHierarchiesIntegrationTest extends WrappingServerIntegrationTest {

    private Session session;

    @Override
    protected int neoServerPort() {
        return 7896;
    }

    @Before
    public void init() throws IOException {
        SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.integration.hierarchy.domain");
        session = sessionFactory.openSession("http://localhost:" + 7896);
    }

    @Test
    public void annotatedChildWithAnnotatedAbstractNamedParent() {
        session.save(new AnnotatedChildWithAnnotatedAbstractNamedParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithAnnotatedAbstractNamedParent:Parent)");

        assertNotNull(session.load(AnnotatedChildWithAnnotatedAbstractNamedParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void annotatedChildWithAnnotatedNamedInterfaceParent() {
        session.save(new AnnotatedChildWithAnnotatedNamedInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithAnnotatedNamedInterfaceParent:Parent)");

        assertNotNull(session.load(AnnotatedChildWithAnnotatedNamedInterfaceParent.class, 0L));
    }


    @Test
    public void annotatedChildWithAnnotatedAbstractParent() {
        session.save(new AnnotatedChildWithAnnotatedAbstractParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithAnnotatedAbstractParent:AnnotatedAbstractParent)");

        assertNotNull(session.load(AnnotatedChildWithAnnotatedAbstractParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void annotatedChildWithAnnotatedInterfaceParent() {
        session.save(new AnnotatedChildWithAnnotatedInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithAnnotatedInterfaceParent:AnnotatedInterface)");

        assertNotNull(session.load(AnnotatedChildWithAnnotatedInterfaceParent.class, 0L));
    }

    @Test
    public void annotatedChildWithAnnotatedConcreteNamedParent() {
        session.save(new AnnotatedChildWithAnnotatedConcreteNamedParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithAnnotatedConcreteNamedParent:Parent)");

        assertNotNull(session.load(AnnotatedChildWithAnnotatedConcreteNamedParent.class, 0L));
    }

    @Test
    public void annotatedChildWithAnnotatedConcreteParent() {
        session.save(new AnnotatedChildWithAnnotatedConcreteParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithAnnotatedConcreteParent:AnnotatedConcreteParent)");

        assertNotNull(session.load(AnnotatedChildWithAnnotatedConcreteParent.class, 0L));
    }

    @Test
    public void annotatedChildWithPlainAbstractParent() {
        session.save(new AnnotatedChildWithPlainAbstractParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithPlainAbstractParent)");

        assertNotNull(session.load(AnnotatedChildWithPlainAbstractParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void annotatedChildWithPlainInterfaceParent() {
        session.save(new AnnotatedChildWithPlainInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithPlainInterfaceParent)");

        assertNotNull(session.load(AnnotatedChildWithPlainInterfaceParent.class, 0L));
    }

    @Test
    public void annotatedChildWithPlainConcreteParent() {
        session.save(new AnnotatedChildWithPlainConcreteParent());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithPlainConcreteParent:PlainConcreteParent)");

        assertNotNull(session.load(AnnotatedChildWithPlainConcreteParent.class, 0L));
    }

    @Test
    public void annotatedNamedChildWithAnnotatedAbstractNamedParent() {
        session.save(new AnnotatedNamedChildWithAnnotatedAbstractNamedParent());

        assertSameGraph(getDatabase(), "CREATE (:Child:Parent)");

        assertNotNull(session.load(AnnotatedNamedChildWithAnnotatedAbstractNamedParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void annotatedNamedChildWithAnnotatedNamedInterface() {
        session.save(new AnnotatedNamedChildWithAnnotatedNamedInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:Child:Parent)");

        assertNotNull(session.load(AnnotatedNamedChildWithAnnotatedNamedInterfaceParent.class, 0L));
    }

    @Test
    public void annotatedNamedChildWithAnnotatedAbstractParent() {
        session.save(new AnnotatedNamedChildWithAnnotatedAbstractParent());

        assertSameGraph(getDatabase(), "CREATE (:Child:AnnotatedAbstractParent)");

        assertNotNull(session.load(AnnotatedNamedChildWithAnnotatedAbstractParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void annotatedNamedChildWithAnnotatedInterfaceParent() {
        session.save(new AnnotatedNamedChildWithAnnotatedInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:Child:AnnotatedInterface)");

        assertNotNull(session.load(AnnotatedNamedChildWithAnnotatedInterfaceParent.class, 0L));
    }

    @Test
    public void annotatedNamedChildWithAnnotatedConcreteNamedParent() {
        session.save(new AnnotatedNamedChildWithAnnotatedConcreteNamedParent());

        assertSameGraph(getDatabase(), "CREATE (:Child:Parent)");

        assertNotNull(session.load(AnnotatedNamedChildWithAnnotatedConcreteNamedParent.class, 0L));
    }

    @Test
    public void annotatedNamedChildWithAnnotatedConcreteParent() {
        session.save(new AnnotatedNamedChildWithAnnotatedConcreteParent());

        assertSameGraph(getDatabase(), "CREATE (:Child:AnnotatedConcreteParent)");

        assertNotNull(session.load(AnnotatedNamedChildWithAnnotatedConcreteParent.class, 0L));
    }

    @Test
    public void annotatedNamedChildWithPlainAbstractParent() {
        session.save(new AnnotatedNamedChildWithPlainAbstractParent());

        assertSameGraph(getDatabase(), "CREATE (:Child)");

        assertNotNull(session.load(AnnotatedNamedChildWithPlainAbstractParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void annotatedNamedChildWithPlainInterfaceParent() {
        session.save(new AnnotatedNamedChildWithPlainInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:Child)");

        assertNotNull(session.load(AnnotatedNamedChildWithPlainInterfaceParent.class, 0L));
    }

    @Test
    public void annotatedNamedChildWithPlainConcreteParent() {
        session.save(new AnnotatedNamedChildWithPlainConcreteParent());

        assertSameGraph(getDatabase(), "CREATE (:Child:PlainConcreteParent)");

        assertNotNull(session.load(AnnotatedNamedChildWithPlainConcreteParent.class, 0L));
    }

    @Test
    public void annotatedNamedSingleClass() {
        session.save(new AnnotatedNamedSingleClass());

        assertSameGraph(getDatabase(), "CREATE (:Single)");

        assertNotNull(session.load(AnnotatedNamedSingleClass.class, 0L));
    }

    @Test
    public void annotatedSingleClass() {
        session.save(new AnnotatedSingleClass());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedSingleClass)");

        assertNotNull(session.load(AnnotatedSingleClass.class, 0L));
    }

    @Test
    public void plainChildWithAnnotatedAbstractNamedParent() {
        session.save(new PlainChildWithAnnotatedAbstractNamedParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAnnotatedAbstractNamedParent:Parent)");

        assertNotNull(session.load(PlainChildWithAnnotatedAbstractNamedParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildWithAnnotatedNamedInterfaceParent() {
        session.save(new PlainChildWithAnnotatedNamedInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAnnotatedNamedInterfaceParent:Parent)");

        assertNotNull(session.load(PlainChildWithAnnotatedNamedInterfaceParent.class, 0L));
    }

    @Test
    public void plainChildWithAnnotatedAbstractParent() {
        session.save(new PlainChildWithAnnotatedAbstractParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAnnotatedAbstractParent:AnnotatedAbstractParent)");

        assertNotNull(session.load(PlainChildWithAnnotatedAbstractParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildWithAnnotatedInterfaceParent() {
        session.save(new PlainChildWithAnnotatedInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAnnotatedInterfaceParent:AnnotatedInterface)");

        assertNotNull(session.load(PlainChildWithAnnotatedInterfaceParent.class, 0L));
    }

    @Test
    public void plainChildWithAnnotatedConcreteNamedParent() {
        session.save(new PlainChildWithAnnotatedConcreteNamedParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAnnotatedConcreteNamedParent:Parent)");

        assertNotNull(session.load(PlainChildWithAnnotatedConcreteNamedParent.class, 0L));
    }

    @Test
    public void plainChildWithAnnotatedConcreteParent() {
        session.save(new PlainChildWithAnnotatedConcreteParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAnnotatedConcreteParent:AnnotatedConcreteParent)");

        assertNotNull(session.load(PlainChildWithAnnotatedConcreteParent.class, 0L));
    }

    @Test
    public void plainChildWithPlainAbstractParent() {
        session.save(new PlainChildWithPlainAbstractParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithPlainAbstractParent)");

        assertNotNull(session.load(PlainChildWithPlainAbstractParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildWithPlainInterfaceParent() {
        session.save(new PlainChildWithPlainInterfaceParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithPlainInterfaceParent)");

        assertNotNull(session.load(PlainChildWithPlainInterfaceParent.class, 0L));
    }


    @Test
    public void plainChildWithPlainConcreteParent() {
        session.save(new PlainChildWithPlainConcreteParent());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithPlainConcreteParent:PlainConcreteParent)");

        assertNotNull(session.load(PlainChildWithPlainConcreteParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildWithPlainConcreteParentImplementingInterface() {
        session.save(new PlainChildWithPlainConcreteParentImplementingInterface());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithPlainConcreteParentImplementingInterface:PlainConcreteParent)");

        assertNotNull(session.load(PlainChildWithPlainConcreteParentImplementingInterface.class, 0L));
        assertNotNull(session.load(PlainConcreteParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildWithPlainInterfaceChild() {
        session.save(new PlainChildWithPlainInterfaceChild());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithPlainInterfaceChild)");

        assertNotNull(session.load(PlainChildWithPlainInterfaceChild.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildWithAnnotatedSuperInterface() {
        /*
        PlainChildWithAnnotatedSuperInterface->PlainInterfaceChildWithAnnotatedParentInterface->AnnotatedInterface
         */
        session.save(new PlainChildWithAnnotatedSuperInterface());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAnnotatedSuperInterface:AnnotatedInterface)");
        assertNotNull(session.load(PlainChildWithAnnotatedSuperInterface.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void annotatedChildWithAnnotatedInterface() {
        /*
        AnnotatedChildWithAnnotatedInterface->AnnotatedInterfaceWithSingleImpl
         */
        session.save(new AnnotatedChildWithAnnotatedInterface());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithAnnotatedInterface:AnnotatedInterfaceWithSingleImpl)");
        assertNotNull(session.load(AnnotatedChildWithAnnotatedInterface.class, 0L));
        assertNotNull(session.load(AnnotatedInterfaceWithSingleImpl.class, 0L)); //AnnotatedInterfaceWithSingleImpl has a single implementation so we should be able to load it
        assertEquals("org.neo4j.ogm.integration.hierarchy.domain.annotated.AnnotatedChildWithAnnotatedInterface",session.load(AnnotatedInterfaceWithSingleImpl.class, 0L).getClass().getName());
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildWithAnnotatedSuperclass() {
        /*
           PlainChildWithAnnotatedConcreteSuperclass->PlainChildWithAnnotatedConcreteParent->AnnotatedConcreteParent
         */
        session.save(new PlainChildWithAnnotatedConcreteSuperclass());

        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAnnotatedConcreteSuperclass:PlainChildWithAnnotatedConcreteParent:AnnotatedConcreteParent)");
        assertNotNull(session.load(PlainChildWithAnnotatedConcreteSuperclass.class, 0L));
        assertNotNull(session.load(PlainChildWithAnnotatedConcreteParent.class, 0L));
        assertNotNull(session.load(AnnotatedConcreteParent.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildWithAbstractParentAndAnnotatedSuperclass() {
        /*
           PlainChildWithAbstractParentAndAnnotatedSuperclass->PlainAbstractWithAnnotatedParent->AnnotatedSingleClass
         */
        session.save(new PlainChildWithAbstractParentAndAnnotatedSuperclass());
        assertSameGraph(getDatabase(), "CREATE (:PlainChildWithAbstractParentAndAnnotatedSuperclass:AnnotatedSingleClass)");
        assertNotNull(session.load(PlainChildWithAbstractParentAndAnnotatedSuperclass.class, 0L));
        assertNotNull(session.load(PlainAbstractWithAnnotatedParent.class, 0L));
        assertNotNull(session.load(AnnotatedSingleClass.class, 0L));
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void annotatedChildWithMultipleAnnotatedInterfaces() {
        session.save(new AnnotatedChildWithMultipleAnnotatedInterfaces());

        assertSameGraph(getDatabase(), "CREATE (:AnnotatedChildWithMultipleAnnotatedInterfaces:AnnotatedInterface:Parent)");
        assertNotNull(session.load(AnnotatedChildWithMultipleAnnotatedInterfaces.class, 0L));
        assertEquals(1,session.loadAll(AnnotatedInterface.class).size());
        assertEquals(1,session.loadAll(AnnotatedNamedInterfaceParent.class).size());
    }

    @Test
    public void plainSingleClass() {
        session.save(new PlainSingleClass());

        assertSameGraph(getDatabase(), "CREATE (:PlainSingleClass)");

        assertNotNull(session.load(PlainSingleClass.class, 0L));
    }

    @Test
    public void plainChildOfTransientParent() {
        session.save(new PlainChildOfTransientParent());

        try (Transaction tx = getDatabase().beginTx()) {
            assertFalse(GlobalGraphOperations.at(getDatabase()).getAllNodes().iterator().hasNext());
            tx.success();
        }
    }

    /**
     * @see DATAGRAPH-577
     */
    @Test
    public void plainChildOfTransientInterface() {
        session.save(new PlainChildOfTransientInterface());
        try (Transaction tx = getDatabase().beginTx()) {
            assertFalse(GlobalGraphOperations.at(getDatabase()).getAllNodes().iterator().hasNext());
            tx.success();
        }
    }

    @Test
    public void transientChildWithPlainConcreteParent() {
        session.save(new TransientChildWithPlainConcreteParent());

        try (Transaction tx = getDatabase().beginTx()) {
            assertFalse(GlobalGraphOperations.at(getDatabase()).getAllNodes().iterator().hasNext());
            tx.success();
        }
    }

    @Test
    public void transientSingleClass() {
        session.save(new TransientSingleClass());

        try (Transaction tx = getDatabase().beginTx()) {
            assertFalse(GlobalGraphOperations.at(getDatabase()).getAllNodes().iterator().hasNext());
            tx.success();
        }
    }

    @Test
    public void transientSingleClassWithId() {
        session.save(new TransientSingleClassWithId());

        try (Transaction tx = getDatabase().beginTx()) {
            assertFalse(GlobalGraphOperations.at(getDatabase()).getAllNodes().iterator().hasNext());
            tx.success();
        }
    }

    @Test
    public void plainClassWithTransientFields() {

        PlainClassWithTransientFields toSave = new PlainClassWithTransientFields();

        toSave.setAnotherTransientField(new PlainSingleClass());
        toSave.setTransientField(new PlainChildOfTransientParent());
        toSave.setYetAnotherTransientField(new PlainSingleClass());

        session.save(toSave);

        assertSameGraph(getDatabase(), "CREATE (:PlainClassWithTransientFields)");

        assertNotNull(session.load(PlainClassWithTransientFields.class, 0L));
    }

    @Test(expected = ClassCastException.class)
    public void shouldNotBeAbleToLoadClassOfWrongType() {
        session.save(new AnnotatedNamedSingleClass());
        session.load(PlainSingleClass.class, 0L);
    }

    @Test
    public void shouldSaveHierarchy() {
        session.save(new Female("Daniela"));
        session.save(new Male("Michal"));
        session.save(new Bloke("Adam"));

        assertSameGraph(getDatabase(), "CREATE (:Female:Person {name:'Daniela'})," +
                "(:Male:Person {name:'Michal'})," +
                "(:Bloke:Male:Person {name:'Adam'})");
    }

    @Test
    public void shouldSaveHierarchy2() {
        session.save(Arrays.asList(new Female("Daniela"), new Male("Michal"), new Bloke("Adam")));

        assertSameGraph(getDatabase(), "CREATE (:Female:Person {name:'Daniela'})," +
                "(:Male:Person {name:'Michal'})," +
                "(:Bloke:Male:Person {name:'Adam'})");
    }

    @Test
    public void shouldReadHierarchyAndRetrieveBySuperclass() {

        Female daniela = new Female("Daniela");
        Male michal = new Male("Michal");
        Bloke adam = new Bloke("Adam");

        session.save(Arrays.asList(daniela, michal, adam));

        Collection<Entity> entities = session.loadAll(Entity.class);

        Collection<Person> people = session.loadAll(Person.class);
        Collection<Male> males = session.loadAll(Male.class);
        Collection<Female> females = session.loadAll(Female.class);
        Collection<Bloke> blokes = session.loadAll(Bloke.class);

        assertTrue("Shouldn't be able to load by non-annotated, abstract classes", entities.isEmpty());

        assertEquals(3, people.size());
        assertEquals(people.size(), session.countEntitiesOfType(Person.class));
        assertTrue(people.containsAll(Arrays.asList(daniela, michal, adam)));

        assertEquals(2, males.size());
        assertTrue(males.containsAll(Arrays.asList(michal, adam)));

        assertEquals(1, females.size());
        assertEquals(females.size(), session.countEntitiesOfType(Female.class));
        assertTrue(females.contains(daniela));

        assertEquals(1, blokes.size());
        assertTrue(blokes.contains(adam));

    }

    @Test
    public void shouldReadHierarchy2() {

        new ExecutionEngine(getDatabase()).execute("CREATE (:Female:Person:Entity {name:'Daniela'})," +
                "(:Male:Person:Entity {name:'Michal'})," +
                "(:Bloke:Male:Person:Entity {name:'Adam'})");

        Female daniela = new Female("Daniela");
        Male michal = new Male("Michal");
        Bloke adam = new Bloke("Adam");

        Collection<Entity> entities = session.loadAll(Entity.class);
        Collection<Person> people = session.loadAll(Person.class);
        Collection<Male> males = session.loadAll(Male.class);
        Collection<Female> females = session.loadAll(Female.class);
        Collection<Bloke> blokes = session.loadAll(Bloke.class);

        assertEquals(3, entities.size());
        assertTrue(entities.containsAll(Arrays.asList(daniela, michal, adam)));

        assertEquals(3, people.size());
        assertTrue(people.containsAll(Arrays.asList(daniela, michal, adam)));

        assertEquals(2, males.size());
        assertTrue(males.containsAll(Arrays.asList(michal, adam)));

        assertEquals(1, females.size());
        assertTrue(females.contains(daniela));

        for (Bloke bloke : blokes) {
            System.out.println(bloke.getName() + ": " + bloke);
        }
        assertEquals(1, blokes.size());
        assertTrue(blokes.contains(adam));
    }

    @Test
    public void shouldReadHierarchy3() {
        new ExecutionEngine(getDatabase()).execute("CREATE (:Female:Person {name:'Daniela'})," +
                "(:Male:Person {name:'Michal'})," +
                "(:Bloke:Male:Person {name:'Adam'})");

        Female daniela = new Female("Daniela");
        Male michal = new Male("Michal");
        Bloke adam = new Bloke("Adam");

        Collection<Person> people = session.loadAll(Person.class);
        Collection<Male> males = session.loadAll(Male.class);
        Collection<Female> females = session.loadAll(Female.class);
        Collection<Bloke> blokes = session.loadAll(Bloke.class);

        assertEquals(3, people.size());
        assertTrue(people.containsAll(Arrays.asList(daniela, michal, adam)));

        assertEquals(2, males.size());
        assertTrue(males.containsAll(Arrays.asList(michal, adam)));

        assertEquals(1, females.size());
        assertTrue(females.contains(daniela));

        assertEquals(1, blokes.size());
        assertTrue(blokes.contains(adam));
    }

    @Test
    public void shouldReadHierarchy4() {
        new ExecutionEngine(getDatabase()).execute("CREATE (:Female {name:'Daniela'})," +
                "(:Male {name:'Michal'})," +
                "(:Bloke:Male {name:'Adam'})");

        Female daniela = new Female("Daniela");
        Male michal = new Male("Michal");
        Bloke adam = new Bloke("Adam");

        Collection<Male> males = session.loadAll(Male.class);
        Collection<Female> females = session.loadAll(Female.class);
        Collection<Bloke> blokes = session.loadAll(Bloke.class);

        assertEquals(2, males.size());
        assertTrue(males.containsAll(Arrays.asList(michal, adam)));

        assertEquals(1, females.size());
        assertTrue(females.contains(daniela));

        assertEquals(1, blokes.size());
        assertTrue(blokes.contains(adam));
    }

    @Test
    // the logic of this test is debatable. the domain model and persisted schema are not the same.
    public void shouldReadHierarchy5() {

        new ExecutionEngine(getDatabase()).execute("CREATE (:Female {name:'Daniela'})," +
                "(:Male {name:'Michal'})," +
                "(:Bloke {name:'Adam'})");

        Female daniela = new Female("Daniela");
        Male michal = new Male("Michal");
        Bloke adam = new Bloke("Adam");

        Collection<Male> males = session.loadAll(Male.class);
        Collection<Female> females = session.loadAll(Female.class);
        Collection<Bloke> blokes = session.loadAll(Bloke.class);

        assertEquals(1, males.size());
        assertTrue(males.containsAll(Arrays.asList(michal)));

        assertEquals(1, females.size());
        assertTrue(females.contains(daniela));

        assertEquals(1, blokes.size());
        assertTrue(blokes.contains(adam));
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotReadHierarchy() {
        new ExecutionEngine(getDatabase()).execute("CREATE (:Person {name:'Daniela'})");

        session.loadAll(Person.class);
    }

    @Test
    public void shouldLeaveExistingLabelsAlone() {
        new ExecutionEngine(getDatabase()).execute("CREATE (:Female:Person:GoldMember {name:'Daniela'})");

        session.save(session.load(Person.class, 0L));

        assertSameGraph(getDatabase(), "CREATE (:Female:Person:GoldMember {name:'Daniela'})");
    }

    //this should throw an exception, but for a different reason than it does now!
    @Test//(expected = RuntimeException.class)
    public void shouldFailWithConflictingHierarchies() {
        new ExecutionEngine(getDatabase()).execute("CREATE (:Female:Person {name:'Daniela'})");

        SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.integration.hierarchy.domain", "org.neo4j.ogm.integration.hierarchy.conflicting");
        session = sessionFactory.openSession("http://localhost:" + 7896);

        session.loadAll(Person.class);
    }
}
