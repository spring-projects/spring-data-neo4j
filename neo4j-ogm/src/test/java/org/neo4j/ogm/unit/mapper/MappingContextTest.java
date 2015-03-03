/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.unit.mapper;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.policy.Person;
import org.neo4j.ogm.domain.policy.Policy;
import org.neo4j.ogm.mapper.MappedRelationship;
import org.neo4j.ogm.mapper.MappingContext;
import org.neo4j.ogm.metadata.MetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MappingContextTest {

    private MappingContext collector;
    private static final int NUM_OBJECTS=100000;
    private static final int NUM_THREADS=15;

    @Before
    public void setUp() {
        collector = new MappingContext(new MetaData("org.neo4j.ogm.domain.policy"));
    }

    @Test
    public void testPath() {

        Person jim = new Person("jim");
        jim.setId(1L);

        Policy policy = new Policy("healthcare");
        policy.setId(2L);

        collector.registerNodeEntity(jim, jim.getId());
        collector.registerNodeEntity(policy, policy.getId());
        collector.registerRelationship(new MappedRelationship(jim.getId(), "INFLUENCES", policy.getId()));

        assertEquals(jim, collector.get(jim.getId()));
        assertEquals(policy, collector.get(policy.getId()));
        assertTrue(collector.isRegisteredRelationship(new MappedRelationship(jim.getId(), "INFLUENCES", policy.getId())));

    }

    @Test
    public void clearOne() {

        Person jim = new Person("jim");
        jim.setId(1L);

        Policy policy = new Policy("healthcare");
        policy.setId(2L);

        collector.registerNodeEntity(jim, jim.getId());
        collector.registerNodeEntity(policy, policy.getId());
        collector.registerRelationship(new MappedRelationship(jim.getId(), "INFLUENCES", policy.getId()));
        collector.clear(jim);

        assertEquals(null, collector.get(jim.getId()));
        assertEquals(policy, collector.get(policy.getId()));
        assertFalse(collector.isRegisteredRelationship(new MappedRelationship(jim.getId(), "INFLUENCES", policy.getId())));

    }

    @Test
    public void clearType() {
        Person jim = new Person("jim");
        jim.setId(1L);

        Policy healthcare = new Policy("healthcare");
        healthcare.setId(2L);

        Policy immigration = new Policy("immigration");
        immigration.setId(3L);

        Person rik = new Person("rik");
        rik.setId(4L);

        collector.registerNodeEntity(jim, jim.getId());
        collector.registerNodeEntity(rik, rik.getId());
        collector.registerNodeEntity(healthcare, healthcare.getId());
        collector.registerNodeEntity(immigration, immigration.getId());

        collector.registerRelationship(new MappedRelationship(jim.getId(), "INFLUENCES", healthcare.getId()));
        collector.registerRelationship(new MappedRelationship(jim.getId(), "INFLUENCES", immigration.getId()));
        collector.registerRelationship(new MappedRelationship(jim.getId(), "WORKS_WITH", rik.getId()));

        collector.clear(Policy.class);

        assertEquals(0, collector.getAll(Policy.class).size());
        assertEquals(null, collector.get(healthcare.getId()));
        assertEquals(null, collector.get(immigration.getId()));

        assertEquals(jim, collector.get(jim.getId()));
        assertEquals(rik, collector.get(rik.getId()));
        assertEquals(1, collector.mappedRelationships().size());

    }

    @Test
    public void ensureThreadSafe() throws InterruptedException {

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thread = new Thread(new Inserter());
            threads.add(thread);
            thread.start();
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            threads.get(i).join();
        }

        Set<Object> objects = collector.getAll(TestObject.class);

        assertEquals(NUM_OBJECTS, objects.size());

        int sum = (NUM_OBJECTS * (NUM_OBJECTS + 1)) / 2;

        for (Object object : objects) {
            TestObject testObject = (TestObject) object;
            sum -= testObject.id;                           // remove this id from sum of all ids
            assertEquals(1, testObject.notes.size());       // only one thread created this object
            int id = Integer.parseInt(testObject.notes.get(0));
        }

        assertEquals(0, sum);                               // all objects were created

    }

    public class TestObject {
        Long id = null;
        List<String> notes = new ArrayList<>();
    }

    class Inserter implements Runnable {

        @Override
        public void run() {
            for (int i = 1; i <= NUM_OBJECTS; i++) {
                Long id = new Long(i);

                TestObject testObject = (TestObject) collector.get(id);
                if (testObject == null) {
                    testObject = (TestObject) collector.registerNodeEntity(new TestObject(), id);
                    synchronized (testObject) {
                        if (testObject.id == null) {
                            testObject.notes.add(String.valueOf(Thread.currentThread().getId()));
                            testObject.id = id;
                        }
                    }
                }

            }
        }
    }

}
