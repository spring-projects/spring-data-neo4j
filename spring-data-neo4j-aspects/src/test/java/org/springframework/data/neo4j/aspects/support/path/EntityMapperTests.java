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

package org.springframework.data.neo4j.aspects.support.path;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.support.EntityTestBase;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.support.path.EntityMapper;
import org.springframework.data.neo4j.support.path.NodePath;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mh
 * @since 26.02.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})
public class EntityMapperTests extends EntityTestBase {

    @Test
    @Transactional
    public void entityMapperShouldForwardEntityPath() throws Exception {
        Person michael = persist(new Person("Michael", 36));
        EntityMapper<Person, Person, String> mapper = new EntityMapper<Person, Person, String>(neo4jTemplate) {
            @Override
            public String mapPath(EntityPath<Person, Person> entityPath) {
                return entityPath.<Person>startEntity().getName();
            }
        };
        String name = mapper.mapPath(new NodePath(getNodeState(michael)));
        Assert.assertEquals(michael.getName(), name);
    }
}
