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
