/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.bookmark;

import java.lang.reflect.Method;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.neo4j.annotation.UseBookmark;

/**
 * Pointcut for methods marked with @{@link UseBookmark}
 *
 * @author Frantisek Hartman
 */
public class BookmarkOperationPointcut extends StaticMethodMatcherPointcut {

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		UseBookmark classAnnotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, UseBookmark.class);
		UseBookmark methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, UseBookmark.class);

		// method matches if @UseBookmark is on class, is true and not on method or
		// @UseBookmark is on method and is true
		// it is needed to cover @UseBookmark on class and @UseBookmark(false) on method
		return ((classAnnotation != null) && classAnnotation.value() && methodAnnotation == null)
				|| ((methodAnnotation != null) && methodAnnotation.value());
	}
}
