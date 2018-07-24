/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

/**
 * Indicates that a transactional operation will use bookmarks that are currently stored in BookmarkManager when
 * creating Neo4j session. Must be used on a method with @Transactional annotation. May be used on class - applies to
 * all methods or on specific methods.
 * <p>
 *
 * @author Frantisek Hartman
 * @see EnableBookmarkManagement
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
public @interface UseBookmark {

	/**
	 * If the annotated method should set bookmark
	 */
	boolean value() default true;
}
