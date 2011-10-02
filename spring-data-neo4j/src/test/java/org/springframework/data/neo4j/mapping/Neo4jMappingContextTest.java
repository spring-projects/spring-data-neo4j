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
package org.springframework.data.neo4j.mapping;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.neo4j.Person;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 19.09.11
 */
public class Neo4jMappingContextTest {

    private Neo4jMappingContext mappingContext;
    private Neo4jPersistentEntityImpl<?> personType;

    @Before
    public void setUp() throws Exception {
        mappingContext = new Neo4jMappingContext();
        personType = mappingContext.getPersistentEntity(Person.class);
    }

    @Test
    public void checkGraphIdProperty() {
        final Neo4jPersistentProperty idProperty = personType.getIdProperty();
        assertEquals("graphId", idProperty.getName());
    }

    @Test public void checkNameProperty() {
        final Neo4jPersistentProperty nameProperty = personType.getPersistentProperty("name");
        assertEquals("name",nameProperty.getName());
        assertEquals(String.class,nameProperty.getType());
        assertEquals(true,nameProperty.isIndexed());
        assertEquals(Person.NAME_INDEX,nameProperty.getIndexInfo().getIndexName());
        assertEquals(false,nameProperty.getIndexInfo().isFulltext());
        assertEquals(false,nameProperty.isRelationship());
    }
}
