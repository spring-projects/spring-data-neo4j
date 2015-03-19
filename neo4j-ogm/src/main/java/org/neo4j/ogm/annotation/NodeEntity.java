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

package org.neo4j.ogm.annotation;

import java.lang.annotation.*;


/**
 * Identifies a domain entity as being backed by a node in the graph.
 *
 * This annotation is not needed if the domain entity's simple classname
 * matches at least one of the labels of the node in the graph (case
 * insensitive)
 *
 * @author Vince Bickers
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface NodeEntity {

    static final String CLASS = "org.neo4j.ogm.annotation.NodeEntity";
    static final String LABEL = "label";

    String label() default "";
}
