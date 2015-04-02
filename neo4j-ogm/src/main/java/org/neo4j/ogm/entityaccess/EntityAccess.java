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

package org.neo4j.ogm.entityaccess;

import java.lang.reflect.Array;
import java.util.*;

/**
 * @author Vince Bickers
 */
public abstract class EntityAccess implements PropertyWriter, RelationalWriter {


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object merge(Class<?> parameterType, Iterable<?> newValues, Object[] currentValues) {
        if (currentValues != null) {
            return merge(parameterType, newValues, Arrays.asList(currentValues));
        } else {
            return merge(parameterType, newValues, new ArrayList());
        }
    }


    /**
     * Merges the contents of <em>collection</em> with <em>hydrated</em> ensuring no duplicates and returns the result as an
     * instance of the given parameter type.
     *
     * @param parameterType The type of Iterable or array to return
     * @param newValues The objects to merge into a collection of the given parameter type, which may not necessarily be of a
     *        type assignable from <em>parameterType</em> already
     * @param currentValues The Iterable to merge into, which may be <code>null</code> if a new collection needs creating
     * @return The result of the merge, as an instance of the specified parameter type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object merge(Class<?> parameterType, Iterable<?> newValues, Iterable<?> currentValues) {

        if (parameterType.isArray()) {
            Class type = parameterType.getComponentType();
            List<Object> objects = new ArrayList<>(union(newValues, currentValues));

            Object array = Array.newInstance(type, objects.size());
            for (int i = 0; i < objects.size(); i++) {
                Array.set(array, i, objects.get(i));
            }
            return array;
        }

        // hydrated is unusable at this point so we can just set the other collection if it's compatible
        if (parameterType.isAssignableFrom(newValues.getClass())) {
            return newValues;
        }

        // create the desired type of collection and use it for the merge
        Collection newCollection = createCollection(parameterType, newValues, currentValues);
        if (newCollection != null) {
            return newCollection;
        }

        throw new RuntimeException("Unsupported: " + parameterType.getName());
    }

    private static Collection<?> createCollection(Class<?> parameterType, Iterable<?> collection, Iterable<?> hydrated) {
        if (Vector.class.isAssignableFrom(parameterType)) {
            return new Vector<>(union(collection, hydrated));
        }
        if (List.class.isAssignableFrom(parameterType)) {
            return new ArrayList<>(union(collection, hydrated));
        }
        if (Set.class.isAssignableFrom(parameterType)) {
            return union(collection, hydrated);
        }
        return null;
    }

    private static Collection<Object> union(Iterable<?> collection, Iterable<?> hydrated) {
        Collection<Object> result = new ArrayList<>();
        for (Object object : collection) {
            result.add(object);
        }
        if (hydrated != null) {
            for (Object object : hydrated) {
                if (!result.contains( object )) {
                    result.add(object);
                }
            }
        }
        return result;
    }
}
