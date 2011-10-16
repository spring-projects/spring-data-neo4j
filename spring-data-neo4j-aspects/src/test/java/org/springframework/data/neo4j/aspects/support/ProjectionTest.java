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
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Named;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml"})

public class ProjectionTest extends EntityTestBase {

    @Test
    @Transactional
    public void testProjectGroupToNamed() {
        Group group = persist(new Group());
        group.setName("developers");

        Named named = graphDatabaseContext.projectTo(group,Named.class);
        assertEquals("named.name","developers", named.getName());
        assertEquals("nameds node name property","developers", getNodeState(named).getProperty("name"));
    }

}
