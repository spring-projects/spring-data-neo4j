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
package org.springframework.data.neo4j.annotation.relatedto;

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
public class BiDirectionalRelatedToViaMappingTests {
    @NodeEntity
    static class TestEntityBiDirectional {
        @GraphId Long id;
        @RelatedTo(type="test",direction = Direction.INCOMING) TestEntityBiDirectional directParent;
        @RelatedToVia(type="test") Set<TestRelationshipBiDirectional> kids;
        @RelatedToVia(type="test",direction = Direction.INCOMING) TestRelationshipBiDirectional parent;
    }

    @RelationshipEntity(type="test")
    static class TestRelationshipBiDirectional {
        @GraphId Long id;
        @Fetch
        @StartNode TestEntityBiDirectional parent;
        @Fetch
        @EndNode TestEntityBiDirectional kid;

        TestRelationshipBiDirectional() {
        }

        public TestRelationshipBiDirectional(TestEntityBiDirectional parent, TestEntityBiDirectional kid) {
            this.parent=parent;
            this.kid=kid;
        }
    }

    @Autowired
    Neo4jTemplate template;
    @Test
    public void testLoadBidirectionalRelationship() throws Exception {
        TestEntityBiDirectional one = template.save(new TestEntityBiDirectional());
        TestEntityBiDirectional kid = template.save(new TestEntityBiDirectional());
        TestEntityBiDirectional kid2 = template.save(new TestEntityBiDirectional());
        TestRelationshipBiDirectional rel1 = template.save(new TestRelationshipBiDirectional(one, kid));
        template.save(new TestRelationshipBiDirectional(one,kid2));

        TestEntityBiDirectional anotherOne = template.findOne(kid.id, TestEntityBiDirectional.class);
        assertSame(rel1.id, anotherOne.parent.id);
        template.fetch(anotherOne.parent);
        assertSame(anotherOne.id, anotherOne.parent.kid.id); // todo missing cache assertSame(anotherOne, anotherOne.parent.kid);
        assertSame(one.id, anotherOne.parent.parent.id);
        assertSame(anotherOne.directParent.id, anotherOne.parent.parent.id); // todo missing cache assertSame(anotherOne.p2, anotherOne.parent.parent);
    }
}
