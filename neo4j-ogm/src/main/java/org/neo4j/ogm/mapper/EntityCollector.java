/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility to help group elements of a common type into a single collection to be set on an owning object.
 */
class EntityCollector {

    private final Logger logger = LoggerFactory.getLogger(EntityCollector.class);
    private final Map<Object, Map<Class<?>, Set<Object>>> typeRelationships = new HashMap<>();

    /**
     * Adds the given collectible element into a collection ready to be set on the given owning type.
     *
     * @param owningEntity The type on which the collection is to be set
     * @param collectibleElement The element to add to the collection that will eventually be set on the owning type
     */
    public void recordTypeRelationship(Object owningEntity, Object collectibleElement) {
        Map<Class<?>, Set<Object>> handled = this.typeRelationships.get(owningEntity);
        if (handled == null) {
            this.typeRelationships.put(owningEntity, handled = new HashMap<>());
        }
        Class<?> type = collectibleElement.getClass();
        Set<Object> objects = handled.get(type);
        if (objects == null) {
            handled.put(type, objects = new HashSet<>());
        }
        objects.add(collectibleElement);
    }

    /**
     * @return All the owning types that have been added to this {@link EntityCollector}
     */
    public Iterable<Object> getOwningTypes() {
        return this.typeRelationships.keySet();
    }

    /**
     * Retrieves the type map that corresponds to the given owning object, which is a mapping between a type and
     * a collection of instances of that type to set on the owning object.
     *
     * @param owningObject The object for which to retrieve the type map
     * @return The type map for the given object or an empty map if it's unknown, never <code>null</code>
     */
    public Map<Class<?>, Set<Object>> getTypeCollectionMapping(Object owningObject) {
        Map<Class<?>, Set<Object>> handled = this.typeRelationships.get(owningObject);
        return handled != null ? handled : Collections.<Class<?>, Set<Object>> emptyMap();
    }

}
