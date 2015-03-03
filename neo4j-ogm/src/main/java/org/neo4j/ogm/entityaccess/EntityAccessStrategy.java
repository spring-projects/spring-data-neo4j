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

package org.neo4j.ogm.entityaccess;

import org.neo4j.ogm.metadata.info.ClassInfo;

import java.util.Collection;

/**
 * Implements the logic to determine how entities should be accessed in both reading and writing scenarios.
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
