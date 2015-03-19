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
 * By default all domain entity types will be persisted unless they are
 * annotated with @Transient, or are non-annotated abstract classes.
 *
 * This annotation can be placed on types, fields and methods
 * and the OGM will ignore any object or object reference with
 * the annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Inherited
public @interface Transient {
    static final String CLASS = "org.neo4j.ogm.annotation.Transient";
}

