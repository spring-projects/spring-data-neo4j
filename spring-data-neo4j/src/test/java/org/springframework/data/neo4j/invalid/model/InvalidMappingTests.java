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
package org.springframework.data.neo4j.invalid.model;

import org.junit.Test;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;

public class InvalidMappingTests {
    @NodeEntity
    static class TestEntityRelatedTo {
        @GraphId Long id;
        @RelatedToVia TestEntityRelatedTo test;
    }
    @RelationshipEntity
    static class TestRelationship {
        @GraphId Long id;
    }
    @NodeEntity
    static class TestEntityRelatedToVia {
        @GraphId Long id;
        @RelatedTo TestRelationship test;
    }

    @Test(expected = MappingException.class)
    public void testFailInvalidRelatedTo() throws Exception {
        MappingContext context = new Neo4jMappingContext();
        context.getPersistentEntity(TestEntityRelatedTo.class);
    }
    @Test(expected = MappingException.class)
    public void testFailInvalidRelatedToVia() throws Exception {
        MappingContext context = new Neo4jMappingContext();
        context.getPersistentEntity(TestEntityRelatedToVia.class);
    }
}
