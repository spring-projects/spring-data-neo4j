/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.QueryAnnotation;

/**
 * Annotation to declare finder queries directly on repository methods.
 *
 * @author Mark Angrish
 * @author Luanne Misquitta
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@QueryAnnotation
@Documented
public @interface Query {

	static final String CLASS = "org.springframework.data.neo4j.annotation.Query";
	static final String VALUE = "value";

	/**
	 * Defines the Cypher query to be executed when the annotated method is called.
	 */
	String value() default "";

	/**
	 * @return simpler count-query to be executed for @{see Pageable}-support
	 */
	String countQuery() default "";
}
