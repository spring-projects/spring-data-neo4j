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
package org.springframework.data.neo4j.repository.query;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

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
 * @author Michael J. Simons
 */
public class GraphParameters extends Parameters<GraphParameters, GraphParameters.GraphParameter> {

	private Integer depthIndex;

	GraphParameters(Method method) {
		super(method);
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

			String methodFragment = Optional.ofNullable(parameter)
					.map(MethodParameter::getMethod)
					.map(Method::toString)
					.map(s -> "method " + s)
					.orElse("unknown method");
			throw new IllegalStateException(String.format("Found multiple @Depth annotations on method %s! Only one allowed!",
					methodFragment));
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

	static class GraphParameter extends Parameter {

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
