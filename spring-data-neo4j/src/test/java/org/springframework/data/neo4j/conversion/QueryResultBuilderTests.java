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
package org.springframework.data.neo4j.conversion;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.data.neo4j.conversion.QueryResultBuilder.from;

/**
 * @author mh
 * @since 11.11.11
 */
public class QueryResultBuilderTests {

    private final DefaultConverter defaultConverter = new DefaultConverter();
    private QueryResultBuilder<Integer> result = new QueryResultBuilder<Integer>(asList(1, 2, 3));

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testToWithConverter() throws Exception {
    }

    @Test
    public void testSingle() throws Exception {
        assertThat(from(1).single(), is(1));
    }

    @Test
    public void testSingleOrNull() throws Exception {

    }

    @Test
    public void testTo() throws Exception {

    }

    @Test
    public void testAs() throws Exception {

    }

    @Test
    public void testHandle() throws Exception {

    }

    @Test
    public void testIterator() throws Exception {

    }
}
