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
package org.springframework.data.neo4j.annotation.relatedto.bidirectional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.Assert.assertSame;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:related-to-test-context.xml"})
@Transactional
public class BiDirectionalRelatedToViaWithSingleFetchMappingTests {
    @NodeEntity
    static class TestEntity {
        @GraphId Long id;
        @RelatedTo(type="test",direction = Direction.INCOMING) TestEntity directParent;
        @RelatedToVia(type="test") Set<TestRelationship> kids;
        @Fetch
        @RelatedToVia(type="test",direction = Direction.INCOMING) TestRelationship parent;
    }

    @RelationshipEntity(type="test")
    static class TestRelationship {
        @GraphId Long id;
        @Fetch
        @StartNode TestEntity parent;
        @Fetch
        @EndNode TestEntity kid;

        TestRelationship() {
        }

        public TestRelationship(TestEntity parent, TestEntity kid) {
            this.parent=parent;
            this.kid=kid;
        }
    }

    @Autowired
    Neo4jTemplate template;
    @Test
    public void testLoadBidirectionalRelationship() throws Exception {
        TestEntity one = template.save(new TestEntity());
        TestEntity kid = template.save(new TestEntity());
        TestEntity kid2 = template.save(new TestEntity());
        TestRelationship rel1 = template.save(new TestRelationship(one, kid));
        template.save(new TestRelationship(one,kid2));

        TestEntity anotherOne = template.findOne(kid.id, TestEntity.class);
        assertSame(rel1.id, anotherOne.parent.id);
        assertSame(anotherOne, anotherOne.parent.kid);
        assertSame(one.id, anotherOne.parent.parent.id);
        assertSame(anotherOne.directParent, anotherOne.parent.parent);
    }
}
