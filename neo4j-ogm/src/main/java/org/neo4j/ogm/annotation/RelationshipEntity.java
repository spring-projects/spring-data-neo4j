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

package org.neo4j.ogm.annotation;

import java.lang.annotation.*;

/**
 * Identifies a domain entity as being backed by a relationship in the graph.
 *
 * This annotation is always needed for relationship-backed entities.
 *
 * The type attribute supplies the relatoionship-type in the graph, and
 * can be omitted if the domain entity's simple class name matches
 * exactly the relationship type.
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RelationshipEntity {

    static final String CLASS = "org.neo4j.ogm.annotation.RelationshipEntity";
    static final String TYPE = "type";

    String type() default "";
}
