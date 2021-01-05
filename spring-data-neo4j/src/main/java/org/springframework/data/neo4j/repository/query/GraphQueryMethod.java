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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Oliver Gierke
 * @author Michael J. Simons
 */
public class GraphQueryMethod extends QueryMethod {

	private final Method method;
	private final Query queryAnnotation;
	private final Integer queryDepthParamIndex;
	private @Nullable MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext;

	public GraphQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);

		this.method = method;
		this.queryAnnotation = method.getAnnotation(Query.class);
		this.queryDepthParamIndex = getQueryDepthParamIndex(method);
		Integer queryDepth = getStaticQueryDepth(method);
		if (queryDepth != null && queryDepthParamIndex != null) {
			throw new IllegalArgumentException(method.getName() + " cannot have both a method @Depth and a parameter @Depth");
		}

	}

	@Override
	protected Parameters<?, ?> createParameters(Method method) {
		return new GraphParameters(method);
	}

	@Override
	public GraphParameters getParameters() {
		return (GraphParameters) super.getParameters();
	}

	public MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	public void setMappingContext(MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {
		this.mappingContext = mappingContext;
	}

	public String getQuery() {
		return queryAnnotation.value();
	}

	public Method getMethod() {
		return method;
	}

	/**
	 * Returns the name of the named query this method belongs to.
	 *
	 * @return
	 */
	public String getNamedQueryName() {
		return String.format("%s.%s", getDomainClass().getSimpleName(), method.getName());
	}

	public Integer getQueryDepthParamIndex() {
		return queryDepthParamIndex;
	}

	private Integer getQueryDepthParamIndex(Method method) {
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].length > 0) {
				for (Annotation annotation : annotations[i]) {
					if (annotation.annotationType() == Depth.class) {
						if (method.getParameterTypes()[i] == Integer.class || method.getParameterTypes()[i] == int.class) {
							return i;
						} else {
							throw new IllegalArgumentException("Depth parameter in " + method.getName() + " must be an integer");
						}
					}
				}
			}
		}
		/*
		//Java 8 only
		Parameter[] parameters = method.getParameters();
		 for (int i = 0; i < method.getParameterCount(); i++) {
		     if (parameters[i].isAnnotationPresent(Depth.class)) {
		         if (parameters[i].getType() == Integer.class || parameters[i].getType() == int.class) {
		             return i;
		         }
		         else {
		             throw new IllegalArgumentException("Depth parameter in " + method.getName() + " must be an integer");
		         }
		     }
		 }*/
		return null;
	}

	private Integer getStaticQueryDepth(Method method) {
		if (method.isAnnotationPresent(Depth.class)) {
			return method.getAnnotation(Depth.class).value();
		}
		return null;
	}

	public String getCountQueryString() {
		return queryAnnotation != null ? queryAnnotation.countQuery() : null;
	}

	public boolean hasAnnotatedQuery() {
		return getAnnotatedQuery() != null;
	}

	private String getAnnotatedQuery() {

		String query = (String) AnnotationUtils.getValue(getQueryAnnotation());
		return StringUtils.hasText(query) ? query : null;
	}

	private Query getQueryAnnotation() {
		return AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
	}
}
