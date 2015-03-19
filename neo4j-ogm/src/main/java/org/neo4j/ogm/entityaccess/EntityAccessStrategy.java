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

import org.neo4j.ogm.metadata.info.ClassInfo;

import java.util.Collection;

/**
 * Implements the logic to determine how entities should be accessed in both reading and writing scenarios.
 *
 * @author Adam George
 */
public interface EntityAccessStrategy {

    PropertyReader getIdentityPropertyReader(ClassInfo classInfo);

    PropertyReader getPropertyReader(ClassInfo classInfo, String propertyName);
    PropertyWriter getPropertyWriter(ClassInfo classInfo, String propertyName);

    RelationalWriter getRelationalWriter(ClassInfo classInfo, String relationshipType, Object parameter);
    RelationalReader getRelationalReader(ClassInfo classInfo, String relationshipType);

    RelationalWriter getIterableWriter(ClassInfo classInfo, Class<?> parameterType);
    RelationalReader getIterableReader(ClassInfo classInfo, Class<?> parameterType);

    Collection<RelationalReader> getRelationalReaders(ClassInfo classInfo);
    Collection<PropertyReader> getPropertyReaders(ClassInfo classInfo);

    RelationalReader getEndNodeReader(ClassInfo relationshipEntityClassInfo);
    RelationalReader getStartNodeReader(ClassInfo classInfo);
}
