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

package org.springframework.data.neo4j.aspects.support;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.aspects.support.domain.Account1;
import org.springframework.data.neo4j.aspects.support.domain.Account2;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.neo4j.helpers.collection.IteratorUtil.addToCollection;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/repository-namespace-config-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class GraphRepositoryTests extends EntityTestBase {

    @Before
    public void setUp() throws Exception {
        testTeam.createSDGTeam();
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
    @SuppressWarnings("unchecked")
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
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(testTeam.sdg,page);
        assertThat(teamMemberPage1, hasItem(testTeam.david));
    }
    @Test
    @Transactional
    public void testFindPagedDescending() {
        final PageRequest page = new PageRequest(0, 2, Sort.Direction.DESC, "member.name");
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(testTeam.sdg,page);
        assertEquals(asList(testTeam.michael, testTeam.emil), asCollection(teamMemberPage1));
        assertThat(teamMemberPage1.isFirstPage(), is(true));
    }
    @SuppressWarnings("unchecked")
    @Test
    @Transactional
    public void testFindPagedNull() {
        Page<Person> teamMemberPage1 = personRepository.findAllTeamMembersPaged(testTeam.sdg,null);
        assertEquals(new HashSet(asList(testTeam.david, testTeam.emil, testTeam.michael)), addToCollection(teamMemberPage1, new HashSet()));
        assertThat(teamMemberPage1.isFirstPage(), is(true));
        assertThat(teamMemberPage1.isLastPage(), is(true));
    }

    @Test
    @Transactional
    public void testFindSortedDescending() {
        final Sort sort = new Sort(Sort.Direction.DESC, "member.name");
        Iterable<Person> teamMembers = personRepository.findAllTeamMembersSorted(testTeam.sdg, sort);
        assertEquals(asList(testTeam.michael, testTeam.emil, testTeam.david), asCollection(teamMembers));
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


    @Test
    @Transactional
    public void testSaveWhenFailOnDuplicateSetToFalse() {
        // Account1
        // @Indexed(unique = true, failOnDuplicate = false)
        // private String accountNumber;
        Account1 acc1 = new Account1("111-222-333", "Mr George - Current Account 1");
        Account1 acc2 = new Account1("111-222-333", "Mr George - Current Account 2");
        Account1 savedAcc1 = account1Repository.save(acc1);
        Account1 savedAcc2 = account1Repository.save(acc2);
        assertEquals("expecting the saving of the same entity result in a merge of nodes", ((NodeBacked)savedAcc1).getNodeId(), ((NodeBacked)savedAcc2).getNodeId());
        assertEquals("Mr George - Current Account 2", savedAcc2.getName() );

        Account1 loadedAcc1 = account1Repository.findBySchemaPropertyValue("accountNumber", "111-222-333");
        assertEquals("Mr George - Current Account 2", loadedAcc1.getName() );

    }

    @Test(expected = ConstraintViolationException.class)
    @Transactional
    public void testSaveWhenFailOnDuplicateSetToTrue() {
        // Account2
        // @Indexed(unique = true, failOnDuplicate = true)
        // private String accountNumber;
        Account2 acc1 = new Account2("111-222-333", "Mr George - Current Account 1");
        Account2 acc2 = new Account2("111-222-333", "Mr George - Current Account 2");
        Account2 savedAcc1 = account2Repository.save(acc1);
        Account2 savedAcc2 = account2Repository.save(acc2);
    }

    @Test
    @Transactional
    public void testSaveWhenDefaultFailOnDuplicateSetToTrueAllowsUpdates() {
        // Account2
        // @Indexed(unique = true, failOnDuplicate = true)
        // private String accountNumber;
        Account2 acc1 = new Account2("111-222-333", "Mr George - Current Account 1");
        Account2 savedAcc1 = account2Repository.save(acc1);

        acc1.setName("Mr George - Current Account 2");
        account2Repository.save(savedAcc1);
        // No exception expected!
    }

    /*
    @Ignore("Not catering for explicit overrides at present")
    @Test(expected = ConstraintViolationException.class)
    @Transactional
    public void testSaveWithOverrideFailOnDuplicateSetToTrue() {
        // Account1
        // @Indexed(unique = true, failOnDuplicate = false)
        // private String accountNumber;
        Account1 acc1 = new Account1("111-222-333", "Mr George - Current Account 1");
        Account1 acc2 = new Account1("111-222-333", "Mr George - Current Account 2");
        Account1 savedAcc1 = account1Repository.save(acc1,true);
        Account1 savedAcc2 = account1Repository.save(acc2,true);
    }
    */


}
