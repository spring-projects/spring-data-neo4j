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

import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:related-to-test-context.xml"})
@Transactional
public class BiDirectionalRelatedToViaWithCollectionFetchMappingTests {
    @NodeEntity
    static class TestEntity {
        @GraphId Long id;
        @Fetch @RelatedToVia(type="test") List<TestRelationship> kids;
        @Fetch @RelatedTo(type="test") List<TestEntity> directKids;
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
        TestEntity parent = template.save(new TestEntity());
        TestEntity kid = template.save(new TestEntity());
        TestEntity kid2 = template.save(new TestEntity());
        TestRelationship rel1 = template.save(new TestRelationship(parent, kid));
        TestRelationship rel2 = template.save(new TestRelationship(parent, kid2));

        TestEntity aParent = template.findOne(parent.id, TestEntity.class);

        List<TestRelationship> kids = aParent.kids;
        assertEquals(2, kids.size());
        TestRelationship kidRel1 = kids.get(0);
        TestRelationship kidRel2 = kids.get(1);
        assertThat(asList(kidRel1.id, kidRel2.id), hasItems(rel1.id, rel2.id));

        assertSame(aParent, kidRel1.parent);
        assertSame(aParent, kidRel2.parent);
        assertThat(asList(kidRel1.kid.id, kidRel2.kid.id), hasItems(kid.id, kid2.id));
        assertThat(aParent.directKids, hasItems(kidRel1.kid, kidRel2.kid));
    }
}
