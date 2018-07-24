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

package org.springframework.data.neo4j.bookmark;

import java.lang.reflect.Method;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.data.neo4j.annotation.UseBookmark;

/**
 * Pointcut for methods marked with @{@link UseBookmark}
 *
 * @author Frantisek Hartman
 */
public class BookmarkOperationPointcut extends StaticMethodMatcherPointcut {

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		UseBookmark classAnnotation = targetClass.getAnnotation(UseBookmark.class);
		UseBookmark methodAnnotation = method.getAnnotation(UseBookmark.class);

		// method matches if @UseBookmark is on class, is true and not on method or
		// @UseBookmark is on method and is true
		// it is needed to cover @UseBookmark on class and @UseBookmark(false) on method
		return ((classAnnotation != null) && classAnnotation.value() && methodAnnotation == null)
				|| ((methodAnnotation != null) && methodAnnotation.value());
	}
}
