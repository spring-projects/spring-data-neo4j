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

package org.springframework.data.neo4j.repository;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.Personality;
import org.springframework.data.neo4j.repositories.FriendshipRepository;
import org.springframework.data.neo4j.repositories.GroupRepository;
import org.springframework.data.neo4j.repositories.PersonRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/repository/GraphRepositoryTests-context.xml"})
@Transactional
public class SpatialGraphRepositoryTests {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Autowired
    private PersonRepository personRepository;
    @Autowired
    GroupRepository groupRepository;

    @Autowired
    FriendshipRepository friendshipRepository;

    
    @Autowired
    PlatformTransactionManager transactionManager;
        
    private TestTeam testTeam;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }
    @Before
    public void setUp() throws Exception {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                testTeam = new TestTeam();
                testTeam.createSDGTeam(personRepository, groupRepository,friendshipRepository);
            }
        });
    }

    @Test
    public void testFindPeopleWithinBoundingBox() {
        Iterable<Person> teamMembers = personRepository.findWithinBoundingBox("personLayer", 55, 15, 57, 17);
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david));
    }

    @Test
    public void testFindPeopleWithinBoundingBoxShape() {
        Iterable<Person> teamMembers = personRepository.findWithinBoundingBox("personLayer", new Box(new Point(15, 55), new Point(17, 57)));
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david));
    }

    @Test
    public void testFindPeopleWithinBoundingBoxShapeDerived() {
        Iterable<Person> teamMembers = personRepository.findByWktWithin(new Box(new Point(15, 55), new Point(17, 57)));
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david));
    }

    @Test
    public void testFindPeopleWithinPolygon() {
        Iterable<Person> teamMembers = personRepository.findWithinWellKnownText("personLayer", "POLYGON ((15 55, 15 57, 17 57, 17 55, 15 55))");
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david));
    }

    @Test
    public void testFindPeopleWithinPolygonShape() {
        Iterable<Person> teamMembers = personRepository.findWithinShape("personLayer", new Polygon(new Point(15,55),new Point(15,57), new Point(17,57),new Point(17,55)));
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david));
    }

    @Test
    public void testFindPeopleWithinPolygonShapeDerived() {
        Iterable<Person> teamMembers = personRepository.findByWktWithinAndPersonality(new Polygon(new Point(15, 55), new Point(15, 57), new Point(17, 57), new Point(17, 55)), Personality.EXTROVERT);
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael));
    }

    @Test
    public void testFindPeopleWithinDistance() {
        Iterable<Person> teamMembers = personRepository.findWithinDistance("personLayer", 56,16,70);
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david));
    }

    @Test
    public void testFindPeopleWithinCircle() {
        Iterable<Person> teamMembers = personRepository.findWithinDistance("personLayer", new Circle(new Point(16,56),new Distance(70, Metrics.KILOMETERS)));
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david));
    }

    @Test
    public void testFindPeopleNearCircleDerived() {
        Iterable<Person> teamMembers = personRepository.findByWktNearAndName(new Circle(new Point(16,56),new Distance(70, Metrics.KILOMETERS)),"David");
        assertThat(asCollection(teamMembers), contains(testTeam.david));
    }
    @Test
    public void testFindPeopleWithinCircleDerived() {
        Iterable<Person> teamMembers = personRepository.findByWktWithinAndAgeGreaterThan(new Circle(new Point(16, 56), new Distance(70, Metrics.KILOMETERS)),30);
        assertThat(asCollection(teamMembers), contains(testTeam.michael));
    }

    @Test
    @Ignore
    public void testPerformance() throws Exception {
        long time=System.currentTimeMillis();
        for (int i=0;i<5000;i++) {
            if (i % 1000 == 0) {
                long now = System.currentTimeMillis();
                System.out.println(i+". entries " + (now-time));
                time=now;
            }
            Person person = new Person("John " + i, 40 + i);
            person.setLocation((i % 180) - 90,(i % 180) - 90);
            personRepository.save(person);
        }
    }
}
