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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertSame;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:related-to-test-context.xml"})
@Transactional
public class BiDirectionalMappingTest {
    @NodeEntity
    static class TestEntityOne {
        @GraphId Long id;
        @Fetch
        @RelatedTo TestEntityTwo test;
    }
    @NodeEntity
    static class TestEntityTwo {
        @GraphId Long id;
        @Fetch
        @RelatedTo(direction = Direction.INCOMING) TestEntityOne test;
    }

    @Autowired
    Neo4jTemplate template;
    @Test
    public void testLoadBidirectional() throws Exception {
        TestEntityOne one = template.save(new TestEntityOne());
        TestEntityTwo entity = new TestEntityTwo();
        entity.test = one;
        template.save(entity);

        TestEntityOne anotherOne = template.findOne(one.id, TestEntityOne.class);
        assertSame(anotherOne, anotherOne.test.test);
    }
}
