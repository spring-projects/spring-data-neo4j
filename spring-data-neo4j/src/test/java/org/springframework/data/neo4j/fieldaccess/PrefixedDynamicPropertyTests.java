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
package org.springframework.data.neo4j.fieldaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.hasItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.neo4j.helpers.collection.IteratorUtil;

public class PrefixedDynamicPropertyTests {
    private static final String VALUE = "value";
    private static final String VALUE2 = "value2";
    private static final String DEFAULT_VALUE = "default_value";
    private static final String KEY = "key";
    private static final String KEY2 = "key2";
    private static final String PREFIX = "foo";
    private static final String PREFIXED_KEY = PREFIX + "-" + KEY;
    private static final String PREFIXED_KEY2 = PREFIX + "-" + KEY2;
    PrefixedDynamicProperties props = new PrefixedDynamicProperties(PREFIX);

    private <T> List<T> toList(Iterable<T> it) {
        return IteratorUtil.addToCollection(it, new ArrayList<T>());
    }

    @Test
    public void setProperty() {
        assertFalse(props.hasProperty(KEY));
        assertFalse(props.hasPrefixedProperty(PREFIXED_KEY));
        assertEquals(props.getProperty(KEY, DEFAULT_VALUE), DEFAULT_VALUE);

        props.setProperty(KEY, VALUE);

        assertTrue(props.hasProperty(KEY));
        assertTrue(props.hasPrefixedProperty(PREFIXED_KEY));
        assertEquals(props.getProperty(KEY), VALUE);
        assertEquals(props.getPrefixedProperty(PREFIXED_KEY), VALUE);
        assertEquals(props.getProperty(KEY, DEFAULT_VALUE), VALUE);

        props.setProperty(KEY2, VALUE);

        assertThat(toList(props.getPropertyKeys()), hasItems(KEY, KEY2));
        assertThat(toList(props.getPrefixedPropertyKeys()), hasItems(PREFIXED_KEY, PREFIXED_KEY2));

        props.removeProperty(KEY);

        assertFalse(props.hasProperty(KEY));
        assertFalse(props.hasPrefixedProperty(PREFIXED_KEY));
    }

    @Test
    public void setPropertyIfPrefixed() {
        props.setPropertyIfPrefixed(KEY, VALUE);
        assertFalse(props.hasProperty(KEY));
        assertFalse(props.hasPrefixedProperty(PREFIXED_KEY));

        props.setPropertyIfPrefixed(PREFIXED_KEY, VALUE);
        assertTrue(props.hasProperty(KEY));
        assertTrue(props.hasPrefixedProperty(PREFIXED_KEY));
    }

    @Test
    public void setRawProperty() {
        props.setPrefixedProperty(PREFIXED_KEY, VALUE);
        assertTrue(props.hasPrefixedProperty(PREFIXED_KEY));
        assertTrue(props.hasProperty(KEY));
    }

    @Test
    public void asMap() {
        props.setProperty(KEY, VALUE);
        props.setProperty(KEY2, VALUE2);

        Map<String, Object> map = props.asMap();
        assertThat(map.keySet(), hasItems(KEY, KEY2));
        List<String> values = new ArrayList<String>(map.size());
        for (String key : map.keySet()) {
            values.add((String) map.get(key));
        }
        assertThat(values, hasItems(VALUE, VALUE2));
    }

    @Test
    public void createFrom() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(KEY, VALUE);
        m.put(KEY2, VALUE2);
        DynamicProperties p = props.createFrom(m);
        assertTrue(p.hasProperty(KEY));
        assertTrue(p.hasProperty(KEY2));
    }
}
