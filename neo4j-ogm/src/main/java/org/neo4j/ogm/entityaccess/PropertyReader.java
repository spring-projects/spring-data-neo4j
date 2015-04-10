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

/**
 * Simple interface through which a particular property of a given object can be read.
 *
 *  @author Adam George
 */
public interface PropertyReader {

    /**
     * Retrieves the property name as it would be written to the node or relationship in the graph database.
     *
     * @return The name of the property to write to the graph database property container
     */
    String propertyName();

    /**
     * Reads the value corresponding to this property from the given object.
     *
     * @param instance The instance from which to read the property value
     * @return The value of the property, which may be <code>null</code>
     * @throws RuntimeException if there's an error reading the property or if it's not found on the given object
     */
    Object read(Object instance);

}
