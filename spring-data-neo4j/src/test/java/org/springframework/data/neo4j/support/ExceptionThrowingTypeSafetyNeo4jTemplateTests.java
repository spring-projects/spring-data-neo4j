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
import org.springframework.data.neo4j.mapping.InvalidEntityTypeException;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author spaetzold
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:template-config-context-with-type-safety-throwing-exception.xml"})
public class ExceptionThrowingTypeSafetyNeo4jTemplateTests extends EntityTestBase {

    private Neo4jOperations neo4jOperations;

    @Before
    public void setUp() throws Exception {
        createTeam();
        neo4jOperations = template;
    }

    @Test(expected = InvalidEntityTypeException.class)
    @Transactional
    public void testFindOneReturnsNull() throws Exception {
        neo4jOperations.findOne(testTeam.michael.getId(), Group.class);
    }
}
