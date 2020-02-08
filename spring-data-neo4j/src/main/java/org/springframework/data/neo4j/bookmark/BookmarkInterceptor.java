/*
 * Copyright 2011-2020 the original author or authors.
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
