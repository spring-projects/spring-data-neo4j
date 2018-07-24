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

package org.springframework.data.neo4j.repository.query;

import org.neo4j.ogm.cypher.query.SortOrder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.repository.query.ParametersParameterAccessor;

/**
 * Custom {@link ParametersParameterAccessor} to allow access to the {@link Depth} parameter.
 *
 * @author Nicolas Mervaillie
 */
public class GraphParametersParameterAccessor extends ParametersParameterAccessor implements GraphParameterAccessor {

	private static final int DEFAULT_QUERY_DEPTH = 1;
	private final GraphQueryMethod method;

	/**
	 * Creates a new {@link ParametersParameterAccessor}.
	 *
	 * @param method must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	public GraphParametersParameterAccessor(GraphQueryMethod method, Object[] values) {
		super(method.getParameters(), values);

		this.method = method;
	}

	@Override
	public int getDepth() {

		Depth methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), Depth.class);
		if (methodAnnotation != null) {
			return (int) AnnotationUtils.getValue(methodAnnotation);
		}

		GraphParameters graphParameters = method.getParameters();

		int depthIndex = graphParameters.getDepthIndex();

		if (depthIndex != -1) {
			return getValue(depthIndex);
		} else {
			return DEFAULT_QUERY_DEPTH;
		}
	}

	@Override
	public SortOrder getOgmSort() {
		SortOrder sortOrder = new SortOrder();

		if (getSort() != null) {
			for (Sort.Order order : getSort()) {
				if (order.isAscending()) {
					sortOrder.add(order.getProperty());
				} else {
					sortOrder.add(SortOrder.Direction.DESC, order.getProperty());
				}
			}
		}
		return sortOrder;
	}
}
