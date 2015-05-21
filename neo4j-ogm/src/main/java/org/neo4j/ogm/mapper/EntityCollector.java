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

package org.neo4j.ogm.mapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to help group elements of a common type into a single collection by relationship type to be set on an owning object.
 * The ability to set a collection of instances on an owning entity based on the type of instance is insufficient as described in DATAGRAPH-637.
 * The relationship type is required to be able to correctly determine which instances are to be set for which property of the node entity.
 * @author Adam George
 * @author Luanne Misquitta
 */
class EntityCollector {

    private final Logger logger = LoggerFactory.getLogger(EntityCollector.class);
    private final Map<Object, Map<String, Set<Object>>> relationshipTypes = new HashMap<>();

    /**
     * Adds the given collectible element into a collection based on relationship type ready to be set on the given owning type.
     *
     * @param owningEntity The type on which the collection is to be set
     * @param collectibleElement The element to add to the collection that will eventually be set on the owning type
     * @param relationshipType The relationship type that this collection corresponds to
     */
    public void recordTypeRelationship(Object owningEntity, Object collectibleElement, String relationshipType) {
        if (this.relationshipTypes.get(owningEntity) == null) {
            this.relationshipTypes.put(owningEntity, new HashMap<String, Set<Object>>());
        }
        if (this.relationshipTypes.get(owningEntity).get(relationshipType) == null) {
            this.relationshipTypes.get(owningEntity).put(relationshipType, new HashSet<Object>());
        }
        this.relationshipTypes.get(owningEntity).get(relationshipType).add(collectibleElement);
    }

    /**
     * @return All the owning types that have been added to this {@link EntityCollector}
     */
    public Iterable<Object> getOwningTypes() {
        return this.relationshipTypes.keySet();
    }

    /**
     * Retrieves all relationship types for which collectibles can be set on an owning object
     *
     * @param owningObject the owning object
     * @return all relationship types owned by the owning object
     */
    public Iterable<String> getOwningRelationshipTypes(Object owningObject) {
        return this.relationshipTypes.get(owningObject).keySet();
    }

    /**
     * A set of collectibles based on relationship type for an owning object
     *
     * @param owningObject the owning object
     * @param relationshipType the relationship type
     * @return set of instances to be set for the relationship type on the owning object
     */
    public Set<Object> getCollectiblesForOwnerAndRelationshipType(Object owningObject, String relationshipType) {
        return this.relationshipTypes.get(owningObject).get(relationshipType);
    }

    /**
     * Get the type of the instance to be set on the owner object
     *
     * @param owningObject the owner object
     * @param relationshipType the relationship type
     * @return type of instance
     */
    public Class getCollectibleTypeForOwnerAndRelationshipType(Object owningObject, String relationshipType) {
        return this.relationshipTypes.get(owningObject).get(relationshipType).iterator().next().getClass();
    }
}
