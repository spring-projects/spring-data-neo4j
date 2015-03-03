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
 * Establishes the mapping between a domain entity attribute
 * and a node or relationship property in the graph.
 *
 * This annotation is not needed if the mapping can be
 * derived by the OGM, according to the following
 * heuristics:
 *
 *      an accessor method
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
public @interface Property {

    static final String CLASS = "org.neo4j.ogm.annotation.Property";
    static final String NAME = "name";

    String name() default "";
}
