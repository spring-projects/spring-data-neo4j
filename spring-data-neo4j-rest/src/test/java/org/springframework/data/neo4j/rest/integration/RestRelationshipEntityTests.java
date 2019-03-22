/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest.integration;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.data.neo4j.aspects.LeadRelationship;
import org.springframework.data.neo4j.aspects.support.RelationshipEntityTests;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
* @author mh
* @since 28.03.11
*/

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
"classpath:RestTests-context.xml"})
public class RestRelationshipEntityTests extends RelationshipEntityTests {

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
    public void testSaveRelationship() throws Exception {
        Person person = personRepository.save(new Person("Michael", 39));
        Group group = groupRepository.save(new Group());
        LeadRelationship rel = new LeadRelationship(person,group);
        LeadRelationship saved = neo4jTemplate.save(rel);
        LeadRelationship loaded = neo4jTemplate.findOne(saved.getId(),LeadRelationship.class);
        assertEquals(saved.getUuid(),loaded.getUuid());
        saved.setCreatedDate(new Date());
        LeadRelationship saved2 = neo4jTemplate.save(saved);
        LeadRelationship loaded2 = neo4jTemplate.findOne(saved2.getId(),LeadRelationship.class);
        assertEquals(saved.getId(),loaded2.getId());
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        assertEquals(format.format(saved2.getCreatedDate()),format.format(loaded2.getCreatedDate()));
    }

}
