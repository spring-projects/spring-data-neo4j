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
package org.springframework.data.neo4j.support.query;

import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

public class QueryParameterConverterTests {
    private static final QueryParameterConverter queryParameterConverter = new QueryParameterConverter();

    @Test
    public void shouldConvertNullToEmptyMap() throws Exception {
        assertThat(queryParameterConverter.convert(null), is(Collections.<String, Object>emptyMap()));
    }

    @Test
    public void shouldConvertEmptyMapToEmptyMap() throws Exception {
        assertThat(queryParameterConverter.convert(new HashMap<String, Object>()), is(Collections.<String, Object>emptyMap()));
    }

    @Test
    public void shouldConvertSingleNullValue() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", null);

        assertThat(queryParameterConverter.convert(parameters), is(parameters));
    }

    @Test
    public void shouldConvertSinglePrimitiveValue() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", "bar");
        parameters.put("baz", 42l);

        assertThat(queryParameterConverter.convert(parameters), is(parameters));
    }

    enum Suit {
        SPADE, HEART
    }

    @Test
    public void shouldConvertSingleEnumValueUsingDefaultConversion() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", Suit.HEART);

        Map<String, Object> expectedParameters = new HashMap<String, Object>();
        expectedParameters.put("foo", "HEART");

        assertThat(queryParameterConverter.convert(parameters), is(expectedParameters));
    }

    @Test
    public void shouldConvertSingleDateValueUsingDefaultConversion() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", new Date(42));

        Map<String, Object> expectedParameters = new HashMap<String, Object>();
        expectedParameters.put("foo", "42");

        assertThat(queryParameterConverter.convert(parameters), is(expectedParameters));
    }

    @Test
    public void shouldConvertArrayOfString() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", new String[]{"bar", "baz"});

        assertArrayEquals((String[]) queryParameterConverter.convert(parameters).get("foo"), (String[]) parameters.get("foo"));
    }

    @Test
    public void shouldConvertArrayOfPrimitive() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", new long[]{42, 87});

        assertArrayEquals((long[]) queryParameterConverter.convert(parameters).get("foo"), (long[]) parameters.get("foo"));
    }

    @Test
    public void shouldConvertArrayOfEnum() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", new Suit[]{Suit.HEART, Suit.SPADE});

        assertArrayEquals((String[]) queryParameterConverter.convert(parameters).get("foo"), new String[]{"HEART", "SPADE"});
    }

    @Test
    public void shouldConvertArrayOfDate() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", new Date[]{new Date(42), new Date(87)});

        assertArrayEquals((String[]) queryParameterConverter.convert(parameters).get("foo"), new String[]{"42", "87"});
    }

    @Test
    public void shouldConvertIterableOfString() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", asList("bar", "baz"));

        assertThat(queryParameterConverter.convert(parameters), is(equalTo(parameters)));
    }

    @Test
    public void shouldConvertIterableOfPrimitive() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", asList(42l, 87l));

        assertThat(queryParameterConverter.convert(parameters), is(equalTo(parameters)));
    }

    @Test
    public void shouldConvertIterableOfEnum() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", asList(Suit.HEART, Suit.SPADE));

        assertThat((List<String>) queryParameterConverter.convert(parameters).get("foo"), is(equalTo(asList("HEART", "SPADE"))));
    }

    @Test
    public void shouldConvertIterableOfDate() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", asList(new Date(42), new Date(87)));

        assertThat((List<String>) queryParameterConverter.convert(parameters).get("foo"), is(equalTo(asList("42", "87"))));
    }

    @Test
    public void shouldConvertArrayOfArray() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", new Object[]{new String[]{"bar", "baz"}, new Long[]{42l}, new Date[]{new Date(87)}});

        Object[] foo1 = (Object[]) queryParameterConverter.convert(parameters).get("foo");
        Object[] foo2 = (Object[]) parameters.get("foo");
        assertArrayEquals((String[]) foo1[0], (String[]) foo2[0]);
        assertArrayEquals((Long[]) foo1[1], (Long[]) foo2[1]);
        assertArrayEquals((String[]) foo1[2], new String[]{"87"});
    }

    @Test
    public void shouldConvertIterableOfArray() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", asList(new String[]{"bar", "baz"}, new long[]{87}, new Suit[]{Suit.HEART, Suit.SPADE}));

        List<Object> foo1 = (List<Object>) queryParameterConverter.convert(parameters).get("foo");
        List<Object> foo2 = (List<Object>) parameters.get("foo");
        assertArrayEquals((String[]) foo1.get(0), (String[]) foo2.get(0));
        assertArrayEquals((long[]) foo1.get(1), (long[]) foo2.get(1));
        assertArrayEquals((String[]) foo1.get(2), new String[]{"HEART", "SPADE"});
    }

    class Foo {

    }

    @Test
    public void shouldNotConvertUnknownTypes() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("foo", new Foo());
        parameters.put("bar", asList(new Foo(), new Foo()));

        assertThat(queryParameterConverter.convert(parameters), is(parameters));
    }
}
