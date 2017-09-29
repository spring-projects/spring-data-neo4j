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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.StringUtils;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Oliver Gierke
 */
public class GraphQueryMethod extends QueryMethod {

	private final Method method;
	private final Query queryAnnotation;
	private final Integer queryDepthParamIndex;

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
