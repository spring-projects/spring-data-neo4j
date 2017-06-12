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

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * Advisor for BookmarkManagement
 * <p>
 * Used to setup BookmarkInfo for methods marked with @{@link org.springframework.data.neo4j.annotation.UseBookmark}
 *
 * @author Frantisek Hartman
 */
public class BeanFactoryBookmarkOperationAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	private Pointcut pointcut = new BookmarkOperationPointcut();

	@Override
	public Pointcut getPointcut() {
		return pointcut;
	}
}
