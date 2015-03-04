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
package org.springframework.data.neo4j.annotation.relatedto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.Assert.assertSame;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:related-to-test-context.xml"})
@DirtiesContext
@Transactional
public class BiDirectionalRelatedToViaWithSingleFetchMappingTests {
    @NodeEntity
    static class TestEntitySingle {
        @GraphId Long id;
        @RelatedTo(type="test",direction = Direction.INCOMING) TestEntitySingle directParent;
        @RelatedToVia(type="test") Set<TestRelationshipSingle> kids;
        @Fetch
        @RelatedToVia(type="test",direction = Direction.INCOMING) TestRelationshipSingle parent;
    }

    @RelationshipEntity(type="test")
    static class TestRelationshipSingle {
        @GraphId Long id;
        @Fetch
        @StartNode TestEntitySingle parent;
        @Fetch
        @EndNode TestEntitySingle kid;

        TestRelationshipSingle() {
        }

        public TestRelationshipSingle(TestEntitySingle parent, TestEntitySingle kid) {
            this.parent=parent;
            this.kid=kid;
        }
    }

    @Autowired
    Neo4jTemplate template;
    @Test
    public void testLoadBidirectionalRelationship() throws Exception {
        TestEntitySingle one = template.save(new TestEntitySingle());
        TestEntitySingle kid = template.save(new TestEntitySingle());
        TestEntitySingle kid2 = template.save(new TestEntitySingle());
        TestRelationshipSingle rel1 = template.save(new TestRelationshipSingle(one, kid));
        template.save(new TestRelationshipSingle(one,kid2));

        TestEntitySingle anotherOne = template.findOne(kid.id, TestEntitySingle.class);
        assertSame(rel1.id, anotherOne.parent.id);
        assertSame(anotherOne, anotherOne.parent.kid);
        assertSame(one.id, anotherOne.parent.parent.id);
        assertSame(anotherOne.directParent, anotherOne.parent.parent);
    }
}
