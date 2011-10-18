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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.neo4j.helpers.collection.IteratorUtil.addToCollection;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
@Transactional
public class GraphRepositoryTest {

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Autowired
    private PersonRepository personRepository;
    @Autowired
    GroupRepository groupRepository;

    @Autowired FriendshipRepository friendshipRepository;

    private TestTeam testTeam;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }
    @Before
    public void setUp() throws Exception {
        testTeam = new TestTeam();
        testTeam.createSDGTeam(personRepository, groupRepository,friendshipRepository);
    }

    @Test
    public void testFindIterableOfPersonWithQueryAnnotation() {
        Iterable<Person> teamMembers = personRepository.findAllTeamMembers(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david, testTeam.emil));
    }
    @Test
    public void testFindIterableOfPersonWithQueryAnnotationAndGremlin() {
        Iterable<Person> teamMembers = personRepository.findAllTeamMembersGremlin(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david, testTeam.emil));
    }

    @Test
    public void testFindPersonWithQueryAnnotation() {
        Person boss = personRepository.findBoss(testTeam.michael);
        assertThat(boss, is(testTeam.emil));
    }
    @Test
    public void testFindIterableMapsWithQueryAnnotation() {
        Iterable<Map<String,Object>> teamMembers = personRepository.findAllTeamMemberData(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.simpleRowFor(testTeam.michael, "member"), testTeam.simpleRowFor(testTeam.david, "member"), testTeam.simpleRowFor(testTeam.emil, "member")));
    }

    @Test
    public void testFindPaged() {
        final PageRequest page = new PageRequest(0, 1, Sort.Direction.ASC, "member.name");
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(testTeam.sdg,page);
        assertThat(teamMemberPage1, hasItem(testTeam.david));
    }
    @Test
    public void testFindPagedDescending() {
        final PageRequest page = new PageRequest(0, 2, Sort.Direction.DESC, "member.name");
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(testTeam.sdg,page);
        assertEquals(asList(testTeam.michael, testTeam.emil), asCollection(teamMemberPage1));
        assertThat(teamMemberPage1.isFirstPage(), is(true));
    }
    @Test
    public void testFindPagedNull() {
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(testTeam.sdg,null);
        assertEquals(new HashSet(asList(testTeam.david, testTeam.emil, testTeam.michael)), addToCollection(teamMemberPage1, new HashSet()));
        assertThat(teamMemberPage1.isFirstPage(), is(true));
        assertThat(teamMemberPage1.isLastPage(), is(false));
    }

    @Test
    public void testFindSortedDescending() {
        final Sort sort = new Sort(Sort.Direction.DESC, "member.name");
        Iterable<Person> teamMembers = personRepository.findAllTeamMembersSorted(testTeam.sdg, sort);
        assertEquals(asList(testTeam.michael, testTeam.emil, testTeam.david), asCollection(teamMembers));
    }

    @Test
    public void testFindSortedNull() {
        Iterable<Person> teamMembers = personRepository.findAllTeamMembersSorted(testTeam.sdg, null);
        assertThat(teamMembers, hasItems(testTeam.michael, testTeam.emil, testTeam.david));
    }

    @Test
    public void testFindByNamedQuery() {
        Group team = personRepository.findTeam(testTeam.michael);
        assertThat(team, is(testTeam.sdg));
    }

    @Test
    public void findByName() {

        Iterable<Person> findByName = personRepository.findByName(testTeam.michael.getName());
        assertThat(findByName, hasItem(testTeam.michael));
    }
}
