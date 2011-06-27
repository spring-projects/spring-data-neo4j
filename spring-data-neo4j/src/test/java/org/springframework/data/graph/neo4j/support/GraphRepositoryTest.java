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

package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.PersonRepository;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:repository-namespace-config-context.xml"})
public class GraphRepositoryTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

    @Autowired
    private PersonRepository personRepository;
    private TestTeam testTeam;

    @Before
    public void setUp() throws Exception {
        testTeam = new TestTeam();
        testTeam.createSDGTeam();
    }

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testFindIterableOfPersonWithQueryAnnotation() {
        Iterable<Person> teamMembers = personRepository.findAllTeamMembers(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david, testTeam.emil));
    }

    @Test
    @Transactional
    public void testFindPersonWithQueryAnnotation() {
        Person boss = personRepository.findBoss(testTeam.michael);
        assertThat(boss, is(testTeam.emil));
    }
    @Test
    @Transactional
    public void testFindIterableMapsWithQueryAnnotation() {
        Iterable<Map<String,Object>> teamMembers = personRepository.findAllTeamMemberData(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.simpleRowFor(testTeam.michael, "member"), testTeam.simpleRowFor(testTeam.david, "member"), testTeam.simpleRowFor(testTeam.emil, "member")));
    }

    @Test
    @Transactional
    public void testFindPaged() {
        final PageRequest page = new PageRequest(0, 1, Sort.Direction.ASC, "member.name");
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(page, testTeam.sdg);
        assertThat(teamMemberPage1, is((Iterable) asList(testTeam.david)));
    }
    @Test
    @Transactional
    public void testFindPagedDescending() {
        final PageRequest page = new PageRequest(0, 2, Sort.Direction.DESC, "member.name");
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(page, testTeam.sdg);
        assertThat(teamMemberPage1, is((Iterable) asList(testTeam.michael, testTeam.emil)));
        assertThat(teamMemberPage1.isFirstPage(), is(true));
    }
    @Test
    @Transactional
    public void testFindPagedNull() {
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(null, testTeam.sdg);
        assertThat(teamMemberPage1, is((Iterable) asList(testTeam.michael, testTeam.emil)));
        assertThat(teamMemberPage1.isFirstPage(), is(true));
        assertThat(teamMemberPage1.isLastPage(), is(true));
    }

    @Test
    @Transactional
    public void testFindSortedDescending() {
        final Sort sort = new Sort(Sort.Direction.DESC, "member.name");
        Iterable<Person> teamMembers = personRepository.findAllTeamMembersSorted(testTeam.sdg, sort);
        assertThat(teamMembers, is((Iterable)asList(testTeam.michael, testTeam.emil, testTeam.david)));
    }

    @Test
    @Transactional
    public void testFindSortedNull() {
        Iterable<Person> teamMembers = personRepository.findAllTeamMembersSorted(testTeam.sdg, null);
        assertThat(teamMembers, hasItems(testTeam.michael, testTeam.emil, testTeam.david));
    }

    @Test
    @Transactional
    public void testFindByNamedQuery() {
        Group team = personRepository.findTeam(testTeam.michael);
        assertThat(team, is(testTeam.sdg));
    }
}
