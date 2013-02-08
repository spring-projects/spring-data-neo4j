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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

/**
 * @author mh
 * @since 13.06.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})
@Transactional
public class NodeEntityQueryTests extends EntityTestBase {
    private TestTeam testTeam;
    private Person michael;

    @Before
    public void setUp() throws Exception {
        testTeam = new TestTeam(neo4jTemplate);
        testTeam.createSDGTeam();
        michael = testTeam.michael;
    }

    @SuppressWarnings("unchecked")
    @Test
    @Transactional
    public void testQueryVariableRelationshipSingleResult() throws Exception {
        final Collection<Map<String,Object>> result = IteratorUtil.asCollection(michael.getOtherTeamMemberData());
        assertThat(result,hasItems(testTeam.simpleRowFor(testTeam.emil, "member"), testTeam.simpleRowFor(testTeam.david, "member")));
    }

    @Test
    public void testQueryVariableRelationshipIterableResult() throws Exception {
        final Collection<Person> result = IteratorUtil.asCollection(michael.getOtherTeamMembers());

        assertThat(result,hasItems(testTeam.david,testTeam.emil));

    }

    @Test
    public void testQueryVariableSingleResultPerson() throws Exception {
        assertEquals(testTeam.emil,michael.getBossByQuery());
    }
    @Test
    public void testQueryVariableStringResult() throws Exception {
        assertEquals(testTeam.emil.getName(),michael.getBossName());
    }

    @Test
    public void testEmptyQueryReturnsZero() {
        final Group group = neo4jTemplate.save(new Group());
        assertEquals("empty result leads to zero",(Long)0L, group.getMemberCount());
    }
}
