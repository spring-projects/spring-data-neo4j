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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.support.conversion.EntityResultConverter;

import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.springframework.data.neo4j.conversion.QueryResultBuilder.from;

/**
 * @author mh
 * @since 11.11.11
 */
public class QueryResultBuilderTests {

    private final DefaultConverter defaultConverter = new DefaultConverter();
    private QueryResultBuilder<Integer> result = new QueryResultBuilder<Integer>(asList(1, 2, 3));

    @MapResult
    interface TestResult {
        Integer getValue();
    }

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
        QueryResultBuilder<Map<String,Object>> builder = new QueryResultBuilder<Map<String,Object>>(Collections.<Map<String,Object>>emptyList(), new EntityResultConverter<Map<String, Object>, Object>(null));
        assertThat(builder.to(TestResult.class).singleOrNull(), is(nullValue()));
    }

    @Test
    public void testTo() throws Exception {

    }

    @Test
    public void testAs() throws Exception {
        Map<String, Object> value1 = map("key", 1);
        Map<String, Object> value2 = map("key", 2);
        QueryResultBuilder<Map<String,Object>> builder = new QueryResultBuilder<Map<String,Object>>(Arrays.asList(value1, value2));
        List<Map<String,Object>> list = builder.as(List.class);
        assertEquals(2,list.size());
        assertThat(list,hasItems(value1,value2));
        Page<Map<String,Object>> page = builder.as(Page.class);
        assertEquals(2,page.getNumberOfElements());
        assertThat(page,hasItems(value1,value2));

        Slice<Map<String,Object>> slice = builder.as(Slice.class);
        assertEquals(2,slice.getNumberOfElements());
        assertThat(page,hasItems(value1,value2));
    }

    @Test
    public void testHandle() throws Exception {

    }

    @Test
    public void testIterator() throws Exception {

    }
}
