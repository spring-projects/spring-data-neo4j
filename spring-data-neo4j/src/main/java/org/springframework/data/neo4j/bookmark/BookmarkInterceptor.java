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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Interceptor that sets {@link BookmarkInfo} as a ThreadLocal
 *
 * @author Frantisek Hartman
 */
public class BookmarkInterceptor extends BookmarkSupport implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		BookmarkInfo previousValue;

		if (bookmarkInfoHolder.get() == null) {
			previousValue = null;
		} else {
			previousValue = bookmarkInfoHolder.get();
		}
		bookmarkInfoHolder.set(new BookmarkInfo(true));

		try {
			return invocation.proceed();
		} finally {
			bookmarkInfoHolder.set(previousValue);
		}
	}
}
