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
package org.springframework.data.neo4j.repository.query;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;

/**
 * Unit tests for {@link VariableContext}.
 * 
 * @author Oliver Gierke
 */
public class VariableContextUnitTests {

    Neo4jMappingContext mappingContext;
    VariableContext context;

    @Before
    public void setUp() {

        mappingContext = new Neo4jMappingContext();
        context = new VariableContext();
    }

    @Test
    public void nameForSimplePropertyIsOwner() {
        assertThat(context.getVariableFor(getPath("age")), is("person"));
    }
    @Test
    public void nameForPathViaEntityIsOwnerAndEntity() {
        assertThat(context.getVariableFor(getPath("group.members")), is("person_group_members"));
    }

    @Test
    public void nameForEntityIsLowercaseSimpleClassName() {
        assertThat(context.getVariableFor(mappingContext.getPersistentEntity(Person.class)),is("person"));
    }
    @Test
    public void nameForEntityPropertyIsOwnerAndEntity() {
        final PersistentPropertyPath<Neo4jPersistentProperty> gropPath = getPath("group");
        assertThat(context.getVariableFor(gropPath),is("person_group"));
    }

    private PersistentPropertyPath<Neo4jPersistentProperty> getPath(String expression) {

        PropertyPath path = PropertyPath.from(expression, Person.class);
        return mappingContext.getPersistentPropertyPath(path);
    }
}
