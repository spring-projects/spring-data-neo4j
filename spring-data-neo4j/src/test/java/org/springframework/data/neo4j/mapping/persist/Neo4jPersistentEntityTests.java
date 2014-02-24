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
package org.springframework.data.neo4j.mapping.persist;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.model.Being;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.mapping.Neo4jPersistentEntityImpl;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 20.02.12
 */
public class Neo4jPersistentEntityTests {

    private Neo4jMappingContext mappingContext;
    private Neo4jPersistentEntityImpl<?> personType;

    @Before
    public void setUp() throws Exception {
        mappingContext = new Neo4jMappingContext();
        mappingContext.setInitialEntitySet(Collections.singleton(Person.class));
        personType = mappingContext.getPersistentEntity(Person.class);
    }

    @Test
    public void testDirectEntityType() {
        final StoredEntityType entityType = personType.getEntityType();
        assertEquals("Person",entityType.getAlias());
        assertEquals(Person.class, entityType.getType());
        assertEquals(1,entityType.getSuperTypes().size());
        final StoredEntityType superType = IteratorUtil.single(entityType.getSuperTypes());
        assertEquals(Being.class, superType.getType());
        assertEquals("Being", superType.getAlias());
        assertEquals(0,superType.getSuperTypes().size());
    }
}
