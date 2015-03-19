/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.session;

import org.neo4j.ogm.model.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Utils {

    public static final Map<String, Object> map(final Object... keysAndValues) {
        return new HashMap<String, Object>() {
            {
                for (int i = 0; i < keysAndValues.length; i+=2 ) {
                    put(String.valueOf(keysAndValues[i]), keysAndValues[i+1]);
                }
            }
        };
    }

    public static final Map<String, Object> mapCollection(final String collectionName, final Collection<Property<String, Object>> properties) {

        return new HashMap<String, Object>() {
            {
                final Map<String, Object> values = new HashMap<>();
                for (Property<String, Object> property : properties) {
                    String key = property.getKey();
                    Object value = property.asParameter();
                    if (value != null) {
                        values.put(key, value);
                    }
                }
                put(collectionName, values);
            }
        };
    }

    public static int size(Iterable<?> iterable) {
        return (iterable instanceof Collection)
                       ? ((Collection<?>) iterable).size()
                       : size(iterable.iterator());
    }

    public static int size(Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }
}
