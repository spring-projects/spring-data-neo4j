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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;

/**
 * Custom extension of {@link Parameters} discovering additional to handle @link{Depth} special parameter.
 *
 * @author Nicolas Mervaillie
 */
public class GraphParameters extends Parameters<GraphParameters, GraphParameters.GraphParameter> {

	private Integer depthIndex;

	GraphParameters(Method method) {
		super(method);
	}

	GraphParameters(Method method, List<GraphParameter> parameters) {

		super(parameters);

		for (int i = 0; i < parameters.size(); i++) {
			GraphParameter parameter = parameters.get(i);
			if (parameter.isDepthParameter()) {
				this.depthIndex = i;
			}
		}
	}

	GraphParameters(List<GraphParameter> parameters, Integer depthIndex) {
		super(parameters);
		this.depthIndex = depthIndex;
	}

	@Override
	protected GraphParameter createParameter(MethodParameter parameter) {
		GraphParameter graphParameter = new GraphParameter(parameter);

		// Detect manually annotated @Depth and reject multiple annotated ones
		if (this.depthIndex == null && graphParameter.isDepthParameter()) {
			this.depthIndex = graphParameter.getIndex();
		} else if (graphParameter.isDepthParameter()) {
			throw new IllegalStateException(String.format("Found multiple @Depth annotations on method %s! Only one allowed!",
					parameter.getMethod().toString()));
		}

		return graphParameter;
	}

	@Override
	protected GraphParameters createFrom(List<GraphParameter> parameters) {
		return new GraphParameters(parameters, this.depthIndex);
	}

	int getDepthIndex() {
		return (depthIndex != null) ? depthIndex : -1;
	}

	class GraphParameter extends Parameter {

		private final MethodParameter parameter;

		/**
		 * Creates a new {@link GraphParameter}.
		 *
		 * @param parameter must not be {@literal null}.
		 */
		GraphParameter(MethodParameter parameter) {
			super(parameter);
			this.parameter = parameter;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.Parameter#isSpecialParameter()
		 */
		@Override
		public boolean isSpecialParameter() {
			return super.isSpecialParameter() || Distance.class.isAssignableFrom(getType())
					|| parameter.getParameterAnnotation(Depth.class) != null || Distance.class.isAssignableFrom(getType())
					|| Point.class.isAssignableFrom(getType());
		}

		boolean isDepthParameter() {
			return parameter.getParameterAnnotation(Depth.class) != null;
		}
	}
}
