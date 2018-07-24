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

import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.bookmark.BookmarkManagementConfiguration;

/**
 * Enables bookmark management
 * <p>
 * Bean implementing {@link org.springframework.data.neo4j.bookmark.BookmarkManager} interface needs to exist in the
 * context. Default implementation {@link org.springframework.data.neo4j.bookmark.CaffeineBookmarkManager} exists. Use
 * scope of the bean to control how bookmarks are managed. singleton - suitable for application wide bookmarks, e.g. fat
 * clients request,session - suitable for web applications
 * <p>
 * NOTE: Only usable with OGM Bolt driver.
 *
 * @author Frantisek Hartman
 * @see UseBookmark
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@Documented
@Import(BookmarkManagementConfiguration.class)
public @interface EnableBookmarkManagement {
}
