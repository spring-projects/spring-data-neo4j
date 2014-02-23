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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.SubGroup;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD, hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)

public class FailingIndexTests extends EntityTestBase {

    private static final String NAME_VALUE = "aName";
    private static final String NAME_VALUE2 = "aSecondName";

    @Test
    @Transactional
    public void testFindGroupByInstanceIndex() {
        Group group = persist(new SubGroup());
        group.setIndexLevelName("indexLevelNameValue");
        Index<Node> subGroupIndex = neo4jTemplate.getIndex(SubGroup.class);
        final Node found = subGroupIndex.get("indexLevelName", "indexLevelNameValue").getSingle();
        final SubGroup foundEntity = neo4jTemplate.createEntityFromState(found, SubGroup.class, neo4jTemplate.getMappingPolicy(SubGroup.class));
        assertEquals(group, foundEntity);
    }

    @Test
    @Transactional
    public void testDontFindGroupByNonIndexedFieldWithAnnotation() {
        Group group = persist(new Group());
        group.setUnindexedName("value-unindexedName");
        final Group found = this.groupRepository.findByPropertyValue("unindexedName", "value-unindexedName");
        assertNull(found);
    }

    @Test
    @Transactional
    public void testCanIndexIntFieldsOnRelationshipEntities() {
        Person p = persistedPerson(NAME_VALUE, 35);
        Person p2 = persistedPerson(NAME_VALUE2, 25);
        Friendship friendship = p.knows(p2);
        friendship.setYears(1);
        GraphRepository<Friendship> friendshipFinder = neo4jTemplate.repositoryFor(Friendship.class);
        assertEquals(friendship, friendshipFinder.findByPropertyValue("Friendship.years", 1));
    }
}
