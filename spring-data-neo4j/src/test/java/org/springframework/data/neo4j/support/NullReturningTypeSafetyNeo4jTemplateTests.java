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
package org.springframework.data.neo4j.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.NotFoundException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.neo4j.mapping.InvalidEntityTypeException;
import org.springframework.data.neo4j.model.*;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author spaetzold
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:template-config-context-with-type-safety-returning-null.xml"})
public class NullReturningTypeSafetyNeo4jTemplateTests extends EntityTestBase {

    private Neo4jOperations neo4jOperations;

    @Before
    public void setUp() throws Exception {
        createTeam();
        neo4jOperations = template;
    }

    @Test
    @Transactional
    public void testFindOneWithWrongTypeReturnsNull() throws Exception {
        final Group found = neo4jOperations.findOne(testTeam.michael.getId(), Group.class);
        assertNull(found);
    }

    @Test
    @Transactional
    public void testFindOneWithRightTypeReturnsPerson() throws Exception {
        final Person found = neo4jOperations.findOne(testTeam.michael.getId(), Person.class);
        assertNotNull(found);
    }

    @Test(expected = DataRetrievalFailureException.class)
    @Transactional
    public void testFindOneWithNonExistingIdThrowsDataRetrievalFailureException() throws Exception {
        neo4jOperations.findOne(Long.MAX_VALUE, Person.class);
    }

    @Test
    @Transactional
    public void testFindOneWithAbstractWrongTypeReturnsNull() throws Exception {
        AbstractNodeEntity entity = neo4jOperations.findOne(testTeam.michael.getId(), AbstractNodeEntity.class);
        assertNull(entity);
    }

    @Test
    @Transactional
    public void testFindOneWithConcreteEntityAndConcreteTypeReturnsConcreteEntity() throws Exception {
        AbstractNodeEntity origConcrete1NodeEntity = neo4jOperations.save(new Concrete1NodeEntity("concrete1"));
        AbstractNodeEntity readConcrete1NodeEntity = neo4jOperations.findOne(origConcrete1NodeEntity.id, Concrete1NodeEntity.class);
        assertNotNull(readConcrete1NodeEntity);
        assertEquals(origConcrete1NodeEntity,readConcrete1NodeEntity);
    }

    @Test
    @Transactional
    public void testFindOneWithConcreteEntityAndAbstractTypeReturnsConcreteEntity() throws Exception {
        Concrete1NodeEntity origConcrete1NodeEntity = neo4jOperations.save(new Concrete1NodeEntity("concrete1"));
        Concrete1NodeEntity readConcrete1NodeEntity = (Concrete1NodeEntity)neo4jOperations.findOne(origConcrete1NodeEntity.id, AbstractNodeEntity.class);
        assertNotNull(readConcrete1NodeEntity);
        assertEquals(origConcrete1NodeEntity,readConcrete1NodeEntity);
    }

    @Test
    @Transactional
    public void testFindOneWithDifferentConcreteEntitiesReturnsNull() throws Exception {
        Concrete2NodeEntity origConcrete2NodeEntity = neo4jOperations.save(new Concrete2NodeEntity("concrete2"));
        AbstractNodeEntity readConcrete2NodeEntity = neo4jOperations.findOne(origConcrete2NodeEntity.id, Concrete1NodeEntity.class);
        assertNull(readConcrete2NodeEntity);
    }

}
