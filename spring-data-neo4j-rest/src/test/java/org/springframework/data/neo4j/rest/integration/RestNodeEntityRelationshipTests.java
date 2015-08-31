/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest.integration;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Mentorship;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.support.NodeEntityRelationshipTests;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
* @author mh
* @since 28.03.11
*/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
    "classpath:RestTests-context.xml"})
public class RestNodeEntityRelationshipTests extends NodeEntityRelationshipTests {

    @BeforeClass
    public static void startDb() throws Exception {
        RestTestBase.startDb();
    }

    @Before
    public void cleanDb() {
        RestTestBase.cleanDb();
    }

    @AfterClass
    public static void shutdownDb() {
        RestTestBase.shutdownDb();

    }

    @Test
    @Transactional
    public void testUpdateSingleRelatedToViaField() {
        Group group;
        final Long firstMentorshipId;
        final Person mentor2;
        try (Transaction tx = neo4jTemplate.getGraphDatabaseService().beginTx()) {
            group = persist(new Group());
            group.setMentorship(new Mentorship(persist(new Person()), group));
            persist(group);
            firstMentorshipId = group.getMentorship().getId();
            mentor2 = new Person();
            group.setMentorship(new Mentorship(persist(mentor2), group));
            persist(group);
            tx.success();
        }
        final Node node = neo4jTemplate.getPersistentState(group);
        assertEquals(1, IteratorUtil.count(node.getRelationships(Direction.INCOMING, DynamicRelationshipType.withName("mentors"))));
        final Group loaded = neo4jTemplate.load(node, Group.class);
        assertFalse(loaded.getMentorship().getId().equals(firstMentorshipId));
        assertEquals(mentor2, group.getMentorship().getMentor());
        assertEquals(group, group.getMentorship().getGroup());
    }

}
